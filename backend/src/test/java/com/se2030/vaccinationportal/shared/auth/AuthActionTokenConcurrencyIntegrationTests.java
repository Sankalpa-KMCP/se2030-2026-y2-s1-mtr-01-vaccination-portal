package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.dto.ResetPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.VerifyEmailRequest;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidAuthActionTokenException;
import com.se2030.vaccinationportal.shared.auth.exception.TokenAlreadyUsedException;
import com.se2030.vaccinationportal.shared.auth.exception.TokenExpiredException;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.EmailVerificationToken;
import com.se2030.vaccinationportal.shared.auth.model.PasswordResetToken;
import com.se2030.vaccinationportal.shared.auth.model.RefreshTokenSession;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.CitizenProfileRepository;
import com.se2030.vaccinationportal.shared.auth.repository.EmailVerificationTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.PasswordResetTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.AuthActionTokenService;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency integration tests verifying atomic single-use semantics for
 * email verification and password reset action tokens.
 * This test class is deliberately non-transactional so worker threads operate
 * against committed test fixture data and exercise real database row-level locking.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthActionTokenConcurrencyIntegrationTests {

    private static final String DEFAULT_PASSWORD = "OldPassword123!";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CitizenProfileRepository citizenProfileRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthActionTokenService authActionTokenService;

    @BeforeEach
    void setUp() {
        tearDown();

        roleRepository.saveAll(List.of(
                new Role("ROLE_ADMIN", "System Administrator"),
                new Role("ROLE_CENTER_MANAGER", "Vaccination Center Manager"),
                new Role("ROLE_HEALTHCARE_OFFICER", "Healthcare Officer"),
                new Role("ROLE_CAMPAIGN_OFFICER", "Campaign Officer"),
                new Role("ROLE_INVENTORY_OFFICER", "Inventory Officer"),
                new Role("ROLE_CITIZEN", "Citizen"),
                new Role("ROLE_MANAGEMENT_USER", "Management User")
        ));
        roleRepository.flush();
    }

    @AfterEach
    void tearDown() {
        refreshTokenSessionRepository.deleteAll();
        refreshTokenSessionRepository.flush();
        emailVerificationTokenRepository.deleteAll();
        emailVerificationTokenRepository.flush();
        passwordResetTokenRepository.deleteAll();
        passwordResetTokenRepository.flush();
        citizenProfileRepository.deleteAll();
        citizenProfileRepository.flush();
        userAccountRepository.deleteAll();
        userAccountRepository.flush();
        roleRepository.deleteAll();
        roleRepository.flush();
    }

    @RepeatedTest(3)
    @DisplayName("A2: Concurrent email verification on same token produces exactly 1 success and 1 TOKEN_ALREADY_USED")
    void verifyEmail_concurrentRequests_exactlyOneSucceedsAndLoserReceivesAlreadyUsed() throws Exception {
        UserAccount user = createPendingCitizen("concurrent.verify@example.com");

        String rawToken = "concurrent-verify-token-alpha";
        String tokenHash = authActionTokenService.hashToken(rawToken);
        emailVerificationTokenRepository.saveAndFlush(
                new EmailVerificationToken(user, tokenHash, Instant.now().plus(24, ChronoUnit.HOURS)));

        int concurrency = 2;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch readyLatch = new CountDownLatch(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrency);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger alreadyUsedCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    authService.verifyEmail(new VerifyEmailRequest(rawToken));
                    successCount.incrementAndGet();
                } catch (TokenAlreadyUsedException ex) {
                    alreadyUsedCount.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "Threads failed to prepare in time");
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Concurrent execution timed out");
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one concurrent verification request must succeed");
        assertEquals(1, alreadyUsedCount.get(), "Concurrent loser must receive TOKEN_ALREADY_USED");
        assertTrue(unexpectedErrors.isEmpty(), "Unexpected errors during concurrent verification: " + unexpectedErrors);

        // Verify post-conditions
        EmailVerificationToken consumedToken = emailVerificationTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertTrue(consumedToken.isConsumed(), "Token must be marked consumed");
        assertNotNull(consumedToken.getUsedAt(), "usedAt must be populated");

        UserAccount updatedUser = userAccountRepository.findById(user.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, updatedUser.getAccountStatus(), "Account must be transitioned to ACTIVE");
        assertNotNull(updatedUser.getEmailVerifiedAt(), "emailVerifiedAt must be populated");
    }

    @RepeatedTest(3)
    @DisplayName("A2: Concurrent password reset on same token with distinct passwords produces exactly 1 winner")
    void resetPassword_concurrentRequests_exactlyOneSucceedsAndDistinctPasswordsResolveDeterministically() throws Exception {
        UserAccount user = createActiveCitizen("concurrent.reset@example.com");

        // Seed an active refresh token session
        RefreshTokenSession session = new RefreshTokenSession(
                user, "family-concurrent", authActionTokenService.hashToken("refresh-raw-concurrent"), Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenSessionRepository.saveAndFlush(session);

        String rawToken = "concurrent-reset-token-beta";
        String tokenHash = authActionTokenService.hashToken(rawToken);
        passwordResetTokenRepository.saveAndFlush(
                new PasswordResetToken(user, tokenHash, Instant.now().plus(30, ChronoUnit.MINUTES)));

        String passwordAlpha = "AlphaPassword123!";
        String passwordBeta = "BetaPassword456!";

        int concurrency = 2;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch readyLatch = new CountDownLatch(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrency);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger alreadyUsedCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        String[] passwords = {passwordAlpha, passwordBeta};
        for (int i = 0; i < concurrency; i++) {
            final String selectedPassword = passwords[i];
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    authService.resetPassword(new ResetPasswordRequest(rawToken, selectedPassword));
                    successCount.incrementAndGet();
                } catch (TokenAlreadyUsedException ex) {
                    alreadyUsedCount.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "Threads failed to prepare in time");
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Concurrent execution timed out");
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one concurrent reset request must succeed");
        assertEquals(1, alreadyUsedCount.get(), "Concurrent loser must receive TOKEN_ALREADY_USED");
        assertTrue(unexpectedErrors.isEmpty(), "Unexpected errors during concurrent reset: " + unexpectedErrors);

        // Verify post-conditions
        PasswordResetToken consumedToken = passwordResetTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertTrue(consumedToken.isConsumed(), "Token must be marked consumed");
        assertNotNull(consumedToken.getUsedAt(), "usedAt must be populated");

        // Stored password must match exactly ONE of the two submitted passwords (no overwrite or last-writer-wins)
        UserAccount updatedUser = userAccountRepository.findById(user.getId()).orElseThrow();
        boolean matchesAlpha = passwordEncoder.matches(passwordAlpha, updatedUser.getPasswordHash());
        boolean matchesBeta = passwordEncoder.matches(passwordBeta, updatedUser.getPasswordHash());
        boolean matchesOld = passwordEncoder.matches(DEFAULT_PASSWORD, updatedUser.getPasswordHash());

        assertFalse(matchesOld, "Old password must no longer match");
        assertTrue(matchesAlpha ^ matchesBeta, "Exactly one of the submitted passwords must match the stored hash");

        // Refresh sessions must remain revoked
        RefreshTokenSession updatedSession = refreshTokenSessionRepository.findById(session.getId()).orElseThrow();
        assertTrue(updatedSession.isRevoked(), "Refresh session must be revoked after successful password reset");
        assertNotNull(updatedSession.getRevokedAt());
    }

    @Test
    @DisplayName("Distinct token redemptions do not block or interfere with each other")
    void distinctTokens_concurrentRedemptions_doNotBlockOrInterfere() throws Exception {
        UserAccount user1 = createPendingCitizen("user1@example.com");
        UserAccount user2 = createPendingCitizen("user2@example.com");

        String rawToken1 = "distinct-token-111";
        String rawToken2 = "distinct-token-222";

        emailVerificationTokenRepository.saveAndFlush(
                new EmailVerificationToken(user1, authActionTokenService.hashToken(rawToken1), Instant.now().plus(24, ChronoUnit.HOURS)));
        emailVerificationTokenRepository.saveAndFlush(
                new EmailVerificationToken(user2, authActionTokenService.hashToken(rawToken2), Instant.now().plus(24, ChronoUnit.HOURS)));

        int concurrency = 2;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch readyLatch = new CountDownLatch(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrency);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        String[] tokens = {rawToken1, rawToken2};
        for (int i = 0; i < concurrency; i++) {
            final String token = tokens[i];
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    authService.verifyEmail(new VerifyEmailRequest(token));
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(2, successCount.get(), "Both distinct token verifications must succeed independently");
        assertTrue(unexpectedErrors.isEmpty(), "No errors should occur: " + unexpectedErrors);

        UserAccount u1 = userAccountRepository.findById(user1.getId()).orElseThrow();
        UserAccount u2 = userAccountRepository.findById(user2.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, u1.getAccountStatus());
        assertEquals(AccountStatus.ACTIVE, u2.getAccountStatus());
    }

    @Test
    @DisplayName("Error semantics: verifyEmail preserves INVALID_TOKEN, TOKEN_EXPIRED, and TOKEN_ALREADY_USED")
    void verifyEmail_errorSemantics_preserved() {
        // Unknown token
        assertThrows(InvalidAuthActionTokenException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest("non-existent-token")));

        // Expired token
        UserAccount user = createPendingCitizen("expired.verify@example.com");
        String expiredRawToken = "expired-token-val";
        emailVerificationTokenRepository.saveAndFlush(
                new EmailVerificationToken(user, authActionTokenService.hashToken(expiredRawToken), Instant.now().minus(1, ChronoUnit.HOURS)));
        assertThrows(TokenExpiredException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest(expiredRawToken)));

        // Consumed token
        String usedRawToken = "used-token-val";
        EmailVerificationToken usedToken = new EmailVerificationToken(
                user, authActionTokenService.hashToken(usedRawToken), Instant.now().plus(24, ChronoUnit.HOURS));
        usedToken.markUsed(Instant.now());
        emailVerificationTokenRepository.saveAndFlush(usedToken);
        assertThrows(TokenAlreadyUsedException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest(usedRawToken)));
    }

    @Test
    @DisplayName("Error semantics: resetPassword preserves INVALID_TOKEN, TOKEN_EXPIRED, and TOKEN_ALREADY_USED")
    void resetPassword_errorSemantics_preserved() {
        // Unknown token
        assertThrows(InvalidAuthActionTokenException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("non-existent-reset-token", "NewPassword123!")));

        // Expired token
        UserAccount user = createActiveCitizen("expired.reset@example.com");
        String expiredRawToken = "expired-reset-token";
        passwordResetTokenRepository.saveAndFlush(
                new PasswordResetToken(user, authActionTokenService.hashToken(expiredRawToken), Instant.now().minus(1, ChronoUnit.HOURS)));
        assertThrows(TokenExpiredException.class,
                () -> authService.resetPassword(new ResetPasswordRequest(expiredRawToken, "NewPassword123!")));

        // Consumed token
        String usedRawToken = "used-reset-token";
        PasswordResetToken usedToken = new PasswordResetToken(
                user, authActionTokenService.hashToken(usedRawToken), Instant.now().plus(30, ChronoUnit.MINUTES));
        usedToken.markUsed(Instant.now());
        passwordResetTokenRepository.saveAndFlush(usedToken);
        assertThrows(TokenAlreadyUsedException.class,
                () -> authService.resetPassword(new ResetPasswordRequest(usedRawToken, "NewPassword123!")));
    }

    private UserAccount createPendingCitizen(String email) {
        UserAccount user = new UserAccount(email, passwordEncoder.encode(DEFAULT_PASSWORD), AccountStatus.PENDING_VERIFICATION);
        user.setFullName("Pending Citizen");
        user.setPhoneNumber("0770001111");
        user.addRole(roleRepository.findByCode("ROLE_CITIZEN").orElseThrow());
        return userAccountRepository.saveAndFlush(user);
    }

    private UserAccount createActiveCitizen(String email) {
        UserAccount user = new UserAccount(email, passwordEncoder.encode(DEFAULT_PASSWORD), AccountStatus.ACTIVE);
        user.setFullName("Active Citizen");
        user.setPhoneNumber("0770002222");
        user.setEmailVerifiedAt(Instant.now());
        user.addRole(roleRepository.findByCode("ROLE_CITIZEN").orElseThrow());
        return userAccountRepository.saveAndFlush(user);
    }
}
