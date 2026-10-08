package com.se2030.vaccinationportal.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.shared.auth.dto.CitizenRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.CitizenProfileRepository;
import com.se2030.vaccinationportal.shared.auth.repository.EmailOtpChallengeRepository;
import com.se2030.vaccinationportal.shared.auth.repository.EmailVerificationTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.PasswordResetTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.service.EmailOtpDeliveryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration coverage for the principal-scoped email OTP verification API:
 * registration issues the challenge, an unverified citizen can still log in, and the
 * authenticated citizen verifies, resends, and reads status over HTTP. Delivery is a
 * test double; the plaintext code is captured from the delivery call to drive submissions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailOtpVerificationFlowIntegrationTests {

    private static final Instant BASE_TIME = Instant.parse("2026-03-01T08:00:00Z");
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
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                new Role("ROLE_CITIZEN", "Citizen")
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

    private String registerCitizen(String email) throws Exception {
        CitizenRegistrationRequest request = new CitizenRegistrationRequest(
                "Test Citizen", email, "0770000000", RAW_PASSWORD);
        mockMvc.perform(post("/api/v1/auth/register/citizen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountStatus", is("PENDING_VERIFICATION")));
        return latestDeliveredCode();
    }

    private String latestDeliveredCode() {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(deliveryService, atLeastOnce()).sendVerificationOtp(anyString(), codeCaptor.capture(), any(Duration.class));
        return codeCaptor.getValue();
    }

    private int deliveredCodeCount() {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(deliveryService, atLeastOnce()).sendVerificationOtp(anyString(), codeCaptor.capture(), any(Duration.class));
        return codeCaptor.getAllValues().size();
    }

    private String loginAndGetToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, RAW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String auth(String token) {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("Registration issues an OTP challenge, unverified citizen can log in, and the OTP verifies the account")
    void registration_login_verify_endToEnd() throws Exception {
        String email = "flow.verify@example.com";

        String otp = registerCitizen(email);

        UserAccount account = userAccountRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(AccountStatus.PENDING_VERIFICATION, account.getAccountStatus());
        assertNull(account.getEmailVerifiedAt());
        assertTrue(challengeRepository.findByUser_Id(account.getId()).isPresent(),
                "Registration must create the OTP challenge atomically with the account");

        String token = loginAndGetToken(email);

        mockMvc.perform(get("/api/v1/auth/email-verification/status").header("Authorization", auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified", is(false)))
                .andExpect(jsonPath("$.emailVerifiedAt", nullValue()));

        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otp", otp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified", is(true)))
                .andExpect(jsonPath("$.emailVerifiedAt", notNullValue()));

        mockMvc.perform(get("/api/v1/auth/email-verification/status").header("Authorization", auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified", is(true)));

        UserAccount verified = userAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, verified.getAccountStatus());
        assertNotNull(verified.getEmailVerifiedAt());
    }

    @Test
    @DisplayName("Resend enforces the server-side cooldown, then rotates the code via the API")
    void resend_cooldown_thenRotation_overHttp() throws Exception {
        String email = "flow.resend@example.com";
        String firstCode = registerCitizen(email);
        String token = loginAndGetToken(email);

        STEPPING_CLOCK.advance(Duration.ofSeconds(10));
        mockMvc.perform(post("/api/v1/auth/email-verification/resend").header("Authorization", auth(token)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code", is("EMAIL_VERIFICATION_RESEND_TOO_SOON")))
                .andExpect(jsonPath("$.retryAfterSeconds", is(50)));

        STEPPING_CLOCK.advance(Duration.ofSeconds(50));
        mockMvc.perform(post("/api/v1/auth/email-verification/resend").header("Authorization", auth(token)))
                .andExpect(status().isOk());

        String secondCode = latestDeliveredCode();
        assertNotEquals(firstCode, secondCode);

        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otp", firstCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("EMAIL_VERIFICATION_INVALID_CODE")));

        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otp", secondCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified", is(true)));
    }

    @Test
    @DisplayName("Five wrong submissions over HTTP lock the challenge; the correct code is then rejected")
    void wrongAttempts_lockChallenge_overHttp() throws Exception {
        String email = "flow.attempts@example.com";
        String otp = registerCitizen(email);
        String token = loginAndGetToken(email);
        List<String> wrongCodes = List.of("000000", "111111", "222222", "333333", "444444", "555555").stream()
                .filter(code -> !code.equals(otp))
                .limit(5)
                .toList();

        for (int attempt = 0; attempt < 4; attempt++) {
            mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                            .header("Authorization", auth(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("otp", wrongCodes.get(attempt)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("EMAIL_VERIFICATION_INVALID_CODE")));
        }
        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otp", wrongCodes.get(4)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED")));

        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otp", otp))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED")));

        mockMvc.perform(get("/api/v1/auth/email-verification/status").header("Authorization", auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified", is(false)));
    }

    @Test
    @DisplayName("Verify is idempotent for verified accounts and resend is rejected with EMAIL_ALREADY_VERIFIED")
    void alreadyVerified_idempotentVerify_conflictedResend() throws Exception {
        String email = "flow.verified@example.com";
        String otp = registerCitizen(email);
        String token = loginAndGetToken(email);

        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otp", otp))))
                .andExpect(status().isOk());

        int codesAfterVerification = deliveredCodeCount();

        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otp", otp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified", is(true)));

        mockMvc.perform(post("/api/v1/auth/email-verification/resend").header("Authorization", auth(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("EMAIL_ALREADY_VERIFIED")));

        assertEquals(codesAfterVerification, deliveredCodeCount(),
                "A verified account must never receive additional codes");
    }    @Test
    @DisplayName("Verification is principal-scoped: no request field can select another citizen's account")
    void verification_isPrincipalScoped() throws Exception {
        String emailA = "flow.owner@example.com";
        String emailB = "flow.victim@example.com";
        String codeA = registerCitizen(emailA);
        String codeB = registerCitizen(emailB);
        String tokenA = loginAndGetToken(emailA);
        String tokenB = loginAndGetToken(emailB);

        UserAccount accountB = userAccountRepository.findByEmailIgnoreCase(emailB).orElseThrow();

        // Account A verifies with A's code; browser-controlled selector fields are ignored.
        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "otp", codeA,
                                "userId", accountB.getId(),
                                "email", emailB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified", is(true)));

        UserAccount accountA = userAccountRepository.findByEmailIgnoreCase(emailA).orElseThrow();
        UserAccount accountBAfter = userAccountRepository.findById(accountB.getId()).orElseThrow();
        assertNotNull(accountA.getEmailVerifiedAt());
        assertNull(accountBAfter.getEmailVerifiedAt(),
                "Another principal's submission must never verify account B");
        assertEquals(AccountStatus.PENDING_VERIFICATION, accountBAfter.getAccountStatus());

        // B cannot use A's code: the challenge is B's own.
        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otp", codeA))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("EMAIL_VERIFICATION_INVALID_CODE")));
        assertNull(userAccountRepository.findById(accountB.getId()).orElseThrow().getEmailVerifiedAt());
    }

    @Test
    @DisplayName("Verification endpoints require authentication and the citizen role")
    void endpoints_requireAuthenticationAndCitizenRole() throws Exception {
        mockMvc.perform(get("/api/v1/auth/email-verification/status"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otp", "123456"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/email-verification/resend"))
                .andExpect(status().isUnauthorized());

        // A non-citizen principal reaches no verification endpoint.
        UserAccount admin = new UserAccount("admin.security@example.com",
                passwordEncoder.encode(RAW_PASSWORD), AccountStatus.ACTIVE);
        admin.setFullName("Portal Administrator");
        admin.addRole(roleRepository.findByCode("ROLE_ADMIN").orElseThrow());
        admin.setEmailVerifiedAt(BASE_TIME);
        userAccountRepository.saveAndFlush(admin);
        String adminToken = loginAndGetToken("admin.security@example.com");

        mockMvc.perform(get("/api/v1/auth/email-verification/status").header("Authorization", auth(adminToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otp", "123456"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/email-verification/resend").header("Authorization", auth(adminToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Malformed and blank codes are rejected without touching the challenge")
    void malformedCodes_rejected() throws Exception {
        String email = "flow.malformed@example.com";
        registerCitizen(email);
        String token = loginAndGetToken(email);

        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otp", "12ab56"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("EMAIL_VERIFICATION_INVALID_CODE")));

        mockMvc.perform(post("/api/v1/auth/email-verification/verify")
                        .header("Authorization", auth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        assertEquals(0, challengeRepository.findByUser_Id(
                        userAccountRepository.findByEmailIgnoreCase(email).orElseThrow().getId()).orElseThrow()
                .getAttemptCount(),
                "Malformed submissions must not consume the attempt budget");
    }
}
