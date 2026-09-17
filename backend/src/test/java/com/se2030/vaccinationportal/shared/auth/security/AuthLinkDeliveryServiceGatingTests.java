package com.se2030.vaccinationportal.shared.auth.security;

import com.se2030.vaccinationportal.shared.auth.service.AuthLinkDeliveryService;
import com.se2030.vaccinationportal.shared.auth.service.ConsoleAuthLinkDeliveryService;
import com.se2030.vaccinationportal.shared.auth.service.NoOpAuthLinkDeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class AuthLinkDeliveryServiceGatingTests {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @DisplayName("Default / Missing property: NoOp delivery active, console delivery absent, no token logging")
    class DefaultConfigurationTests {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private AuthLinkDeliveryService authLinkDeliveryService;

        @Test
        @DisplayName("Resolves exactly one AuthLinkDeliveryService bean of type NoOpAuthLinkDeliveryService")
        void resolvesNoOpDeliveryByDefault() {
            Map<String, AuthLinkDeliveryService> beans = applicationContext.getBeansOfType(AuthLinkDeliveryService.class);
            assertEquals(1, beans.size(), "Must resolve exactly one AuthLinkDeliveryService bean");

            assertInstanceOf(NoOpAuthLinkDeliveryService.class, authLinkDeliveryService,
                    "Default bean must be NoOpAuthLinkDeliveryService");

            assertTrue(applicationContext.getBeansOfType(ConsoleAuthLinkDeliveryService.class).isEmpty(),
                    "ConsoleAuthLinkDeliveryService must be absent in default configuration");
        }

        @Test
        @DisplayName("Default delivery does not log raw verification or reset tokens")
        void defaultDelivery_doesNotLogRawTokens(CapturedOutput output) {
            String verificationSentinel = "SENTINEL_RAW_VERIFICATION_TOKEN_ABC123";
            String resetSentinel = "SENTINEL_RAW_RESET_TOKEN_XYZ789";

            authLinkDeliveryService.sendEmailVerificationLink("citizen@example.com", verificationSentinel);
            authLinkDeliveryService.sendPasswordResetLink("citizen@example.com", resetSentinel);

            String logs = output.getAll();
            assertFalse(logs.contains(verificationSentinel),
                    "Raw verification sentinel token must NEVER appear in logs under default configuration");
            assertFalse(logs.contains(resetSentinel),
                    "Raw reset sentinel token must NEVER appear in logs under default configuration");
            assertFalse(logs.contains("http://localhost:5173/verify-email"),
                    "Complete verification URL must not be logged under default configuration");
            assertFalse(logs.contains("http://localhost:5173/reset-password"),
                    "Complete reset URL must not be logged under default configuration");
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "app.security.auth.console-delivery-enabled=false")
    @DisplayName("Explicit false: NoOp delivery active, console delivery absent")
    class ExplicitFalseConfigurationTests {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private AuthLinkDeliveryService authLinkDeliveryService;

        @Test
        @DisplayName("Resolves NoOpAuthLinkDeliveryService and excludes ConsoleAuthLinkDeliveryService")
        void resolvesNoOpDeliveryWhenExplicitlyFalse() {
            Map<String, AuthLinkDeliveryService> beans = applicationContext.getBeansOfType(AuthLinkDeliveryService.class);
            assertEquals(1, beans.size());

            assertInstanceOf(NoOpAuthLinkDeliveryService.class, authLinkDeliveryService);
            assertTrue(applicationContext.getBeansOfType(ConsoleAuthLinkDeliveryService.class).isEmpty());
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "app.security.auth.console-delivery-enabled=true")
    @DisplayName("Explicit opt-in true: Console delivery active, NoOp delivery absent")
    class ExplicitTrueConfigurationTests {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private AuthLinkDeliveryService authLinkDeliveryService;

        @Test
        @DisplayName("Resolves exactly one bean of type ConsoleAuthLinkDeliveryService")
        void resolvesConsoleDeliveryWhenExplicitlyEnabled() {
            Map<String, AuthLinkDeliveryService> beans = applicationContext.getBeansOfType(AuthLinkDeliveryService.class);
            assertEquals(1, beans.size(), "Must resolve exactly one AuthLinkDeliveryService bean");

            assertInstanceOf(ConsoleAuthLinkDeliveryService.class, authLinkDeliveryService,
                    "Explicit true must activate ConsoleAuthLinkDeliveryService");

            assertTrue(applicationContext.getBeansOfType(NoOpAuthLinkDeliveryService.class).isEmpty(),
                    "NoOpAuthLinkDeliveryService must be absent when console delivery is enabled");
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
}
