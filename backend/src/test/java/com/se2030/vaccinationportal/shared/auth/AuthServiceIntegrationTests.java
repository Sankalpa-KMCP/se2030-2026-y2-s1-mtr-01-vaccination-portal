package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginResponse;
import com.se2030.vaccinationportal.shared.auth.dto.ResetPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.UserProfileResponse;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.PasswordResetToken;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.PasswordResetTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.AuthActionTokenService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTests {

    /**
     * The login lockout contract is defined against the injected application Clock;
     * pinning it makes threshold and lock-expiry semantics deterministic forever.
     */
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
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuthActionTokenService authActionTokenService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    private Role centerManagerRole;
    private Role adminRole;
    private Role citizenRole;

    @BeforeEach
    void setUp() {
        cleanup();

        centerManagerRole = roleRepository.save(new Role("ROLE_CENTER_MANAGER", "Vaccination Center Manager"));
        adminRole = roleRepository.save(new Role("ROLE_ADMIN", "System Administrator"));
        citizenRole = roleRepository.save(new Role("ROLE_CITIZEN", "Citizen"));
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        passwordResetTokenRepository.deleteAll();
        refreshTokenSessionRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.flush();
    }

    @Test
    @DisplayName("login with valid active account returns token and user profile")
    void login_validActiveAccount_success() {
        UserAccount account = new UserAccount("manager@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        account.addRole(centerManagerRole);
        userAccountRepository.save(account);

        LoginResponse response = authService.login(new LoginRequest("manager@portal.gov", "Password123!")).response();

        assertNotNull(response);
        assertNotNull(response.token());
        assertNotNull(response.user());
        assertEquals("manager@portal.gov", response.user().email());
        assertTrue(response.user().roles().contains("ROLE_CENTER_MANAGER"));
    }

    @Test
    @DisplayName("login normalizes email and performs case-insensitive lookup")
    void login_emailCaseInsensitive_success() {
        UserAccount account = new UserAccount("nurse@portal.gov", passwordEncoder.encode("SecretPass123!"), AccountStatus.ACTIVE);
        userAccountRepository.save(account);

        LoginResponse response = authService.login(new LoginRequest("  NURSE@PORTAL.GOV  ", "SecretPass123!")).response();

        assertNotNull(response);
        assertEquals("nurse@portal.gov", response.user().email());
    }

    @Test
    @DisplayName("login with incorrect password throws BadCredentialsException")
    void login_wrongPassword_throwsBadCredentials() {
        UserAccount account = new UserAccount("officer@portal.gov", passwordEncoder.encode("CorrectPassword1!"), AccountStatus.ACTIVE);
        userAccountRepository.save(account);

        assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("officer@portal.gov", "WrongPassword!"))
        );
    }

    @Test
    @DisplayName("login with unknown email throws BadCredentialsException without leaking user existence")
    void login_unknownEmail_throwsBadCredentials() {
        assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("unknown@portal.gov", "AnyPassword123!"))
        );
    }

    @Test
    @DisplayName("login with PENDING_VERIFICATION account succeeds so the citizen can reach principal-scoped OTP verification")
    void login_pendingVerificationAccount_succeedsForOtpVerification() {
        UserAccount account = new UserAccount("unverified@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.PENDING_VERIFICATION);
        account.addRole(citizenRole);
        userAccountRepository.save(account);

        LoginResponse response = authService.login(new LoginRequest("unverified@portal.gov", "Password123!")).response();

        assertNotNull(response.token());
        assertEquals(AccountStatus.PENDING_VERIFICATION, response.user().accountStatus());
    }

    @Test
    @DisplayName("login with SUSPENDED account throws DisabledException")
    void login_suspendedAccount_throwsDisabledException() {
        UserAccount account = new UserAccount("suspended@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.SUSPENDED);
        userAccountRepository.save(account);

        assertThrows(DisabledException.class, () ->
                authService.login(new LoginRequest("suspended@portal.gov", "Password123!"))
        );
    }

    @Test
    @DisplayName("login with explicitly LOCKED account status throws LockedException")
    void login_lockedAccount_throwsLockedException() {
        UserAccount account = new UserAccount("locked@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.LOCKED);
        userAccountRepository.save(account);

        assertThrows(LockedException.class, () ->
                authService.login(new LoginRequest("locked@portal.gov", "Password123!"))
        );
    }

    @Test
    @DisplayName("login with future locked_until timestamp throws LockedException")
    void login_temporaryLockNotExpired_throwsLockedException() {
        UserAccount account = new UserAccount("temp_locked@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        account.setLockedUntil(Instant.now().plus(30, ChronoUnit.MINUTES));
        userAccountRepository.save(account);

        assertThrows(LockedException.class, () ->
                authService.login(new LoginRequest("temp_locked@portal.gov", "Password123!"))
        );
    }

    @Test
    @DisplayName("getCurrentUserProfile loads the current account including nullable legacy profile fields")
    void getCurrentUserProfile_validPrincipal_returnsProfile() {
        UserAccount account = new UserAccount("admin@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        account.addRole(adminRole);
        account = userAccountRepository.saveAndFlush(account);
        UserPrincipal principal = UserPrincipal.fromClaims(account.getId(), account.getEmail(), List.of("ROLE_ADMIN"));
        UserProfileResponse profile = authService.getCurrentUserProfile(principal);

        assertNotNull(profile);
        assertEquals(account.getId(), profile.id());
        assertNull(profile.fullName());
        assertNull(profile.phoneNumber());
        assertEquals("admin@portal.gov", profile.email());
        assertEquals(AccountStatus.ACTIVE, profile.accountStatus());
        assertEquals(List.of("ROLE_ADMIN"), profile.roles());
    }

    private UserAccount createAccountWithEmail(String email) {
        UserAccount account = new UserAccount(email, passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        account.addRole(citizenRole);
        return userAccountRepository.saveAndFlush(account);
    }

    private void failLoginTimes(String email, int times) {
        for (int attempt = 0; attempt < times; attempt++) {
            assertThrows(BadCredentialsException.class, () ->
                    authService.login(new LoginRequest(email, "WrongPassword123!")));
            entityManager.flush();
            entityManager.clear();
        }
    }

    private UserAccount reloadByEmail(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email).orElseThrow();
    }

    @Test
    @DisplayName("wrong-password attempts below threshold increment the counter without locking")
    void login_failedAttemptsBelowThreshold_incrementCounterWithoutLock() {
        createAccountWithEmail("lockout.below@portal.gov");

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThrows(BadCredentialsException.class, () ->
                    authService.login(new LoginRequest("lockout.below@portal.gov", "WrongPassword123!")));
            entityManager.flush();
            entityManager.clear();
            UserAccount reloaded = reloadByEmail("lockout.below@portal.gov");
            assertEquals(attempt, reloaded.getFailedLoginAttempts(), "Failed attempt " + attempt + " must persist");
            assertNull(reloaded.getLockedUntil(), "No timed lock may exist below the threshold");
            assertEquals(AccountStatus.ACTIVE, reloaded.getAccountStatus());
        }
    }

    @Test
    @DisplayName("fifth consecutive failure establishes the 15 minute timed lock and resets the counter")
    void login_fifthFailedAttempt_establishesTimedLock() {
        createAccountWithEmail("lockout.threshold@portal.gov");

        failLoginTimes("lockout.threshold@portal.gov", 5);

        UserAccount locked = reloadByEmail("lockout.threshold@portal.gov");
        assertEquals(FIXED_NOW.plus(Duration.ofSeconds(900)), locked.getLockedUntil(),
                "Timed lock must expire exactly Clock + 15 minutes under the fixed clock");
        assertEquals(0, locked.getFailedLoginAttempts(),
                "Threshold transition resets the counter so an expired lock starts a clean cycle");
        assertEquals(AccountStatus.ACTIVE, locked.getAccountStatus());
    }

    @Test
    @DisplayName("correct password is rejected as locked while the timed lock is active")
    void login_correctPasswordWhileLockActive_throwsLocked() {
        createAccountWithEmail("lockout.active@portal.gov");
        failLoginTimes("lockout.active@portal.gov", 5);

        assertThrows(LockedException.class, () ->
                authService.login(new LoginRequest("lockout.active@portal.gov", "Password123!")));

        entityManager.flush();
        entityManager.clear();
        UserAccount stillLocked = reloadByEmail("lockout.active@portal.gov");
        assertNotNull(stillLocked.getLockedUntil(), "A rejected attempt must not consume or clear the lock");
        assertEquals(0, stillLocked.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("an expired timed lock unlocks by system-after-expiry and resets the failure counter")
    void login_expiredTimedLock_unlocksAndResetsCounter() {
        createAccountWithEmail("lockout.expired@portal.gov");
        failLoginTimes("lockout.expired@portal.gov", 5);

        UserAccount locked = reloadByEmail("lockout.expired@portal.gov");
        locked.setLockedUntil(FIXED_NOW.minusSeconds(60));
        userAccountRepository.saveAndFlush(locked);
        entityManager.clear();

        LoginResponse response = authService.login(new LoginRequest("lockout.expired@portal.gov", "Password123!")).response();
        assertNotNull(response.token(), "Login after lock expiry must succeed");

        UserAccount unlocked = reloadByEmail("lockout.expired@portal.gov");
        assertNull(unlocked.getLockedUntil(), "Expired timed lock must be cleared");
        assertEquals(0, unlocked.getFailedLoginAttempts(), "Counter must be zero after expiry unlock");
    }

    @Test
    @DisplayName("successful login below threshold resets the failure counter")
    void login_successBelowThreshold_resetsCounter() {
        createAccountWithEmail("lockout.reset@portal.gov");
        failLoginTimes("lockout.reset@portal.gov", 2);

        LoginResponse response = authService.login(new LoginRequest("lockout.reset@portal.gov", "Password123!")).response();
        assertNotNull(response.token());
        entityManager.flush();
        entityManager.clear();

        UserAccount reloaded = reloadByEmail("lockout.reset@portal.gov");
        assertEquals(0, reloaded.getFailedLoginAttempts(), "Successful login resets the counter");
        assertNull(reloaded.getLockedUntil());
    }

    @Test
    @DisplayName("unknown email returns the same enumeration-safe rejection and creates no account state")
    void login_unknownEmail_sameSemantics_noStateCreated() {
        UserAccount existing = createAccountWithEmail("lockout.known@portal.gov");
        long accountsBefore = userAccountRepository.count();

        BadCredentialsException unknown = assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("ghost.unknown@portal.gov", "Whatever123!")));
        BadCredentialsException knownWrongPassword = assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("lockout.known@portal.gov", "WrongPassword123!")));

        assertEquals(knownWrongPassword.getMessage(), unknown.getMessage(),
                "Unknown email and wrong password must be outwardly identical");
        assertEquals(accountsBefore, userAccountRepository.count(), "Unknown email must not create rows");
        entityManager.clear();
        assertEquals(1, reloadByEmail("lockout.known@portal.gov").getFailedLoginAttempts(),
                "Only the known account records the failure");
        assertTrue(userAccountRepository.findByEmailIgnoreCase("ghost.unknown@portal.gov").isEmpty(),
                "Unknown email must not create an account");
    }

    @Test
    @DisplayName("password reset clears an active timed lock and the failure counter")
    void resetPassword_clearsTimedLockAndCounter() {
        String email = "lockout.resetpw@portal.gov";
        createAccountWithEmail(email);
        failLoginTimes(email, 5);
        assertEquals(FIXED_NOW.plus(Duration.ofSeconds(900)), reloadByEmail(email).getLockedUntil());

        String rawResetToken = "lockout-reset-token-1";
        passwordResetTokenRepository.saveAndFlush(new PasswordResetToken(
                reloadByEmail(email),
                authActionTokenService.hashToken(rawResetToken),
                FIXED_NOW.plus(30, ChronoUnit.MINUTES)));

        authService.resetPassword(new ResetPasswordRequest(rawResetToken, "NewPassword123!"));

        UserAccount afterReset = reloadByEmail(email);
        assertNull(afterReset.getLockedUntil(), "Password reset must clear the active timed lock");
        assertEquals(0, afterReset.getFailedLoginAttempts(), "Password reset must reset the counter");

        LoginResponse response = authService.login(new LoginRequest(email, "NewPassword123!")).response();
        assertNotNull(response.token(), "Login with the new password must succeed immediately after reset");
    }

    @Test
    @DisplayName("permanent LOCKED status remains locked regardless of expired or absent timed-lock state")
    void login_permanentlyLockedStatus_remainsLocked() {
        UserAccount manual = createAccountWithEmail("lockout.manual@portal.gov");
        manual.setAccountStatus(AccountStatus.LOCKED);
        userAccountRepository.saveAndFlush(manual);

        assertThrows(LockedException.class, () ->
                authService.login(new LoginRequest("lockout.manual@portal.gov", "Password123!")));

        UserAccount manualWithExpiredLock = reloadByEmail("lockout.manual@portal.gov");
        manualWithExpiredLock.setLockedUntil(FIXED_NOW.minusSeconds(3600));
        userAccountRepository.saveAndFlush(manualWithExpiredLock);
        entityManager.clear();

        assertThrows(LockedException.class, () ->
                authService.login(new LoginRequest("lockout.manual@portal.gov", "Password123!")),
                "Expired timed lock must not unlock a manually LOCKED account");
    }

    @Test
    @DisplayName("failed attempts during an active timed lock neither extend the lock nor mutate the counter")
    void login_failedAttemptsDuringActiveLock_doNotExtendLockOrCounter() {
        String email = "lockout.noextend@portal.gov";
        createAccountWithEmail(email);
        failLoginTimes(email, 5);
        Instant lockedUntilAtLockTime = reloadByEmail(email).getLockedUntil();
        assertEquals(FIXED_NOW.plus(Duration.ofSeconds(900)), lockedUntilAtLockTime);

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThrows(BadCredentialsException.class, () ->
                    authService.login(new LoginRequest(email, "WrongPassword123!")));
            entityManager.flush();
            entityManager.clear();
            UserAccount reloaded = reloadByEmail(email);
            assertEquals(lockedUntilAtLockTime, reloaded.getLockedUntil(),
                    "Attempt " + attempt + " during the active lock must leave locked_until exactly unchanged");
            assertEquals(0, reloaded.getFailedLoginAttempts(),
                    "Attempt " + attempt + " during the active lock must not start a new threshold cycle");
        }
    }
}
