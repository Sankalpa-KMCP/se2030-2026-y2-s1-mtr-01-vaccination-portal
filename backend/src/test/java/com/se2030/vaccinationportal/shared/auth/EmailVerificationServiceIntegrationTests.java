package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.dto.RegistrationResponse;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.EmailOtpChallenge;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.CitizenProfileRepository;
import com.se2030.vaccinationportal.shared.auth.repository.EmailOtpChallengeRepository;
import com.se2030.vaccinationportal.shared.auth.repository.EmailVerificationTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.PasswordResetTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import com.se2030.vaccinationportal.shared.auth.service.EmailOtpDeliveryService;
import com.se2030.vaccinationportal.shared.auth.service.EmailOtpResendResult;
import com.se2030.vaccinationportal.shared.auth.service.EmailOtpVerificationResult;
import com.se2030.vaccinationportal.shared.auth.service.EmailVerificationService;
import com.se2030.vaccinationportal.shared.auth.service.EmailVerificationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;

/**
 * Integration tests for the Auth-owned email OTP verification service.
 * Deliberately non-transactional so real commits are observed: attempt
 * increments, challenge rotation, and verification state must survive the
 * transaction boundary. Time is fully controlled through a stepped Clock so
 * expiry, cooldown, and resend tests are deterministic with no sleeps.
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class EmailVerificationServiceIntegrationTests {

    private static final Instant BASE_TIME = Instant.parse("2026-01-15T09:00:00Z");
    private static final String RAW_PASSWORD = "Password123!";

    private static final SteppingClock STEPPING_CLOCK = new SteppingClock(BASE_TIME);

    @TestConfiguration
    static class DeterministicClockConfiguration {
        @Bean
        @Primary
        Clock deterministicClock() {
            return STEPPING_CLOCK;
        }
    }

    static final class SteppingClock extends Clock {
        private final AtomicReference<Instant> current;

        SteppingClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        void reset() {
            current.set(BASE_TIME);
        }

        void advance(Duration duration) {
            current.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EmailOtpChallengeRepository challengeRepository;

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

    @MockitoBean
    private EmailOtpDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        cleanup();
        STEPPING_CLOCK.reset();
        roleRepository.saveAll(List.of(
                new Role("ROLE_ADMIN", "System Administrator"),
                new Role("ROLE_CITIZEN", "Citizen"),
                new Role("ROLE_HEALTHCARE_OFFICER", "Healthcare Officer")
        ));
        roleRepository.flush();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        refreshTokenSessionRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        challengeRepository.deleteAll();
        citizenProfileRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
    }

    private UserAccount createPendingCitizen(String email) {
        UserAccount account = new UserAccount(email, passwordEncoder.encode(RAW_PASSWORD), AccountStatus.PENDING_VERIFICATION);
        account.setFullName("Test Citizen");
        account.addRole(roleRepository.findByCode("ROLE_CITIZEN").orElseThrow());
        return userAccountRepository.saveAndFlush(account);
    }

    /** Issues the initial challenge and returns the delivered plaintext code. */
    private String issueCode(UserAccount account) {
        emailVerificationService.issueInitialChallenge(account.getId());
        return lastDeliveredCode();
    }

    private String lastDeliveredCode() {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(deliveryService, atLeastOnce()).sendVerificationOtp(anyString(), codeCaptor.capture(), any(Duration.class));
        return codeCaptor.getValue();
    }

    @Test
    @DisplayName("Initial challenge stores a password-hash (never plaintext), bound to the account email")
    void issueInitialChallenge_persistsHashedBoundChallenge() {
        UserAccount account = createPendingCitizen("challenge.persist@example.com");

        String otp = issueCode(account);

        EmailOtpChallenge challenge = challengeRepository.findByUser_Id(account.getId()).orElseThrow();
        assertEquals(account.getEmail(), challenge.getTargetEmail());
        assertNotEquals(otp, challenge.getCodeHash(), "The code must never be stored in plaintext");
        assertTrue(passwordEncoder.matches(otp, challenge.getCodeHash()));
        assertEquals(0, challenge.getAttemptCount());
        assertEquals(BASE_TIME, challenge.getIssuedAt());
        assertEquals(BASE_TIME.plus(Duration.ofMinutes(5)), challenge.getExpiresAt());
        assertNull(challenge.getConsumedAt());
        assertNull(challenge.getInvalidatedAt());
    }

    @Test
    @DisplayName("Staff provisioning creates no OTP challenge and sends no verification email")
    void staffProvisioning_createsNoChallenge() {
        RegistrationResponse staff = authService.registerStaffRequest(new StaffRegistrationRequest(
                "Staff Member", "staff.challenge@example.com", "0771231234", RAW_PASSWORD, "ROLE_HEALTHCARE_OFFICER"));

        assertTrue(challengeRepository.findByUser_Id(staff.id()).isEmpty(),
                "Only citizen self-registration receives a verification challenge");
        assertNull(userAccountRepository.findById(staff.id()).orElseThrow().getEmailVerifiedAt());
    }

    @Test
    @DisplayName("Valid active OTP marks the account verified, consumes the challenge, and cannot be reused")
    void verify_validCode_setsVerifiedAndConsumes() {
        UserAccount account = createPendingCitizen("verify.valid@example.com");
        String otp = issueCode(account);

        EmailOtpVerificationResult result = emailVerificationService.verify(account.getId(), otp);

        assertEquals(EmailOtpVerificationResult.VERIFIED, result);
        UserAccount verified = userAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(BASE_TIME, verified.getEmailVerifiedAt());
        assertEquals(AccountStatus.ACTIVE, verified.getAccountStatus());
        EmailOtpChallenge consumed = challengeRepository.findByUser_Id(account.getId()).orElseThrow();
        assertEquals(BASE_TIME, consumed.getConsumedAt());

        // Reuse attempt: the account is now verified, so the same code cannot verify anything again.
        assertEquals(EmailOtpVerificationResult.ALREADY_VERIFIED,
                emailVerificationService.verify(account.getId(), otp));
        assertEquals(BASE_TIME, userAccountRepository.findById(account.getId()).orElseThrow().getEmailVerifiedAt(),
                "Verification must never be unset or moved by later submissions");
    }

    @Test
    @DisplayName("Wrong OTP keeps the account unverified and the attempt count persists durably")
    void verify_wrongCode_incrementsAttemptsDurably() {
        UserAccount account = createPendingCitizen("verify.wrong@example.com");
        String otp = issueCode(account);
        String wrongCode = otp.equals("000000") ? "111111" : "000000";

        assertEquals(EmailOtpVerificationResult.INVALID,
                emailVerificationService.verify(account.getId(), wrongCode));

        UserAccount stillUnverified = userAccountRepository.findById(account.getId()).orElseThrow();
        assertNull(stillUnverified.getEmailVerifiedAt());
        assertEquals(AccountStatus.PENDING_VERIFICATION, stillUnverified.getAccountStatus());
        assertEquals(1, challengeRepository.findByUser_Id(account.getId()).orElseThrow().getAttemptCount(),
                "The wrong attempt must be committed, not rolled back with the error mapping");
    }

    @Test
    @DisplayName("After five wrong submissions the challenge is locked and the correct code is rejected")
    void verify_fiveWrongAttempts_locksChallenge() {
        UserAccount account = createPendingCitizen("verify.locked@example.com");
        String otp = issueCode(account);
        List<String> wrongCodes = fiveCodesDistinctFrom(otp);

        for (int attempt = 1; attempt <= 5; attempt++) {
            EmailOtpVerificationResult result = emailVerificationService.verify(account.getId(), wrongCodes.get(attempt - 1));
            if (attempt < 5) {
                assertEquals(EmailOtpVerificationResult.INVALID, result, "Attempt " + attempt);
            } else {
                assertEquals(EmailOtpVerificationResult.ATTEMPTS_EXCEEDED, result, "Attempt " + attempt);
            }
        }

        EmailOtpChallenge locked = challengeRepository.findByUser_Id(account.getId()).orElseThrow();
        assertEquals(5, locked.getAttemptCount());
        assertNotNull(locked.getInvalidatedAt(), "The exhausted challenge must be invalidated");

        // The correct original code is still rejected after the lock.
        assertEquals(EmailOtpVerificationResult.ATTEMPTS_EXCEEDED,
                emailVerificationService.verify(account.getId(), otp));
        assertNull(userAccountRepository.findById(account.getId()).orElseThrow().getEmailVerifiedAt());
    }

    @Test
    @DisplayName("Code verifies exactly until expiry: before expiry valid, at expiry expired")
    void verify_expiryBoundary_usesStrictInclusiveRule() {
        UserAccount expiringAccount = createPendingCitizen("verify.expiry@example.com");
        String expiringOtp = issueCode(expiringAccount);

        STEPPING_CLOCK.advance(Duration.ofSeconds(300));
        assertEquals(EmailOtpVerificationResult.EXPIRED,
                emailVerificationService.verify(expiringAccount.getId(), expiringOtp),
                "now == expiresAt must already be expired");
        assertNull(userAccountRepository.findById(expiringAccount.getId()).orElseThrow().getEmailVerifiedAt());

        STEPPING_CLOCK.reset();
        UserAccount validAccount = createPendingCitizen("verify.justbefore@example.com");
        String validOtp = issueCode(validAccount);

        STEPPING_CLOCK.advance(Duration.ofSeconds(299));
        assertEquals(EmailOtpVerificationResult.VERIFIED,
                emailVerificationService.verify(validAccount.getId(), validOtp),
                "One second before expiry the code must still verify");
    }

    @Test
    @DisplayName("Resend after the cooldown rotates the code: old code dies, new code verifies")
    void resend_afterCooldown_rotatesCode() {
        UserAccount account = createPendingCitizen("resend.rotate@example.com");
        String firstCode = issueCode(account);

        STEPPING_CLOCK.advance(Duration.ofSeconds(61));
        EmailOtpResendResult resend = emailVerificationService.resend(account.getId());
        assertEquals(EmailOtpResendResult.Outcome.SENT, resend.outcome());
        String secondCode = lastDeliveredCode();

        EmailOtpChallenge rotated = challengeRepository.findByUser_Id(account.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(secondCode, rotated.getCodeHash()),
                "The stored hash must belong to the newest code");
        assertEquals(BASE_TIME.plusSeconds(61).plus(Duration.ofMinutes(5)), rotated.getExpiresAt());
        assertEquals(0, rotated.getAttemptCount(), "Rotation resets the attempt budget");
        assertNull(rotated.getInvalidatedAt());

        assertNotEquals(firstCode, secondCode);
        assertEquals(EmailOtpVerificationResult.INVALID, emailVerificationService.verify(account.getId(), firstCode),
                "The previous code must stop working immediately after resend");
        assertEquals(EmailOtpVerificationResult.VERIFIED, emailVerificationService.verify(account.getId(), secondCode));
    }

    @Test
    @DisplayName("Resend inside the cooldown is rejected with retryAfterSeconds and leaves the challenge unchanged")
    void resend_beforeCooldown_rejectedAndUnchanged() {
        UserAccount account = createPendingCitizen("resend.cooldown@example.com");
        String firstCode = issueCode(account);
        String hashBefore = challengeRepository.findByUser_Id(account.getId()).orElseThrow().getCodeHash();

        STEPPING_CLOCK.advance(Duration.ofSeconds(30));
        EmailOtpResendResult resend = emailVerificationService.resend(account.getId());

        assertEquals(EmailOtpResendResult.Outcome.COOLDOWN, resend.outcome());
        assertEquals(30, resend.retryAfterSeconds());
        EmailOtpChallenge unchanged = challengeRepository.findByUser_Id(account.getId()).orElseThrow();
        assertEquals(hashBefore, unchanged.getCodeHash(), "A throttled resend must not touch the challenge");
        assertEquals(0, unchanged.getAttemptCount());

        // Exactly at the cooldown boundary the resend is allowed again.
        STEPPING_CLOCK.advance(Duration.ofSeconds(30));
        assertEquals(EmailOtpResendResult.Outcome.SENT, emailVerificationService.resend(account.getId()).outcome());
        assertNotEquals(firstCode, lastDeliveredCode());
    }

    @Test
    @DisplayName("Verified accounts are stable: status verified, resend and verify change nothing")
    void verifiedAccount_stable_noNewChallenges() {
        UserAccount account = createPendingCitizen("verified.stable@example.com");
        String otp = issueCode(account);
        assertEquals(EmailOtpVerificationResult.VERIFIED, emailVerificationService.verify(account.getId(), otp));
        EmailVerificationStatus verifiedStatus = emailVerificationService.getStatus(account.getId());
        assertTrue(verifiedStatus.emailVerified());
        assertEquals(BASE_TIME, verifiedStatus.emailVerifiedAt());

        assertEquals(EmailOtpResendResult.Outcome.ALREADY_VERIFIED,
                emailVerificationService.resend(account.getId()).outcome(),
                "A verified account must not receive additional codes");
        assertEquals(EmailOtpVerificationResult.ALREADY_VERIFIED,
                emailVerificationService.verify(account.getId(), otp));

        EmailOtpChallenge challenge = challengeRepository.findByUser_Id(account.getId()).orElseThrow();
        assertEquals(BASE_TIME, challenge.getConsumedAt(), "The consumed challenge must remain consumed");
        assertEquals(0, challenge.getAttemptCount());
    }

    @Test
    @DisplayName("A code issued for a different email address can never verify the account")
    void verify_emailMismatch_invalidatesChallenge() {
        UserAccount account = createPendingCitizen("verify.binding@example.com");
        String foreignHash = passwordEncoder.encode("123456");
        challengeRepository.saveAndFlush(new EmailOtpChallenge(
                account, foreignHash, "old.address@example.com", BASE_TIME, BASE_TIME.plus(Duration.ofMinutes(5))));

        assertEquals(EmailOtpVerificationResult.INVALID,
                emailVerificationService.verify(account.getId(), "123456"),
                "A code bound to another email address must not verify the account");
        EmailOtpChallenge invalidated = challengeRepository.findByUser_Id(account.getId()).orElseThrow();
        assertNotNull(invalidated.getInvalidatedAt(), "The mismatched challenge must be invalidated");
    }

    @Test
    @DisplayName("Delivery failure after commit keeps the account and challenge and never logs the code")
    void deliveryFailure_keepsAccountAndChallenge(CapturedOutput output) {
        UserAccount account = createPendingCitizen("delivery.fail@example.com");
        doThrow(new AuthEmailDeliveryException("SMTP outage", new MailSendException("Connection refused")))
                .when(deliveryService).sendVerificationOtp(anyString(), anyString(), any(Duration.class));

        emailVerificationService.issueInitialChallenge(account.getId());

        assertTrue(challengeRepository.findByUser_Id(account.getId()).isPresent(),
                "The challenge must survive a delivery outage so the citizen can resend later");
        UserAccount persisted = userAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(AccountStatus.PENDING_VERIFICATION, persisted.getAccountStatus());

        String otp = lastDeliveredCode();
        String logs = output.getAll();
        assertFalse(logs.contains(otp), "The plaintext code must never appear in delivery failure logs");
        assertTrue(logs.contains("AuthEmailDeliveryException"), "Only the failure class may be reported");
    }

    @Test
    @DisplayName("Status reads reflect the verification state")
    void getStatus_reflectsState() {
        UserAccount account = createPendingCitizen("status.reads@example.com");

        EmailVerificationStatus before = emailVerificationService.getStatus(account.getId());
        assertFalse(before.emailVerified());
        assertNull(before.emailVerifiedAt());

        String otp = issueCode(account);
        emailVerificationService.verify(account.getId(), otp);

        EmailVerificationStatus after = emailVerificationService.getStatus(account.getId());
        assertTrue(after.emailVerified());
        assertEquals(BASE_TIME, after.emailVerifiedAt());
    }

    private static List<String> fiveCodesDistinctFrom(String realCode) {
        List<String> candidates = List.of("000000", "111111", "222222", "333333", "444444", "555555", "654321");
        return candidates.stream().filter(code -> !code.equals(realCode)).limit(5).toList();
    }
}
