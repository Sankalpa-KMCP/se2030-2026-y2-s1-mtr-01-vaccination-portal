package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class SmtpAuthLinkDeliveryServiceTests {

    private JavaMailSender mailSender;
    private SmtpAuthLinkDeliveryService deliveryService;

    private static final String FRONTEND_BASE_URL = "http://localhost:5173";
    private static final String MAIL_FROM = "noreply@vaccinationportal.gov.lk";
    private static final int VERIFICATION_EXPIRY_HOURS = 24;
    private static final int RESET_EXPIRY_MINUTES = 30;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        deliveryService = new SmtpAuthLinkDeliveryService(
                mailSender,
                FRONTEND_BASE_URL,
                MAIL_FROM,
                VERIFICATION_EXPIRY_HOURS,
                RESET_EXPIRY_MINUTES
        );
    }

    @Test
    @DisplayName("Sends verification email with correct recipient, subject, link, sender, and expiration")
    void sendEmailVerificationLink_success(CapturedOutput output) {
        String recipient = "citizen@example.com";
        String rawToken = "SYNTHETIC_VERIFY_TOKEN_XYZ123";

        deliveryService.sendEmailVerificationLink(recipient, rawToken);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertNotNull(message);
        assertArrayEquals(new String[]{recipient}, message.getTo());
        assertEquals(MAIL_FROM, message.getFrom());
        assertEquals("Vaccination Portal - Verify your email address", message.getSubject());

        String body = Objects.requireNonNull(message.getText());
        assertTrue(body.contains("http://localhost:5173/verify-email?token=SYNTHETIC_VERIFY_TOKEN_XYZ123"));
        assertTrue(body.contains("24 hours"));
        assertFalse(body.contains("hash"));

        String logs = output.getAll();
        assertFalse(logs.contains(rawToken), "Raw token must NEVER appear in logs in SMTP mode");
        assertFalse(logs.contains("http://localhost:5173/verify-email"), "Complete verification link must not appear in logs in SMTP mode");
        assertTrue(logs.contains("[AUTH DELIVERY] Dispatched verification email to [citizen@example.com] via SMTP"));
    }

    @Test
    @DisplayName("Sends password reset email with correct recipient, subject, link, sender, and expiration")
    void sendPasswordResetLink_success(CapturedOutput output) {
        String recipient = "user.recovery@example.com";
        String rawToken = "SYNTHETIC_RESET_TOKEN_ABC789";

        deliveryService.sendPasswordResetLink(recipient, rawToken);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertNotNull(message);
        assertArrayEquals(new String[]{recipient}, message.getTo());
        assertEquals(MAIL_FROM, message.getFrom());
        assertEquals("Vaccination Portal - Password Reset Request", message.getSubject());

        String body = Objects.requireNonNull(message.getText());
        assertTrue(body.contains("http://localhost:5173/reset-password?token=SYNTHETIC_RESET_TOKEN_ABC789"));
        assertTrue(body.contains("30 minutes"));
        assertFalse(body.contains("hash"));

        String logs = output.getAll();
        assertFalse(logs.contains(rawToken), "Raw token must NEVER appear in logs in SMTP mode");
        assertFalse(logs.contains("http://localhost:5173/reset-password"), "Complete reset link must not appear in logs in SMTP mode");
        assertTrue(logs.contains("[AUTH DELIVERY] Dispatched password reset email to [user.recovery@example.com] via SMTP"));
    }

    @Test
    @DisplayName("Throws AuthEmailDeliveryException when mailSender.send throws MailException")
    void sendEmailVerificationLink_throwsOnMailException(CapturedOutput output) {
        String recipient = "fail.citizen@example.com";
        String rawToken = "SYNTHETIC_FAIL_TOKEN";

        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        AuthEmailDeliveryException exception = assertThrows(
                AuthEmailDeliveryException.class,
                () -> deliveryService.sendEmailVerificationLink(recipient, rawToken)
        );

        assertTrue(exception.getMessage().contains("Failed to deliver verification email"));

        String logs = output.getAll();
        assertFalse(logs.contains(rawToken), "Raw token must not appear in error logs");
        assertTrue(logs.contains("[AUTH DELIVERY] Failed to deliver verification email to [fail.citizen@example.com] via SMTP"));
    }

    @Test
    @DisplayName("Throws AuthEmailDeliveryException when password reset send throws MailException")
    void sendPasswordResetLink_throwsOnMailException(CapturedOutput output) {
        String recipient = "fail.reset@example.com";
        String rawToken = "SYNTHETIC_FAIL_RESET_TOKEN";

        doThrow(new MailSendException("Mail server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        AuthEmailDeliveryException exception = assertThrows(
                AuthEmailDeliveryException.class,
                () -> deliveryService.sendPasswordResetLink(recipient, rawToken)
        );

        assertTrue(exception.getMessage().contains("Failed to deliver password reset email"));

        String logs = output.getAll();
        assertFalse(logs.contains(rawToken), "Raw token must not appear in error logs");
        assertTrue(logs.contains("[AUTH DELIVERY] Failed to deliver password reset email to [fail.reset@example.com] via SMTP"));
    }

    @Test
    @DisplayName("Constructor enforces non-null mailSender and non-blank mailFrom")
    void constructorValidation() {
        assertThrows(NullPointerException.class, () -> new SmtpAuthLinkDeliveryService(
                null,
                FRONTEND_BASE_URL,
                MAIL_FROM,
                24,
                30
        ));

        assertThrows(IllegalStateException.class, () -> new SmtpAuthLinkDeliveryService(
                mailSender,
                FRONTEND_BASE_URL,
                "",
                24,
                30
        ));

        assertThrows(IllegalStateException.class, () -> new SmtpAuthLinkDeliveryService(
                mailSender,
                FRONTEND_BASE_URL,
                "   ",
                24,
                30
        ));
    }
}
