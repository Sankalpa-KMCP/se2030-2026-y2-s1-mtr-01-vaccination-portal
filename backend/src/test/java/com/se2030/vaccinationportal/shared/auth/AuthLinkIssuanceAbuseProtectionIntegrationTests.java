package com.se2030.vaccinationportal.shared.auth;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.shared.auth.dto.ForgotPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ResendVerificationRequest;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.EmailVerificationToken;
import com.se2030.vaccinationportal.shared.auth.model.PasswordResetToken;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.EmailVerificationTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.PasswordResetTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for bounded abuse protection on public authentication link flows (R06):
 * repeated forgot-password / resend-verification requests inside the per-identity cooldown must
 * neither rotate the outstanding token nor mint additional tokens, while the public response stays
 * generic (non-enumerating) in every state. A short cooldown is configured for this context so the
 * "cooldown expiry permits legitimate retry" scenario can be exercised without long waits.
 */
@SpringBootTest(properties = "app.security.auth.link-issuance-cooldown-seconds=1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthLinkIssuanceAbuseProtectionIntegrationTests {

    private static final String GENERIC_FORGOT =
            com.se2030.vaccinationportal.shared.auth.service.AuthService.GENERIC_FORGOT_PASSWORD_MESSAGE;
    private static final String GENERIC_RESEND =
            com.se2030.vaccinationportal.shared.auth.service.AuthService.GENERIC_RESEND_VERIFICATION_MESSAGE;

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
    private com.se2030.vaccinationportal.shared.auth.service.AuthLinkIssuanceThrottle authLinkIssuanceThrottle;

    @Autowired
    private com.se2030.vaccinationportal.shared.auth.service.AuthService authService;

    @BeforeEach
    void setUp() {
        emailVerificationTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.flush();
        roleRepository.saveAll(List.of(
                new Role("ROLE_CITIZEN", "Citizen")
        ));
    }

    @AfterEach
    void tearDown() {
        emailVerificationTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
    }

    private UserAccount createPendingCitizen(String email) {
        UserAccount account = new UserAccount(email, "Password123!", AccountStatus.PENDING_VERIFICATION);
        account.setFullName("R06 Test Citizen");
        account.addRole(roleRepository.findByCode("ROLE_CITIZEN").orElseThrow());
        return userAccountRepository.saveAndFlush(account);
    }

    @Test
    @DisplayName("R06: normal forgot-password issues exactly one reset token")
    void forgotPassword_normalRequest_issuesOneToken() throws Exception {
        UserAccount user = createPendingCitizen("r06.forgot.normal@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(user.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokens.size());
    }

    @Test
    @DisplayName("R06: repeated forgot-password inside cooldown does not rotate the outstanding token")
    void forgotPassword_repeatInsideCooldown_doesNotRotateToken() throws Exception {
        UserAccount user = createPendingCitizen("r06.forgot.repeat@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(user.getEmail()))))
                .andExpect(status().isOk());

        List<PasswordResetToken> tokensAfterFirst = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterFirst.size());
        String outstandingTokenHash = tokensAfterFirst.get(0).getTokenHash();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(user.getEmail()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));
        }

        List<PasswordResetToken> tokensAfterRepeats = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterRepeats.size(),
                "Repeated requests inside the cooldown must not mint additional tokens");
        org.junit.jupiter.api.Assertions.assertEquals(outstandingTokenHash, tokensAfterRepeats.get(0).getTokenHash(),
                "The legitimate outstanding token must not be invalidated by throttled requests");
        org.junit.jupiter.api.Assertions.assertNull(tokensAfterRepeats.get(0).getUsedAt(),
                "The outstanding token must remain unconsumed while the cooldown holds");
    }

    @Test
    @DisplayName("R06: repeated resend-verification inside cooldown does not rotate the outstanding token")
    void resendVerification_repeatInsideCooldown_doesNotRotateToken() throws Exception {
        UserAccount user = createPendingCitizen("r06.resend.repeat@example.com");

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest(user.getEmail()))))
                .andExpect(status().isOk());

        List<EmailVerificationToken> tokensAfterFirst = emailVerificationTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterFirst.size());
        String outstandingTokenHash = tokensAfterFirst.get(0).getTokenHash();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResendVerificationRequest(user.getEmail()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is(GENERIC_RESEND)));
        }

        List<EmailVerificationToken> tokensAfterRepeats = emailVerificationTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterRepeats.size(),
                "Repeated requests inside the cooldown must not mint additional tokens");
        org.junit.jupiter.api.Assertions.assertEquals(outstandingTokenHash, tokensAfterRepeats.get(0).getTokenHash(),
                "The legitimate outstanding token must not be invalidated by throttled requests");
    }

    @Test
    @DisplayName("R06: throttling preserves the generic non-enumerating response for existing and unknown accounts")
    void throttling_doesNotRevealAccountExistence() throws Exception {
        UserAccount user = createPendingCitizen("r06.enum.existing@example.com");
        String unknownEmail = "r06.enum.unknown@example.com";

        // Prime the cooldown for both identities
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(user.getEmail()))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(unknownEmail))))
                .andExpect(status().isOk());

        // Throttled repeats must return byte-identical generic messages regardless of existence
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(user.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(unknownEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));
    }

    @Test
    @DisplayName("R06: cooldown expiry permits a legitimate retry that rotates the token")
    void cooldownExpiry_permitsLegitimateRetry() throws Exception {
        UserAccount user = createPendingCitizen("r06.forgot.expiry@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(user.getEmail()))))
                .andExpect(status().isOk());

        // Blocked inside the 1s cooldown
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(user.getEmail()))))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(1,
                passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId()).size());

        // Wait past the short cooldown, then the retry legitimately rotates the token
        Thread.sleep(1200);
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(user.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));

        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, activeTokens.size(),
                "Retry after cooldown expiry must rotate to exactly one fresh outstanding token");
        org.junit.jupiter.api.Assertions.assertTrue(activeTokens.get(0).getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("F2: identity flood at throttle capacity returns generic response and preserves victim cooldown")
    void throttling_identityFlood_preservesGenericResponseAndVictimCooldown() throws Exception {
        UserAccount victim = createPendingCitizen("victim.user@example.com");

        // 1. Victim requests password reset -> succeeds, token issued
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(victim.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));

        List<PasswordResetToken> initialTokens = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(victim.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, initialTokens.size());
        String initialTokenHash = initialTokens.get(0).getTokenHash();

        // 2. Flood with multiple distinct identities
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("flood" + i + "@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));
        }

        // 3. Victim requests again inside cooldown -> must receive generic message, token must NOT be rotated
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(victim.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));

        List<PasswordResetToken> tokensAfterFlood = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(victim.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterFlood.size(),
                "Victim's token count must remain 1 — flood must not evict victim's active cooldown (F2)");
        org.junit.jupiter.api.Assertions.assertEquals(initialTokenHash, tokensAfterFlood.get(0).getTokenHash(),
                "Victim's outstanding token must not rotate during cooldown, despite flood");
    }

    @Test
    @DisplayName("F2-DOS: unique nonexistent requests do NOT consume account throttle capacity")
    void nonexistentRequests_doNotConsumeAccountThrottleCapacity() throws Exception {
        int initialTracked = authLinkIssuanceThrottle.trackedKeyCount();

        for (int i = 0; i < 50; i++) {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("nonexistent.forgot." + i + "@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));

            mockMvc.perform(post("/api/v1/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResendVerificationRequest("nonexistent.resend." + i + "@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is(GENERIC_RESEND)));
        }

        org.junit.jupiter.api.Assertions.assertEquals(initialTracked, authLinkIssuanceThrottle.trackedKeyCount(),
                "Nonexistent accounts must not consume any slots in the account throttle (F2-DOS)");
    }

    @Test
    @DisplayName("F2-DOS: previously unseen real account receives recovery link after nonexistent identity flood")
    void unseenRealAccount_receivesRecoveryLink_afterNonexistentFlood() throws Exception {
        // 1. Flood with 50 unique nonexistent identities
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("flood.nonexistent." + i + "@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));
        }

        // 2. Previously unseen legitimate citizen requests recovery
        UserAccount unseenUser = createPendingCitizen("unseen.citizen@example.com");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(unseenUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));

        // 3. Outstanding token must be issued despite the flood
        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(unseenUser.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokens.size(),
                "Unseen legitimate citizen must successfully receive recovery token after nonexistent flood (F2-DOS)");
    }

    @Test
    @DisplayName("F2-DOS: previously unseen pending account receives verification link after nonexistent identity flood")
    void unseenPendingAccount_receivesVerificationLink_afterNonexistentFlood() throws Exception {
        // 1. Flood with 50 unique nonexistent identities
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(post("/api/v1/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResendVerificationRequest("flood.nonexistent.resend." + i + "@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is(GENERIC_RESEND)));
        }

        // 2. Previously unseen legitimate pending citizen requests verification resend
        UserAccount pendingCitizen = createPendingCitizen("unseen.pending.citizen@example.com");
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest(pendingCitizen.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_RESEND)));

        // 3. New verification token must be issued
        List<EmailVerificationToken> tokens = emailVerificationTokenRepository.findAllByUser_IdAndUsedAtIsNull(pendingCitizen.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokens.size(),
                "Unseen pending citizen must successfully receive fresh verification token after nonexistent flood (F2-DOS)");
    }

    @Test
    @DisplayName("F2-DOS: external status and message body are identical across existing, nonexistent, cooldown, and flood conditions")
    void responseStatusAndBody_areIdentical_acrossAllConditions() throws Exception {
        UserAccount realUser = createPendingCitizen("real.user.identical@example.com");
        String nonexistentEmail = "nonexistent.user.identical@example.com";

        // Condition 1: existing account initial request
        String res1 = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(realUser.getEmail()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Condition 2: nonexistent account request
        String res2 = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(nonexistentEmail))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Condition 3: existing account repeat request inside cooldown
        String res3 = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(realUser.getEmail()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Condition 4: flood request
        String res4 = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("flood.identity.999@example.com"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(res1, res2, "Existing vs nonexistent response must be identical");
        org.junit.jupiter.api.Assertions.assertEquals(res1, res3, "Existing vs cooldown response must be identical");
        org.junit.jupiter.api.Assertions.assertEquals(res1, res4, "Existing vs flood response must be identical");
    }

    @Test
    @DisplayName("F2-ALIAS: forgot-password case variant aliases share same account cooldown and do not rotate token")
    void forgotPassword_caseVariantAliases_shareSameAccountCooldown_andDoNotRotateToken() throws Exception {
        UserAccount user = createPendingCitizen("case.forgot@example.com");

        // Request 1: lowercase spelling
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("case.forgot@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));

        List<PasswordResetToken> tokensAfterFirst = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterFirst.size());
        String initialTokenHash = tokensAfterFirst.get(0).getTokenHash();

        // Request 2: uppercase spelling inside cooldown
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("CASE.FORGOT@EXAMPLE.COM"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));

        // Request 3: mixed-case spelling inside cooldown
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("CaSe.FoRgOt@ExAmPlE.cOm"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));

        List<PasswordResetToken> tokensAfterAliases = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterAliases.size(),
                "Case-variant requests inside cooldown must not mint additional tokens");
        org.junit.jupiter.api.Assertions.assertEquals(initialTokenHash, tokensAfterAliases.get(0).getTokenHash(),
                "Case-variant requests must not rotate the existing outstanding token (F2-ALIAS)");
    }

    @Test
    @DisplayName("F2-ALIAS: resend-verification case variant aliases share same account cooldown and do not rotate token")
    void resendVerification_caseVariantAliases_shareSameAccountCooldown_andDoNotRotateToken() throws Exception {
        UserAccount user = createPendingCitizen("case.resend@example.com");

        // Request 1: lowercase spelling
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest("case.resend@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_RESEND)));

        List<EmailVerificationToken> tokensAfterFirst = emailVerificationTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterFirst.size());
        String initialTokenHash = tokensAfterFirst.get(0).getTokenHash();

        // Request 2: uppercase spelling inside cooldown
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest("CASE.RESEND@EXAMPLE.COM"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_RESEND)));

        // Request 3: mixed-case spelling inside cooldown
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest("CaSe.ReSeNd@ExAmPlE.cOm"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_RESEND)));

        List<EmailVerificationToken> tokensAfterAliases = emailVerificationTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterAliases.size(),
                "Case-variant resend requests inside cooldown must not mint additional tokens");
        org.junit.jupiter.api.Assertions.assertEquals(initialTokenHash, tokensAfterAliases.get(0).getTokenHash(),
                "Case-variant resend requests must not rotate the existing outstanding token (F2-ALIAS)");
    }

    @Test
    @DisplayName("F2-ALIAS: formatting and whitespace aliases share same account cooldown")
    void authLinks_whitespaceAndFormattingAliases_shareSameAccountCooldown() throws Exception {
        UserAccount user = createPendingCitizen("format.alias@example.com");

        // Request 1: with untrimmed whitespace
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("   format.alias@example.com   "))))
                .andExpect(status().isOk());

        List<PasswordResetToken> tokensAfterFirst = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterFirst.size());
        String initialTokenHash = tokensAfterFirst.get(0).getTokenHash();

        // Request 2: clean trimmed spelling inside cooldown
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("format.alias@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_FORGOT)));

        List<PasswordResetToken> tokensAfterSecond = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterSecond.size());
        org.junit.jupiter.api.Assertions.assertEquals(initialTokenHash, tokensAfterSecond.get(0).getTokenHash(),
                "Whitespace-variant request must hit the same account cooldown and not rotate token");
    }

    @Test
    @DisplayName("F2-ALIAS: cross-flow aliases share same account cooldown and cannot bypass cooldown by switching flows")
    void crossFlowAliases_shareSameAccountCooldown_cannotBypassBySwitchingFlows() throws Exception {
        UserAccount user = createPendingCitizen("crossflow.alias@example.com");

        // Step 1: Initial forgot-password request
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("crossflow.alias@example.com"))))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(1,
                passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId()).size());

        // Step 2: Immediate resend-verification request using uppercase alias for same account
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest("CROSSFLOW.ALIAS@EXAMPLE.COM"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(GENERIC_RESEND)));

        // No verification token must be minted because the account is in cooldown from the forgot-password flow
        List<EmailVerificationToken> verificationTokens =
                emailVerificationTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(0, verificationTokens.size(),
                "Switching flows with an alias spelling must not bypass the account cooldown (F2-ALIAS)");
    }

    @Test
    @DisplayName("F2-ALIAS fallback: authoritative account identity protects against arbitrary database alias lookups")
    void distinctAliases_resolvingToSameAccount_shareCooldownKey() {
        UserAccount user = createPendingCitizen("canonical.db.alias@example.com");

        // First issuance using canonical identifier
        authService.forgotPassword(new ForgotPasswordRequest("canonical.db.alias@example.com"));
        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokens.size());
        String initialTokenHash = tokens.get(0).getTokenHash();

        // Direct throttle check: account ID is strictly inside cooldown
        org.junit.jupiter.api.Assertions.assertFalse(authLinkIssuanceThrottle.tryRecordIssuance(user.getId(), Instant.now()),
                "Account ID must be in active cooldown");

        // Second request with alias spelling
        authService.forgotPassword(new ForgotPasswordRequest("CANONICAL.DB.ALIAS@EXAMPLE.COM"));
        List<PasswordResetToken> tokensAfterAlias = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokensAfterAlias.size(),
                "Alias request must not create an additional reset token");
        org.junit.jupiter.api.Assertions.assertEquals(initialTokenHash, tokensAfterAlias.get(0).getTokenHash(),
                "Alias request must not rotate the existing reset token");
    }
}
