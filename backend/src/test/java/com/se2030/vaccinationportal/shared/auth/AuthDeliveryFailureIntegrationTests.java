package com.se2030.vaccinationportal.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.shared.auth.dto.CitizenRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ForgotPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ResendVerificationRequest;
import com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.EmailVerificationToken;
import com.se2030.vaccinationportal.shared.auth.model.PasswordResetToken;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.CitizenProfileRepository;
import com.se2030.vaccinationportal.shared.auth.repository.EmailVerificationTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.PasswordResetTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.AuthActionTokenService;
import com.se2030.vaccinationportal.shared.auth.service.AuthLinkDeliveryService;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying account enumeration safety and transactional rollback
 * when email link delivery fails (e.g. SMTP outages).
 * This test class is deliberately non-transactional so that real transaction commit/rollback
 * behaviors of AuthService and AuthController are observed and verified in the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthDeliveryFailureIntegrationTests {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @MockitoBean
    private AuthLinkDeliveryService authLinkDeliveryService;

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

    @Test
    @DisplayName("Forgot password during delivery outage returns HTTP 200 generic message and rolls back token transaction")
    void forgotPassword_smtpFailure_returns200AndRollsBackTokens() throws Exception {
        UserAccount user = createActiveCitizen("recovery.fail@example.com");

        String initialRawToken = "initial-reset-token-111";
        String initialTokenHash = authActionTokenService.hashToken(initialRawToken);
        passwordResetTokenRepository.saveAndFlush(
                new PasswordResetToken(user, initialTokenHash, Instant.now().plus(30, ChronoUnit.MINUTES)));

        doThrow(new AuthEmailDeliveryException("SMTP outage", new MailSendException("Connection refused")))
                .when(authLinkDeliveryService).sendPasswordResetLink(anyString(), anyString());

        ForgotPasswordRequest request = new ForgotPasswordRequest(user.getEmail());
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(AuthService.GENERIC_FORGOT_PASSWORD_MESSAGE)));

        // Verify transaction rolled back: exactly 1 token exists, it is the initial token, and usedAt is still null
        List<PasswordResetToken> allTokens = passwordResetTokenRepository.findAll();
        assertEquals(1, allTokens.size(), "No replacement token should be committed after mail failure");

        PasswordResetToken retainedToken = allTokens.get(0);
        assertEquals(initialTokenHash, retainedToken.getTokenHash());
        assertNull(retainedToken.getUsedAt(), "Initial token must not be marked consumed after mail failure rollback");

        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        assertEquals(1, activeTokens.size(), "Initial token must remain active");
    }

    @Test
    @DisplayName("Resend verification during delivery outage returns HTTP 200 generic message and rolls back token transaction")
    void resendVerification_smtpFailure_returns200AndRollsBackTokens() throws Exception {
        UserAccount user = createPendingCitizen("verify.fail@example.com");

        String initialRawToken = "initial-verify-token-222";
        String initialTokenHash = authActionTokenService.hashToken(initialRawToken);
        emailVerificationTokenRepository.saveAndFlush(
                new EmailVerificationToken(user, initialTokenHash, Instant.now().plus(24, ChronoUnit.HOURS)));

        doThrow(new AuthEmailDeliveryException("SMTP outage", new MailSendException("Connection refused")))
                .when(authLinkDeliveryService).sendEmailVerificationLink(anyString(), anyString());

        ResendVerificationRequest request = new ResendVerificationRequest(user.getEmail());
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(AuthService.GENERIC_RESEND_VERIFICATION_MESSAGE)));

        // Verify transaction rolled back: exactly 1 token exists, it is the initial token, and usedAt is still null
        List<EmailVerificationToken> allTokens = emailVerificationTokenRepository.findAll();
        assertEquals(1, allTokens.size(), "No replacement token should be committed after mail failure");

        EmailVerificationToken retainedToken = allTokens.get(0);
        assertEquals(initialTokenHash, retainedToken.getTokenHash());
        assertNull(retainedToken.getUsedAt(), "Initial token must not be marked consumed after mail failure rollback");

        List<EmailVerificationToken> activeTokens = emailVerificationTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        assertEquals(1, activeTokens.size(), "Initial token must remain active");
    }

    @Test
    @DisplayName("Citizen registration during delivery outage returns HTTP 503 and rolls back account registration")
    void registerCitizen_smtpFailure_returns503AndRollsBackRegistration() throws Exception {
        doThrow(new AuthEmailDeliveryException("SMTP outage", new MailSendException("Connection refused")))
                .when(authLinkDeliveryService).sendEmailVerificationLink(anyString(), anyString());

        CitizenRegistrationRequest request = new CitizenRegistrationRequest(
                "Nimal Perera", "nimal.failed@example.com", "0778899001", RAW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register/citizen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", is("EMAIL_DELIVERY_FAILED")));

        // Verify user account was rolled back
        assertTrue(userAccountRepository.findByEmailIgnoreCase("nimal.failed@example.com").isEmpty(),
                "User account must not be persisted when registration email delivery fails");

        // Verify no tokens were persisted
        assertTrue(emailVerificationTokenRepository.findAll().isEmpty(),
                "No verification token should be persisted when registration email delivery fails");
    }

    @Test
    @DisplayName("Enumeration matrix: forgot-password returns identical response across all states and failure modes")
    void forgotPassword_enumerationMatrix_identicalResponses() throws Exception {
        UserAccount activeUser = createActiveCitizen("active.enum@example.com");

        UserAccount rejectedUser = new UserAccount(
                "rejected.enum@example.com",
                passwordEncoder.encode(RAW_PASSWORD),
                AccountStatus.REJECTED
        );
        rejectedUser.setFullName("Rejected User");
        rejectedUser.setPhoneNumber("0773334444");
        rejectedUser.addRole(roleRepository.findByCode("ROLE_CITIZEN").orElseThrow());
        userAccountRepository.saveAndFlush(rejectedUser);

        // 1. Non-existent email
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("nonexistent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(AuthService.GENERIC_FORGOT_PASSWORD_MESSAGE)));

        // 2. Rejected account
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(rejectedUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(AuthService.GENERIC_FORGOT_PASSWORD_MESSAGE)));

        // 3. Active account with mail delivery success
        doNothing().when(authLinkDeliveryService).sendPasswordResetLink(anyString(), anyString());
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(activeUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(AuthService.GENERIC_FORGOT_PASSWORD_MESSAGE)));

        // 4. Active account with mail delivery outage
        doThrow(new AuthEmailDeliveryException("SMTP outage", new MailSendException("Connection refused")))
                .when(authLinkDeliveryService).sendPasswordResetLink(anyString(), anyString());
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(activeUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(AuthService.GENERIC_FORGOT_PASSWORD_MESSAGE)));
    }

    @Test
    @DisplayName("Enumeration matrix: resend-verification returns identical response across all states and failure modes")
    void resendVerification_enumerationMatrix_identicalResponses() throws Exception {
        UserAccount pendingUser = createPendingCitizen("pending.enum@example.com");
        UserAccount activeUser = createActiveCitizen("active.enum2@example.com");

        // 1. Non-existent email
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest("nonexistent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(AuthService.GENERIC_RESEND_VERIFICATION_MESSAGE)));

        // 2. Already active account (not pending)
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest(activeUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(AuthService.GENERIC_RESEND_VERIFICATION_MESSAGE)));

        // 3. Pending account with mail delivery success
        doNothing().when(authLinkDeliveryService).sendEmailVerificationLink(anyString(), anyString());
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest(pendingUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(AuthService.GENERIC_RESEND_VERIFICATION_MESSAGE)));

        // 4. Pending account with mail delivery outage
        doThrow(new AuthEmailDeliveryException("SMTP outage", new MailSendException("Connection refused")))
                .when(authLinkDeliveryService).sendEmailVerificationLink(anyString(), anyString());
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest(pendingUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(AuthService.GENERIC_RESEND_VERIFICATION_MESSAGE)));
    }

    private UserAccount createPendingCitizen(String email) {
        UserAccount user = new UserAccount(email, passwordEncoder.encode(RAW_PASSWORD), AccountStatus.PENDING_VERIFICATION);
        user.setFullName("Pending Citizen");
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
