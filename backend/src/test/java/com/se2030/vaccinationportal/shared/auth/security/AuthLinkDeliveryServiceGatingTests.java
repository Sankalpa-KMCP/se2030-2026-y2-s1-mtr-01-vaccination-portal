package com.se2030.vaccinationportal.shared.auth.security;

import com.se2030.vaccinationportal.shared.auth.service.AuthLinkDeliveryService;
import com.se2030.vaccinationportal.shared.auth.service.ConsoleAuthLinkDeliveryService;
import com.se2030.vaccinationportal.shared.auth.service.ConsoleEmailOtpDeliveryService;
import com.se2030.vaccinationportal.shared.auth.service.EmailOtpDeliveryService;
import com.se2030.vaccinationportal.shared.auth.service.NoOpAuthLinkDeliveryService;
import com.se2030.vaccinationportal.shared.auth.service.SmtpAuthLinkDeliveryService;
import com.se2030.vaccinationportal.shared.auth.service.SmtpEmailOtpDeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class AuthLinkDeliveryServiceGatingTests {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "app.security.auth.delivery-mode=smtp")
    @DisplayName("SMTP delivery mode: SmtpAuthLinkDeliveryService active, others absent, no raw token logging")
    class SmtpDeliveryModeTests {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private AuthLinkDeliveryService authLinkDeliveryService;

        @MockitoBean
        private JavaMailSender mailSender;

        @Test
        @DisplayName("Resolves exactly one AuthLinkDeliveryService bean of type SmtpAuthLinkDeliveryService")
        void resolvesSmtpDeliveryInSmtpMode() {
            Map<String, AuthLinkDeliveryService> beans = applicationContext.getBeansOfType(AuthLinkDeliveryService.class);
            assertEquals(1, beans.size(), "Must resolve exactly one AuthLinkDeliveryService bean");

            assertInstanceOf(SmtpAuthLinkDeliveryService.class, authLinkDeliveryService,
                    "Bean must be SmtpAuthLinkDeliveryService");

            assertTrue(applicationContext.getBeansOfType(ConsoleAuthLinkDeliveryService.class).isEmpty(),
                    "ConsoleAuthLinkDeliveryService must be absent in SMTP mode");
            assertTrue(applicationContext.getBeansOfType(NoOpAuthLinkDeliveryService.class).isEmpty(),
                    "NoOpAuthLinkDeliveryService must be absent in SMTP mode");
        }

        @Test
        @DisplayName("SMTP delivery does not log raw verification or reset tokens")
        void smtpDelivery_doesNotLogRawTokens(CapturedOutput output) {
            String verificationSentinel = "SENTINEL_RAW_VERIFICATION_TOKEN_SMTP_123";
            String resetSentinel = "SENTINEL_RAW_RESET_TOKEN_SMTP_456";

            authLinkDeliveryService.sendEmailVerificationLink("citizen@example.com", verificationSentinel);
            authLinkDeliveryService.sendPasswordResetLink("citizen@example.com", resetSentinel);

            String logs = output.getAll();
            assertFalse(logs.contains(verificationSentinel),
                    "Raw verification sentinel token must NEVER appear in logs under SMTP configuration");
            assertFalse(logs.contains(resetSentinel),
                    "Raw reset sentinel token must NEVER appear in logs under SMTP configuration");
            assertFalse(logs.contains("/verify-email?token="),
                    "Complete verification URL must not be logged under SMTP configuration");
            assertFalse(logs.contains("/reset-password?token="),
                    "Complete reset URL must not be logged under SMTP configuration");
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles({"test", "local"})
    @TestPropertySource(properties = "app.security.auth.delivery-mode=console")
    @DisplayName("Console delivery mode: ConsoleAuthLinkDeliveryService active, others absent")
    class ConsoleDeliveryModeTests {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private AuthLinkDeliveryService authLinkDeliveryService;

        @Autowired
        private EmailOtpDeliveryService emailOtpDeliveryService;

        @Test
        @DisplayName("Resolves exactly one bean of type ConsoleAuthLinkDeliveryService")
        void resolvesConsoleDeliveryInConsoleMode() {
            Map<String, AuthLinkDeliveryService> beans = applicationContext.getBeansOfType(AuthLinkDeliveryService.class);
            assertEquals(1, beans.size(), "Must resolve exactly one AuthLinkDeliveryService bean");

            assertInstanceOf(ConsoleAuthLinkDeliveryService.class, authLinkDeliveryService,
                    "Bean must be ConsoleAuthLinkDeliveryService");

            assertTrue(applicationContext.getBeansOfType(SmtpAuthLinkDeliveryService.class).isEmpty(),
                    "SmtpAuthLinkDeliveryService must be absent when console delivery is enabled");
            assertTrue(applicationContext.getBeansOfType(NoOpAuthLinkDeliveryService.class).isEmpty(),
                    "NoOpAuthLinkDeliveryService must be absent when console delivery is enabled");
            assertEquals(1, applicationContext.getBeansOfType(EmailOtpDeliveryService.class).size());
            assertInstanceOf(ConsoleEmailOtpDeliveryService.class, emailOtpDeliveryService);
            assertTrue(applicationContext.getBeansOfType(SmtpEmailOtpDeliveryService.class).isEmpty());
        }

        @Test
        @DisplayName("Local console delivery displays the verification code and expiry without SMTP")
        void consoleOtpDelivery_logsCodeForLocalDemo(CapturedOutput output) {
            String syntheticCode = "0".repeat(6);

            emailOtpDeliveryService.sendVerificationOtp("dev.user@example.com", syntheticCode, Duration.ofMinutes(5));

            String logs = output.getAll();
            assertTrue(logs.contains("[AUTH OTP DELIVERY][LOCAL ONLY]"));
            assertTrue(logs.contains(syntheticCode));
            assertTrue(logs.contains("expires in 300 seconds"));
        }

        @Test
        @DisplayName("Console delivery logs formatted links when explicitly enabled for dev/eval")
        void consoleDelivery_logsFormattedLinksWhenEnabled(CapturedOutput output) {
            String syntheticToken = "SYNTHETIC_DEV_TOKEN_456";

            authLinkDeliveryService.sendEmailVerificationLink("dev.user@example.com", syntheticToken);
            authLinkDeliveryService.sendPasswordResetLink("dev.user@example.com", syntheticToken);

            String logs = output.getAll();
            assertTrue(logs.contains("[AUTH DELIVERY] Citizen Email Verification Link for [dev.user@example.com]"),
                    "Console delivery must log verification link banner when explicitly enabled");
            assertTrue(logs.contains("[AUTH DELIVERY] Password Reset Link for [dev.user@example.com]"),
                    "Console delivery must log reset link banner when explicitly enabled");
            assertTrue(logs.contains("/verify-email?token=" + syntheticToken));
            assertTrue(logs.contains("/reset-password?token=" + syntheticToken));
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "app.security.auth.delivery-mode=disabled")
    @DisplayName("Disabled delivery mode: NoOp delivery active, others absent")
    class DisabledDeliveryModeTests {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private AuthLinkDeliveryService authLinkDeliveryService;

        @Test
        @DisplayName("Resolves NoOpAuthLinkDeliveryService and excludes Smtp and Console services")
        void resolvesNoOpDeliveryWhenDisabled() {
            Map<String, AuthLinkDeliveryService> beans = applicationContext.getBeansOfType(AuthLinkDeliveryService.class);
            assertEquals(1, beans.size());

            assertInstanceOf(NoOpAuthLinkDeliveryService.class, authLinkDeliveryService);
            assertTrue(applicationContext.getBeansOfType(SmtpAuthLinkDeliveryService.class).isEmpty());
            assertTrue(applicationContext.getBeansOfType(ConsoleAuthLinkDeliveryService.class).isEmpty());
        }

        @Test
        @DisplayName("Disabled delivery does not log raw verification or reset tokens")
        void disabledDelivery_doesNotLogRawTokens(CapturedOutput output) {
            String verificationSentinel = "SENTINEL_RAW_VERIFICATION_TOKEN_DISABLED_123";
            String resetSentinel = "SENTINEL_RAW_RESET_TOKEN_DISABLED_456";

            authLinkDeliveryService.sendEmailVerificationLink("citizen@example.com", verificationSentinel);
            authLinkDeliveryService.sendPasswordResetLink("citizen@example.com", resetSentinel);

            String logs = output.getAll();
            assertFalse(logs.contains(verificationSentinel),
                    "Raw verification sentinel token must NEVER appear in logs under disabled configuration");
            assertFalse(logs.contains(resetSentinel),
                    "Raw reset sentinel token must NEVER appear in logs under disabled configuration");
            assertFalse(logs.contains("/verify-email?token="),
                    "Complete verification URL must not be logged under disabled configuration");
            assertFalse(logs.contains("/reset-password?token="),
                    "Complete reset URL must not be logged under disabled configuration");
        }
    }
}
