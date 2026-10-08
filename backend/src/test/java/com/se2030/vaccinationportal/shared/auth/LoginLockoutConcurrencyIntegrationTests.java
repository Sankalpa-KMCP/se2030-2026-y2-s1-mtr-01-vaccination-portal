package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency assurance for the durable failed-login counter: parallel failed
 * logins must serialize on the atomic threshold transition so that the timed
 * lock is established exactly once, no increment is lost, and the counter is
 * left in its post-threshold (zero) state.
 *
 * <p>Deliberately non-transactional so worker threads operate in independent
 * transactions, mirroring {@code AuthActionTokenConcurrencyIntegrationTests}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class LoginLockoutConcurrencyIntegrationTests {

    private static final String LOCKED_EMAIL = "concurrent.lockout@portal.gov";
    private static final String CORRECT_PASSWORD = "Password123!";
    private static final String WRONG_PASSWORD = "WrongPassword123!";
    private static final int CONCURRENCY = 6;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        refreshTokenSessionRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.flush();
    }

    @Test
    @DisplayName("concurrent failed logins cannot lose increments or bypass the lock threshold")
    void concurrentFailedLogins_lockExactlyAtThresholdWithoutLostUpdates() throws Exception {
        Role citizenRole = roleRepository.save(new Role("ROLE_CITIZEN", "Citizen"));
        UserAccount account = new UserAccount(LOCKED_EMAIL, passwordEncoder.encode(CORRECT_PASSWORD), AccountStatus.ACTIVE);
        account.addRole(citizenRole);
        userAccountRepository.saveAndFlush(account);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENCY);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENCY);
        AtomicInteger badCredentialRejections = new AtomicInteger();
        AtomicInteger unexpectedFailures = new AtomicInteger();

        for (int worker = 0; worker < CONCURRENCY; worker++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                    authService.login(new LoginRequest(LOCKED_EMAIL, WRONG_PASSWORD));
                } catch (BadCredentialsException expected) {
                    badCredentialRejections.incrementAndGet();
                } catch (Exception unexpected) {
                    unexpectedFailures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "Workers failed to prepare in time");
        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Workers failed to finish in time");
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(CONCURRENCY, badCredentialRejections.get(),
                "Every failed login must be rejected with invalid credentials");
        assertEquals(0, unexpectedFailures.get(), "No unexpected exception may escape the atomic transition");

        UserAccount after = userAccountRepository.findByEmailIgnoreCase(LOCKED_EMAIL).orElseThrow();
        assertNotNull(after.getLockedUntil(),
                "The atomic threshold transition must establish the timed lock exactly once");
        assertEquals(0, after.getFailedLoginAttempts(),
                "The threshold transition resets the counter; attempts during an active lock match no rows");
        assertEquals(AccountStatus.ACTIVE, after.getAccountStatus());
    }
}
