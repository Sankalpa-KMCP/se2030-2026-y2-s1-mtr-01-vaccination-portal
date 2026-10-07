package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.RefreshTokenSession;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.AuthActionTokenService;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import com.se2030.vaccinationportal.shared.auth.service.RefreshTokenSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Non-transactionally wrapped integration tests proving that token reuse detection
 * and token family revocation persist to durable storage across transaction boundaries,
 * verifying the repair for Finding F-01 (noRollbackFor = BadCredentialsException.class).
 */
@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenSessionPersistenceIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenSessionService refreshTokenSessionService;

    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthActionTokenService authActionTokenService;

    private Role citizenRole;

    @BeforeEach
    void setUp() {
        cleanup();
        citizenRole = roleRepository.findByCode("ROLE_CITIZEN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_CITIZEN", "Citizen")));
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        refreshTokenSessionRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.flush();
    }

    private UserAccount createActiveUser(String email) {
        UserAccount user = new UserAccount(email, passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        user.setFullName("Test Citizen");
        user.setPhoneNumber("+94771234567");
        user.addRole(citizenRole);
        return userAccountRepository.save(user);
    }

    @Test
    @DisplayName("F-01: Reused rotated token persists reuse_detected_at and family revocation despite BadCredentialsException")
    void rotatedTokenReuse_persistsSecurityStateAcrossTransactionBoundary() {
        UserAccount user = createActiveUser("reuse-persists@portal.gov");

        // 1. Issue root session
        var rootIssue = refreshTokenSessionService.issueRootSession(user);
        String rootRawToken = rootIssue.rawToken();

        RefreshTokenSession rootSessionBefore = refreshTokenSessionRepository
                .findByTokenHash(authActionTokenService.hashToken(rootRawToken))
                .orElseThrow();
        Long rootSessionId = rootSessionBefore.getId();
        String familyId = rootSessionBefore.getTokenFamilyId();

        // 2. Legitimate rotation: root -> successor
        var successorIssue = refreshTokenSessionService.rotate(rootRawToken);
        String successorRawToken = successorIssue.rawToken();

        RefreshTokenSession successorSessionBefore = refreshTokenSessionRepository
                .findByTokenHash(authActionTokenService.hashToken(successorRawToken))
                .orElseThrow();
        Long successorSessionId = successorSessionBefore.getId();
        assertEquals(familyId, successorSessionBefore.getTokenFamilyId());
        assertFalse(successorSessionBefore.isRevoked(), "Successor must initially be active");

        // Verify root session is marked revoked from rotation
        RefreshTokenSession rootAfterRotation = refreshTokenSessionRepository.findById(rootSessionId).orElseThrow();
        assertTrue(rootAfterRotation.isRevoked(), "Root session must be revoked after rotation");
        assertNull(rootAfterRotation.getReuseDetectedAt(), "No reuse detected yet on legitimate rotation");

        // 3. Replay/reuse root token through AuthService.refresh(): must fail with BadCredentialsException
        assertThrows(BadCredentialsException.class, () -> authService.refresh(rootRawToken),
                "Replaying old rotated root token must throw BadCredentialsException");

        // 4. CRITICAL VERIFICATION (F-01): Query fresh state from durable persistence.
        // Because noRollbackFor = BadCredentialsException is configured, the security updates must NOT roll back!
        RefreshTokenSession rootAfterReuse = refreshTokenSessionRepository.findById(rootSessionId).orElseThrow();
        RefreshTokenSession successorAfterReuse = refreshTokenSessionRepository.findById(successorSessionId).orElseThrow();

        assertNotNull(rootAfterReuse.getReuseDetectedAt(),
                "Root session must have persisted reuse_detected_at in database despite 401 exception");
        assertNotNull(rootAfterReuse.getRevokedAt(),
                "Root session must have persisted revoked_at in database");
        assertNotNull(successorAfterReuse.getRevokedAt(),
                "Successor session must have persisted revoked_at in database via family revocation");

        // 5. Subsequent attempt to use the successor token must now also fail
        assertThrows(BadCredentialsException.class, () -> authService.refresh(successorRawToken),
                "Revoked successor must not allow refresh after family revocation");
    }

    @Test
    @DisplayName("F-01/Concurrency: Concurrent rotation loser unable to claim token persists family revocation (NEW-F2)")
    void concurrentRotationLoser_persistsFamilyRevocationAcrossTransactionBoundary() throws Exception {
        UserAccount user = createActiveUser("concurrent-loser@portal.gov");

        // 1. Issue root session for user 1 (target family)
        var rootIssue = refreshTokenSessionService.issueRootSession(user);
        String rawToken = rootIssue.rawToken();
        RefreshTokenSession rootSession = refreshTokenSessionRepository
                .findByTokenHash(authActionTokenService.hashToken(rawToken))
                .orElseThrow();
        Long sessionId = rootSession.getId();
        String familyId = rootSession.getTokenFamilyId();

        // 2. Issue root session for user 2 (unrelated family) to prove isolation
        UserAccount unrelatedUser = createActiveUser("unrelated-family@portal.gov");
        var unrelatedIssue = refreshTokenSessionService.issueRootSession(unrelatedUser);
        String unrelatedRawToken = unrelatedIssue.rawToken();
        RefreshTokenSession unrelatedSession = refreshTokenSessionRepository
                .findByTokenHash(authActionTokenService.hashToken(unrelatedRawToken))
                .orElseThrow();
        Long unrelatedSessionId = unrelatedSession.getId();

        // 3. Coordinate competing threads to rotate user 1's token
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<String> winnerRawToken = new AtomicReference<>();
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    var rotated = refreshTokenSessionService.rotate(rawToken);
                    winnerRawToken.set(rotated.rawToken());
                    return true;
                } catch (BadCredentialsException e) {
                    return false;
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        int successes = 0;
        int failures = 0;
        for (Future<Boolean> f : results) {
            if (f.get(5, TimeUnit.SECONDS)) {
                successes++;
            } else {
                failures++;
            }
        }
        executor.shutdown();

        // Exactly one rotation succeeds; loser experiences reuse/unclaimed token
        assertEquals(1, successes, "Exactly one concurrent rotation may succeed");
        assertEquals(1, failures, "The competing attempt must fail with BadCredentialsException");

        // Proof 1: Root session has reuse_detected_at populated by the loser across transaction boundaries
        RefreshTokenSession rootAfterRace = refreshTokenSessionRepository.findById(sessionId).orElseThrow();
        assertNotNull(rootAfterRace.getRevokedAt(), "Root session must be revoked in database after concurrent rotation");
        assertNotNull(rootAfterRace.getReuseDetectedAt(),
                "Root session must have reuse_detected_at populated by the loser across transaction boundary");

        // Proof 2: Winner's newly minted successor token is marked revoked due to family revocation by loser
        assertNotNull(winnerRawToken.get(), "Winner must have received a new raw token");
        RefreshTokenSession successorInDb = refreshTokenSessionRepository
                .findByTokenHash(authActionTokenService.hashToken(winnerRawToken.get()))
                .orElseThrow();
        assertEquals(familyId, successorInDb.getTokenFamilyId(), "Winner's successor must belong to the same family");
        assertNotNull(successorInDb.getRevokedAt(),
                "Winner's successor must be marked revoked in durable storage due to family revocation by the loser");

        // Proof 3: Winner's child token cannot subsequently be used to refresh
        assertThrows(BadCredentialsException.class, () -> authService.refresh(winnerRawToken.get()),
                "Winner's revoked successor token must not allow subsequent refresh");

        // Proof 4: Unrelated concurrent/active token family is unaffected
        RefreshTokenSession unrelatedInDb = refreshTokenSessionRepository.findById(unrelatedSessionId).orElseThrow();
        assertNull(unrelatedInDb.getRevokedAt(), "Unrelated family session must not be revoked");
        assertNull(unrelatedInDb.getReuseDetectedAt(), "Unrelated family session must not have reuse detected");
        assertDoesNotThrow(() -> authService.refresh(unrelatedRawToken),
                "Unrelated family active token must remain fully usable for refresh");
    }

    @Test
    @DisplayName("Unknown token throws BadCredentialsException without creating bogus sessions")
    void unknownToken_throwsWithoutCreatingSessions() {
        long countBefore = refreshTokenSessionRepository.count();
        assertThrows(BadCredentialsException.class,
                () -> authService.refresh("completely-unknown-token-that-never-existed-in-the-database"));
        assertEquals(countBefore, refreshTokenSessionRepository.count(),
                "No sessions or mutations should occur for unknown token");
    }

    @Test
    @DisplayName("Expired token throws BadCredentialsException without stamping reuse")
    void expiredToken_throwsWithoutStampingReuse() {
        UserAccount user = createActiveUser("expired-token@portal.gov");

        // Create an already-expired token session directly
        Instant now = Instant.now();
        Instant issuedAt = now.minus(Duration.ofDays(10));
        Instant expiresAt = now.minus(Duration.ofDays(3));
        String rawToken = "custom-test-expired-raw-token-12345678901234567890";
        String tokenHash = authActionTokenService.hashToken(rawToken);

        RefreshTokenSession expiredSession = new RefreshTokenSession(
                user,
                "expired-family-id",
                tokenHash,
                expiresAt,
                issuedAt
        );
        refreshTokenSessionRepository.save(expiredSession);

        assertThrows(BadCredentialsException.class, () -> authService.refresh(rawToken));

        RefreshTokenSession sessionAfter = refreshTokenSessionRepository.findById(expiredSession.getId()).orElseThrow();
        assertNull(sessionAfter.getReuseDetectedAt(), "Expired token must not be stamped as reuse detected");
    }
}
