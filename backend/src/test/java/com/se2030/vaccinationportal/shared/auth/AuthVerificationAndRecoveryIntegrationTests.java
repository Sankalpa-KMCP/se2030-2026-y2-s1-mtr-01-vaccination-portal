package com.se2030.vaccinationportal.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.shared.auth.dto.CitizenRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ForgotPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ResendVerificationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ResetPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.VerifyEmailRequest;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.EmailVerificationToken;
import com.se2030.vaccinationportal.shared.auth.model.PasswordResetToken;
import com.se2030.vaccinationportal.shared.auth.model.RefreshTokenSession;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.EmailOtpChallengeRepository;
import com.se2030.vaccinationportal.shared.auth.repository.EmailVerificationTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.PasswordResetTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.AuthActionTokenService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthVerificationAndRecoveryIntegrationTests {

    private static final String RAW_PASSWORD = "Password123!";
    private static final String NEW_PASSWORD = "NewValidPassword456!";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private EmailOtpChallengeRepository emailOtpChallengeRepository;

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
        refreshTokenSessionRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        emailOtpChallengeRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.flush();

        roleRepository.saveAll(List.of(
                new Role("ROLE_ADMIN", "System Administrator"),
                new Role("ROLE_CENTER_MANAGER", "Vaccination Center Manager"),
                new Role("ROLE_HEALTHCARE_OFFICER", "Healthcare Officer"),
                new Role("ROLE_CAMPAIGN_OFFICER", "Campaign Officer"),
                new Role("ROLE_INVENTORY_OFFICER", "Inventory Officer"),
                new Role("ROLE_CITIZEN", "Citizen"),
                new Role("ROLE_MANAGEMENT_USER", "Management User")
        ));
    }

    @Test
    @DisplayName("Scenario 1: Citizen registration creates a PENDING_VERIFICATION account with an OTP challenge and allows login")
    void scenario1_citizenRegistration_pendingVerificationWithOtpChallenge_loginAllowed() throws Exception {
        CitizenRegistrationRequest request = new CitizenRegistrationRequest(
                "Kasun Silva", "kasun.silva@example.com", "0771122334", RAW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register/citizen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountStatus", is("PENDING_VERIFICATION")))
                .andExpect(jsonPath("$.email", is("kasun.silva@example.com")));

        UserAccount user = userAccountRepository.findByEmailIgnoreCase("kasun.silva@example.com").orElseThrow();
        assertEquals(AccountStatus.PENDING_VERIFICATION, user.getAccountStatus());
        assertNull(user.getEmailVerifiedAt());

        // Registration now issues the Auth-owned OTP challenge instead of a link token.
        assertTrue(emailOtpChallengeRepository.findByUser_Id(user.getId()).isPresent(),
                "Registration must create the current OTP challenge");
        assertTrue(challengeTargetMatches(user), "The challenge must be bound to the registered email");
        assertTrue(emailVerificationTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId()).isEmpty(),
                "The legacy link-token flow is no longer part of registration");

        // An unverified citizen may log in so the principal-scoped OTP verification API is reachable.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user.getEmail(), RAW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.accountStatus", is("PENDING_VERIFICATION")));
    }

    private boolean challengeTargetMatches(UserAccount user) {
        return emailOtpChallengeRepository.findByUser_Id(user.getId())
                .map(challenge -> user.getEmail().equals(challenge.getTargetEmail()))
                .orElse(false);
    }

    @Test
    @DisplayName("Scenario 2: Valid email verification activates account and enables login")
    void scenario2_validToken_verifiesEmail_activatesAccount() throws Exception {
        UserAccount user = createPendingCitizen("verify.me@example.com");
        String rawToken = "raw-deterministic-verification-token-12345";
        String tokenHash = authActionTokenService.hashToken(rawToken);
        emailVerificationTokenRepository.saveAndFlush(
                new EmailVerificationToken(user, tokenHash, Instant.now().plus(24, ChronoUnit.HOURS)));

        VerifyEmailRequest request = new VerifyEmailRequest(rawToken);
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Email verified successfully")));

        UserAccount updatedUser = userAccountRepository.findById(user.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, updatedUser.getAccountStatus());
        assertNotNull(updatedUser.getEmailVerifiedAt());

        EmailVerificationToken consumedToken = emailVerificationTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertTrue(consumedToken.isConsumed());
        assertNotNull(consumedToken.getUsedAt());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user.getEmail(), RAW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.accountStatus", is("ACTIVE")));
    }

    @Test
    @DisplayName("Scenario 3: Consumed token reuse is rejected with 400 TOKEN_ALREADY_USED")
    void scenario3_consumedToken_reuseRejected() throws Exception {
        UserAccount user = createPendingCitizen("reuse.token@example.com");
        String rawToken = "token-to-be-consumed-twice";
        String tokenHash = authActionTokenService.hashToken(rawToken);
        EmailVerificationToken token = new EmailVerificationToken(user, tokenHash, Instant.now().plus(24, ChronoUnit.HOURS));
        token.markUsed(Instant.now());
        emailVerificationTokenRepository.saveAndFlush(token);

        VerifyEmailRequest request = new VerifyEmailRequest(rawToken);
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("TOKEN_ALREADY_USED")));
    }

    @Test
    @DisplayName("Scenario 4: Expired verification token is rejected with 400 TOKEN_EXPIRED")
    void scenario4_expiredToken_rejected() throws Exception {
        UserAccount user = createPendingCitizen("expired.token@example.com");
        String rawToken = "expired-token-val";
        String tokenHash = authActionTokenService.hashToken(rawToken);
        EmailVerificationToken token = new EmailVerificationToken(user, tokenHash, Instant.now().minus(1, ChronoUnit.HOURS));
        emailVerificationTokenRepository.saveAndFlush(token);

        VerifyEmailRequest request = new VerifyEmailRequest(rawToken);
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("TOKEN_EXPIRED")));
    }

    @Test
    @DisplayName("Scenario 5: Resend verification invalidates old tokens and returns generic response")
    void scenario5_resendVerification_invalidatesOldTokens_genericResponse() throws Exception {
        UserAccount user = createPendingCitizen("resend.test@example.com");
        String oldRawToken = "old-verification-token";
        emailVerificationTokenRepository.saveAndFlush(
                new EmailVerificationToken(user, authActionTokenService.hashToken(oldRawToken), Instant.now().plus(24, ChronoUnit.HOURS)));

        ResendVerificationRequest request = new ResendVerificationRequest(user.getEmail());
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If the account exists and is pending verification, a verification email has been sent.")));

        // Old token must now be marked consumed
        EmailVerificationToken oldToken = emailVerificationTokenRepository.findByTokenHash(authActionTokenService.hashToken(oldRawToken)).orElseThrow();
        assertTrue(oldToken.isConsumed());

        // A new active token must exist
        List<EmailVerificationToken> activeTokens = emailVerificationTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        assertEquals(1, activeTokens.size());
        assertNotEquals(oldToken.getTokenHash(), activeTokens.get(0).getTokenHash());

        // Generic response for unknown account
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest("unknown@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If the account exists and is pending verification, a verification email has been sent.")));
    }

    @Test
    @DisplayName("Scenario 6: Forgot password generates single-use token and returns enumeration-safe response")
    void scenario6_forgotPassword_generatesToken_enumerationSafe() throws Exception {
        UserAccount user = createActiveCitizen("forgot.pw@example.com");

        ForgotPasswordRequest request = new ForgotPasswordRequest(user.getEmail());
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If an eligible account is associated with this email address, password reset instructions have been sent.")));

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        assertEquals(1, tokens.size());
        PasswordResetToken token = tokens.get(0);
        assertNotNull(token.getTokenHash());
        assertNull(token.getUsedAt());
        // Should expire within ~30 minutes
        assertTrue(token.getExpiresAt().isAfter(Instant.now().plus(25, ChronoUnit.MINUTES)));
        assertTrue(token.getExpiresAt().isBefore(Instant.now().plus(35, ChronoUnit.MINUTES)));

        // Non-existent email returns same generic message
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("nonexistent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If an eligible account is associated with this email address, password reset instructions have been sent.")));
    }

    @Test
    @DisplayName("Scenario 7: Password reset updates credentials, revokes sessions, and marks token used")
    void scenario7_passwordReset_updatesPassword_revokesSessions() throws Exception {
        UserAccount user = createActiveCitizen("reset.pw@example.com");

        // Seed an active refresh token session
        RefreshTokenSession session = new RefreshTokenSession(
                user, "family-1", authActionTokenService.hashToken("refresh-raw-1"), Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenSessionRepository.saveAndFlush(session);
        assertNull(session.getRevokedAt());

        String rawResetToken = "valid-reset-token-777";
        String tokenHash = authActionTokenService.hashToken(rawResetToken);
        passwordResetTokenRepository.saveAndFlush(
                new PasswordResetToken(user, tokenHash, Instant.now().plus(30, ChronoUnit.MINUTES)));

        ResetPasswordRequest request = new ResetPasswordRequest(rawResetToken, NEW_PASSWORD);
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password has been reset successfully")));

        // Token must be marked used
        PasswordResetToken consumedToken = passwordResetTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertTrue(consumedToken.isConsumed());

        // Refresh token session must be revoked
        RefreshTokenSession updatedSession = refreshTokenSessionRepository.findById(session.getId()).orElseThrow();
        assertTrue(updatedSession.isRevoked());
        assertNotNull(updatedSession.getRevokedAt());

        // Old password fails
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user.getEmail(), RAW_PASSWORD))))
                .andExpect(status().isUnauthorized());

        // New password succeeds
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user.getEmail(), NEW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    @DisplayName("Scenario 8: Backward compatibility - Pre-existing active account with email_verified_at NULL logs in seamlessly")
    void scenario8_legacyActiveAccount_nullEmailVerifiedAt_logsInSuccessfully() throws Exception {
        UserAccount legacyUser = new UserAccount(
                "legacy.active@example.com",
                passwordEncoder.encode(RAW_PASSWORD),
                AccountStatus.ACTIVE
        );
        legacyUser.setFullName("Legacy Citizen");
        legacyUser.setPhoneNumber("0779998877");
        legacyUser.setEmailVerifiedAt(null);
        legacyUser.addRole(roleRepository.findByCode("ROLE_CITIZEN").orElseThrow());
        userAccountRepository.saveAndFlush(legacyUser);

        assertNull(legacyUser.getEmailVerifiedAt());
        assertEquals(AccountStatus.ACTIVE, legacyUser.getAccountStatus());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("legacy.active@example.com", RAW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is("legacy.active@example.com")))
                .andExpect(jsonPath("$.user.accountStatus", is("ACTIVE")));
    }

    @Test
    @DisplayName("Password reset with expired token is rejected with 400 TOKEN_EXPIRED")
    void passwordReset_expiredToken_rejected() throws Exception {
        UserAccount user = createActiveCitizen("expired.reset@example.com");
        String rawToken = "expired-reset-token";
        passwordResetTokenRepository.saveAndFlush(
                new PasswordResetToken(user, authActionTokenService.hashToken(rawToken), Instant.now().minus(5, ChronoUnit.MINUTES)));

        ResetPasswordRequest request = new ResetPasswordRequest(rawToken, NEW_PASSWORD);
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("TOKEN_EXPIRED")));
    }

    @Test
    @DisplayName("Password reset validation rejects short passwords")
    void passwordReset_shortPassword_validationFailed() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("some-token", "short");
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors.newPassword").isArray());
    }

    @Test
    @DisplayName("Verification token for an ACTIVE account is rejected with 400 INVALID_TOKEN and left unconsumed")
    void verifyEmail_activeAccount_rejectedWithInvalidToken() throws Exception {
        UserAccount user = createActiveCitizen("already.active@example.com");
        Instant initialVerifiedAt = user.getEmailVerifiedAt();
        String rawToken = "token-for-already-active-user";
        String tokenHash = authActionTokenService.hashToken(rawToken);
        emailVerificationTokenRepository.saveAndFlush(
                new EmailVerificationToken(user, tokenHash, Instant.now().plus(24, ChronoUnit.HOURS)));

        VerifyEmailRequest request = new VerifyEmailRequest(rawToken);
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_TOKEN")));

        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertFalse(token.isConsumed());
        assertNull(token.getUsedAt());

        UserAccount refreshedUser = userAccountRepository.findById(user.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, refreshedUser.getAccountStatus());
        assertEquals(initialVerifiedAt, refreshedUser.getEmailVerifiedAt());
    }

    @Test
    @DisplayName("Verification token for a PENDING_APPROVAL account is rejected with 400 INVALID_TOKEN and left unconsumed")
    void verifyEmail_pendingApprovalAccount_rejectedWithInvalidToken() throws Exception {
        UserAccount user = new UserAccount("staff.pending@example.com", passwordEncoder.encode(RAW_PASSWORD), AccountStatus.PENDING_APPROVAL);
        user.setFullName("Pending Staff");
        user.setPhoneNumber("0770003333");
        user = userAccountRepository.saveAndFlush(user);

        String rawToken = "token-for-pending-approval-user";
        String tokenHash = authActionTokenService.hashToken(rawToken);
        emailVerificationTokenRepository.saveAndFlush(
                new EmailVerificationToken(user, tokenHash, Instant.now().plus(24, ChronoUnit.HOURS)));

        VerifyEmailRequest request = new VerifyEmailRequest(rawToken);
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_TOKEN")));

        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertFalse(token.isConsumed());
        assertNull(token.getUsedAt());

        UserAccount refreshedUser = userAccountRepository.findById(user.getId()).orElseThrow();
        assertEquals(AccountStatus.PENDING_APPROVAL, refreshedUser.getAccountStatus());
        assertNull(refreshedUser.getEmailVerifiedAt());
    }

    @Test
    @DisplayName("Verification token for a REJECTED account is rejected with 400 INVALID_TOKEN and left unconsumed")
    void verifyEmail_rejectedAccount_rejectedWithInvalidToken() throws Exception {
        UserAccount user = new UserAccount("rejected.user@example.com", passwordEncoder.encode(RAW_PASSWORD), AccountStatus.REJECTED);
        user.setFullName("Rejected User");
        user.setPhoneNumber("0770004444");
        user = userAccountRepository.saveAndFlush(user);

        String rawToken = "token-for-rejected-user";
        String tokenHash = authActionTokenService.hashToken(rawToken);
        emailVerificationTokenRepository.saveAndFlush(
                new EmailVerificationToken(user, tokenHash, Instant.now().plus(24, ChronoUnit.HOURS)));

        VerifyEmailRequest request = new VerifyEmailRequest(rawToken);
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_TOKEN")));

        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertFalse(token.isConsumed());
        assertNull(token.getUsedAt());

        UserAccount refreshedUser = userAccountRepository.findById(user.getId()).orElseThrow();
        assertEquals(AccountStatus.REJECTED, refreshedUser.getAccountStatus());
        assertNull(refreshedUser.getEmailVerifiedAt());
    }

    @Test
    @DisplayName("Password reset accepts 8-character minimum password and rejects 129 character password")
    void passwordReset_boundaryPasswordLengths() throws Exception {
        UserAccount user = createActiveCitizen("boundary.pw@example.com");

        // 129 chars rejected
        ResetPasswordRequest requestOver = new ResetPasswordRequest("some-token", "a".repeat(129));
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestOver)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors.newPassword", hasItem("Password must be between 8 and 128 characters")));

        // 8 chars accepted
        String validMinToken = "token-for-8-char-reset";
        passwordResetTokenRepository.saveAndFlush(
                new PasswordResetToken(user, authActionTokenService.hashToken(validMinToken), Instant.now().plus(30, ChronoUnit.MINUTES)));

        ResetPasswordRequest requestMin = new ResetPasswordRequest(validMinToken, "a".repeat(8));
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestMin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password has been reset successfully")));
    }

    @Test
    @DisplayName("Password length boundaries at 8, 128, and 129 are verified across auth DTOs")
    void passwordDtoValidationBoundaries() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        // CitizenRegistrationRequest
        assertEquals(1, validator.validate(new CitizenRegistrationRequest("User", "u@e.com", "0771234567", "1234567")).size());
        assertEquals(0, validator.validate(new CitizenRegistrationRequest("User", "u@e.com", "0771234567", "a".repeat(8))).size());
        assertEquals(0, validator.validate(new CitizenRegistrationRequest("User", "u@e.com", "0771234567", "a".repeat(128))).size());
        assertEquals(1, validator.validate(new CitizenRegistrationRequest("User", "u@e.com", "0771234567", "a".repeat(129))).size());

        // StaffRegistrationRequest
        assertEquals(1, validator.validate(new StaffRegistrationRequest("User", "u@e.com", "0771234567", "1234567", "ROLE_HEALTHCARE_OFFICER")).size());
        assertEquals(0, validator.validate(new StaffRegistrationRequest("User", "u@e.com", "0771234567", "a".repeat(8), "ROLE_HEALTHCARE_OFFICER")).size());
        assertEquals(0, validator.validate(new StaffRegistrationRequest("User", "u@e.com", "0771234567", "a".repeat(128), "ROLE_HEALTHCARE_OFFICER")).size());
        assertEquals(1, validator.validate(new StaffRegistrationRequest("User", "u@e.com", "0771234567", "a".repeat(129), "ROLE_HEALTHCARE_OFFICER")).size());

        // ResetPasswordRequest
        assertEquals(1, validator.validate(new ResetPasswordRequest("token", "1234567")).size());
        assertEquals(0, validator.validate(new ResetPasswordRequest("token", "a".repeat(8))).size());
        assertEquals(0, validator.validate(new ResetPasswordRequest("token", "a".repeat(128))).size());
        assertEquals(1, validator.validate(new ResetPasswordRequest("token", "a".repeat(129))).size());
    }

    private UserAccount createPendingCitizen(String email) {
        UserAccount user = new UserAccount(email, passwordEncoder.encode(RAW_PASSWORD), AccountStatus.PENDING_VERIFICATION);
        user.setFullName("Test User");
        user.setPhoneNumber("0770001111");
        user.addRole(roleRepository.findByCode("ROLE_CITIZEN").orElseThrow());
        return userAccountRepository.saveAndFlush(user);
    }

    private UserAccount createActiveCitizen(String email) {
        UserAccount user = new UserAccount(email, passwordEncoder.encode(RAW_PASSWORD), AccountStatus.ACTIVE);
        user.setFullName("Active Citizen");
        user.setPhoneNumber("0770002222");
        user.setEmailVerifiedAt(Instant.now());
        user.addRole(roleRepository.findByCode("ROLE_CITIZEN").orElseThrow());
        return userAccountRepository.saveAndFlush(user);
    }
}
