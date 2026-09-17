package com.se2030.vaccinationportal.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.shared.auth.dto.CitizenRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ForgotPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ResendVerificationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ResetPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.VerifyEmailRequest;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.EmailVerificationToken;
import com.se2030.vaccinationportal.shared.auth.model.PasswordResetToken;
import com.se2030.vaccinationportal.shared.auth.model.RefreshTokenSession;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.EmailVerificationTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.PasswordResetTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.AuthActionTokenService;
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
    @DisplayName("Scenario 1: Citizen registration creates account in PENDING_VERIFICATION and blocks login with 403")
    void scenario1_citizenRegistration_pendingVerification_loginBlocked() throws Exception {
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

        List<EmailVerificationToken> tokens = emailVerificationTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        assertEquals(1, tokens.size());
        EmailVerificationToken token = tokens.get(0);
        assertNotNull(token.getTokenHash());
        assertNull(token.getUsedAt());
        assertTrue(token.getExpiresAt().isAfter(Instant.now()));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user.getEmail(), RAW_PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCOUNT_PENDING_VERIFICATION")));
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
