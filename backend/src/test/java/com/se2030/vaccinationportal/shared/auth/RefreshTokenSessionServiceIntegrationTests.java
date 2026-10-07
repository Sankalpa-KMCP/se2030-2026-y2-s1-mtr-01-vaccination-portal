package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.RefreshTokenSession;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.AuthActionTokenService;
import com.se2030.vaccinationportal.shared.auth.service.RefreshTokenSessionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RefreshTokenSessionServiceIntegrationTests {

    static final Instant FIXED_NOW = Instant.parse("2026-01-15T10:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }

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

    @Autowired
    private EntityManager entityManager;

    private Role citizenRole;

    @BeforeEach
    void setUp() {
        refreshTokenSessionRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        citizenRole = roleRepository.save(new Role("ROLE_CITIZEN", "Citizen"));
    }

    private UserAccount createAccountWithEmail(String email, AccountStatus status) {
        UserAccount account = new UserAccount(email, passwordEncoder.encode("Password123!"), status);
        account.addRole(citizenRole);
        return userAccountRepository.saveAndFlush(account);
    }

    private UserAccount reload(UserAccount account) {
        entityManager.flush();
        entityManager.clear();
        return userAccountRepository.findById(account.getId()).orElseThrow();
    }

    private RefreshTokenSession familySessionOf(UserAccount account) {
        return refreshTokenSessionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(account.getId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("root session stores only the hash, a fresh family, and Clock + 7 day expiry")
    void issueRootSession_storesHashOnlyWithDeterministicExpiry() {
        UserAccount account = createAccountWithEmail("refresh.root@portal.gov", AccountStatus.ACTIVE);

        RefreshTokenSessionService.IssuedRefreshToken issued =
                refreshTokenSessionService.issueRootSession(account);

        assertNotNull(issued.rawToken());
        assertNotEquals(issued.rawToken(), authActionTokenService.hashToken(issued.rawToken()),
                "Raw refresh token must never be persisted as-is");

        entityManager.flush();
        entityManager.clear();
        RefreshTokenSession stored = familySessionOf(account);
        assertEquals(authActionTokenService.hashToken(issued.rawToken()), stored.getTokenHash(),
                "Server must store the deterministic SHA-256 lookup hash");
        assertNull(stored.getRotatedFromId(), "A root session starts its own family");
        assertNull(stored.getRevokedAt());
        assertNull(stored.getReuseDetectedAt());
        assertEquals(FIXED_NOW.plus(Duration.ofDays(7)), stored.getExpiresAt(),
                "Refresh lifetime must be exactly Clock + 7 days under the fixed clock");
    }

    @Test
    @DisplayName("valid rotation revokes the old session and links the child in the same family")
    void rotate_validToken_revokesOldAndCreatesLinkedChild() {
        UserAccount account = createAccountWithEmail("refresh.rotate@portal.gov", AccountStatus.ACTIVE);
        RefreshTokenSessionService.IssuedRefreshToken issued = refreshTokenSessionService.issueRootSession(account);
        RefreshTokenSession root = familySessionOf(account);

        RefreshTokenSessionService.RotatedRefreshSession rotated =
                refreshTokenSessionService.rotate(issued.rawToken());

        assertEquals(account.getId(), rotated.account().getId());
        assertNotEquals(issued.rawToken(), rotated.rawToken());

        entityManager.flush();
        entityManager.clear();
        RefreshTokenSession rotatedRoot = refreshTokenSessionRepository.findById(root.getId()).orElseThrow();
        assertTrue(rotatedRoot.isRevoked(), "The presented session must be consumed by rotation");
        RefreshTokenSession child = refreshTokenSessionRepository.findAll().stream()
                .filter(s -> root.getId().equals(s.getRotatedFromId()))
                .findFirst()
                .orElseThrow();
        assertEquals(root.getTokenFamilyId(), child.getTokenFamilyId(), "Rotation stays in the same family");
        assertEquals(root.getId(), child.getRotatedFromId(), "Child must reference the rotated parent");
        assertEquals(FIXED_NOW.plus(Duration.ofDays(7)), child.getExpiresAt());
        assertNotEquals(root.getTokenHash(), child.getTokenHash());
    }

    @Test
    @DisplayName("reuse of a rotated token revokes the whole family and stamps reuse detection")
    void rotate_reusedToken_revokesFamilyAndMarksReuse() {
        UserAccount account = createAccountWithEmail("refresh.reuse@portal.gov", AccountStatus.ACTIVE);
        RefreshTokenSessionService.IssuedRefreshToken issued = refreshTokenSessionService.issueRootSession(account);
        RefreshTokenSession root = familySessionOf(account);

        refreshTokenSessionService.rotate(issued.rawToken());
        entityManager.flush();
        entityManager.clear();

        assertThrows(BadCredentialsException.class, () ->
                refreshTokenSessionService.rotate(issued.rawToken()));

        entityManager.flush();
        entityManager.clear();
        RefreshTokenSession presentedRoot = refreshTokenSessionRepository.findById(root.getId()).orElseThrow();
        assertNotNull(presentedRoot.getReuseDetectedAt(),
                "Reuse detection must be stamped on the presented (rotated) session");
        RefreshTokenSession unusedChild = refreshTokenSessionRepository.findAll().stream()
                .filter(s -> root.getId().equals(s.getRotatedFromId()))
                .findFirst()
                .orElseThrow();
        assertTrue(unusedChild.isRevoked(), "Reuse must revoke every session in the family");
        assertNull(unusedChild.getReuseDetectedAt(),
                "Only the presented session carries the reuse-detection stamp");
    }

    @Test
    @DisplayName("the rotation claim is atomic: a second claim of the same session finds no rows")
    void claimForRotation_isAtomic() {
        UserAccount account = createAccountWithEmail("refresh.claim@portal.gov", AccountStatus.ACTIVE);
        RefreshTokenSessionService.IssuedRefreshToken issued = refreshTokenSessionService.issueRootSession(account);
        RefreshTokenSession root = familySessionOf(account);

        assertEquals(1, refreshTokenSessionRepository.claimForRotation(root.getId(), FIXED_NOW, FIXED_NOW),
                "Exactly one claim must win the rotation race");
        assertEquals(0, refreshTokenSessionRepository.claimForRotation(root.getId(), FIXED_NOW, FIXED_NOW),
                "A competing claim must match no rows and trigger reuse handling");
    }

    @Test
    @DisplayName("expired and unknown refresh tokens are rejected without state creation")
    void rotate_expiredOrUnknown_rejectedWithoutStateChange() {
        UserAccount account = createAccountWithEmail("refresh.expired@portal.gov", AccountStatus.ACTIVE);

        String expiredRaw = "expired-raw-refresh-token";
        RefreshTokenSession expired = new RefreshTokenSession(
                account,
                "expired-family",
                authActionTokenService.hashToken(expiredRaw),
                FIXED_NOW.minusSeconds(60),
                FIXED_NOW.minus(Duration.ofDays(8)));
        refreshTokenSessionRepository.saveAndFlush(expired);
        int sessionsBefore = refreshTokenSessionRepository.findAll().size();

        assertThrows(BadCredentialsException.class, () -> refreshTokenSessionService.rotate(expiredRaw));
        assertThrows(BadCredentialsException.class, () -> refreshTokenSessionService.rotate("totally-unknown-token"));

        assertEquals(sessionsBefore, refreshTokenSessionRepository.findAll().size(),
                "Expired/unknown tokens must not create sessions");
    }

    @Test
    @DisplayName("refresh for a permanently LOCKED account is rejected as locked")
    void rotate_lockedAccount_rejectedAsLocked() {
        UserAccount account = createAccountWithEmail("refresh.locked@portal.gov", AccountStatus.LOCKED);
        RefreshTokenSessionService.IssuedRefreshToken issued = refreshTokenSessionService.issueRootSession(account);

        assertThrows(LockedException.class, () -> refreshTokenSessionService.rotate(issued.rawToken()));
    }

    @Test
    @DisplayName("logout semantics revoke the whole presented family")
    void revokeFamilyByRawToken_revokesWholeFamily() {
        UserAccount account = createAccountWithEmail("refresh.logout@portal.gov", AccountStatus.ACTIVE);
        RefreshTokenSessionService.IssuedRefreshToken issued = refreshTokenSessionService.issueRootSession(account);
        RefreshTokenSession root = familySessionOf(account);

        RefreshTokenSessionService.RotatedRefreshSession rotated = refreshTokenSessionService.rotate(issued.rawToken());

        refreshTokenSessionService.revokeFamilyByRawToken(rotated.rawToken());

        entityManager.flush();
        entityManager.clear();
        refreshTokenSessionRepository.findAll().forEach(session ->
                assertTrue(session.isRevoked(), "Logout must revoke the whole family"));
        assertDoesNotThrow(() -> refreshTokenSessionService.revokeFamilyByRawToken(rotated.rawToken()),
                "Repeated logout must stay idempotent");
        assertDoesNotThrow(() -> refreshTokenSessionService.revokeFamilyByRawToken(null),
                "Logout without a cookie must be a no-op");
    }
}
