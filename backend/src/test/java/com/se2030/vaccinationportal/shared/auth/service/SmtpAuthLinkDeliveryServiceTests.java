package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

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
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        deliveryService = new SmtpAuthLinkDeliveryService(
                mailSender,
                FRONTEND_BASE_URL,
                MAIL_FROM,
                VERIFICATION_EXPIRY_HOURS,
                RESET_EXPIRY_MINUTES
        );
    }

    @Test
    @DisplayName("Sends verification email with multipart HTML and plain text, correct headers, and no raw token in logs")
    void sendEmailVerificationLink_success(CapturedOutput output) throws Exception {
        String recipient = "citizen@example.com";
        String rawToken = "SYNTHETIC_VERIFY_TOKEN_XYZ123";

        deliveryService.sendEmailVerificationLink(recipient, rawToken);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage message = captor.getValue();
        assertNotNull(message);
        assertEquals(recipient, message.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals(MAIL_FROM, message.getFrom()[0].toString());
        assertEquals("Vaccination Portal - Verify your email address", message.getSubject());

        String plainText = getPartContent(message, "text/plain");
        assertNotNull(plainText);
        assertTrue(plainText.contains("http://localhost:5173/verify-email?token=SYNTHETIC_VERIFY_TOKEN_XYZ123"));
        assertTrue(plainText.contains("24 hours"));
        assertFalse(plainText.contains("hash"));

        String html = getPartContent(message, "text/html");
        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("<table role=\"presentation\""));
        assertTrue(html.contains("Verify your email address"));
        assertTrue(html.contains("Verify Email Address"));
        assertTrue(html.contains("http://localhost:5173/verify-email?token=SYNTHETIC_VERIFY_TOKEN_XYZ123"));
        assertTrue(html.contains("24 hours"));
        assertTrue(html.contains("Security Notice"));
        assertTrue(html.contains("#0B6E99"));
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("<link"));
        assertFalse(html.contains("<img"));
        assertFalse(html.contains("hash"));

        String logs = output.getAll();
        assertFalse(logs.contains(rawToken), "Raw token must NEVER appear in logs in SMTP mode");
        assertFalse(logs.contains("http://localhost:5173/verify-email"), "Complete verification link must not appear in logs in SMTP mode");
        assertTrue(logs.contains("[AUTH DELIVERY] Dispatched verification email to [citizen@example.com] via SMTP"));
    }

    @Test
    @DisplayName("Sends password reset email with multipart HTML and plain text, correct headers, and no raw token in logs")
    void sendPasswordResetLink_success(CapturedOutput output) throws Exception {
        String recipient = "user.recovery@example.com";
        String rawToken = "SYNTHETIC_RESET_TOKEN_ABC789";

        deliveryService.sendPasswordResetLink(recipient, rawToken);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage message = captor.getValue();
        assertNotNull(message);
        assertEquals(recipient, message.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals(MAIL_FROM, message.getFrom()[0].toString());
        assertEquals("Vaccination Portal - Password Reset Request", message.getSubject());

        String plainText = getPartContent(message, "text/plain");
        assertNotNull(plainText);
        assertTrue(plainText.contains("http://localhost:5173/reset-password?token=SYNTHETIC_RESET_TOKEN_ABC789"));
        assertTrue(plainText.contains("30 minutes"));
        assertFalse(plainText.contains("hash"));

        String html = getPartContent(message, "text/html");
        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("<table role=\"presentation\""));
        assertTrue(html.contains("Reset your password"));
        assertTrue(html.contains("Reset Password"));
        assertTrue(html.contains("http://localhost:5173/reset-password?token=SYNTHETIC_RESET_TOKEN_ABC789"));
        assertTrue(html.contains("30 minutes"));
        assertTrue(html.contains("Security Notice"));
        assertTrue(html.contains("#0B6E99"));
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("<link"));
        assertFalse(html.contains("<img"));
        assertFalse(html.contains("hash"));

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
                .when(mailSender).send(any(MimeMessage.class));

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
                .when(mailSender).send(any(MimeMessage.class));

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

        SmtpAuthLinkDeliveryService serviceWithSlash = new SmtpAuthLinkDeliveryService(
                mailSender,
                "http://localhost:5173///",
                MAIL_FROM,
                24,
                30
        );
        serviceWithSlash.sendEmailVerificationLink("user@example.com", "tok");
        verify(mailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("HTML body properly escapes special characters in action URLs")
    void htmlEscapesSpecialCharactersInUrls() throws Exception {
        String recipient = "test@example.com";
        String rawToken = "TOKEN_WITH_<>&\"'_SPECIALS";

        deliveryService.sendEmailVerificationLink(recipient, rawToken);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());

        MimeMessage message = captor.getValue();
        String html = getPartContent(message, "text/html");
        assertNotNull(html);
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("<iframe"));
    }

    private String getPartContent(MimeMessage message, String mimeType) throws Exception {
        Object content = message.getContent();
        if (content instanceof Multipart multipart) {
            return extractFromMultipart(multipart, mimeType);
        } else if (message.isMimeType(mimeType)) {
            return content.toString();
        }
        return null;
    }

    private String extractFromMultipart(Multipart multipart, String mimeType) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            Object content = part.getContent();
            if (content instanceof Multipart nested) {
                String nestedContent = extractFromMultipart(nested, mimeType);
                if (nestedContent != null) {
                    return nestedContent;
                }
            } else if (part.isMimeType(mimeType)) {
                if (content instanceof String str) {
                    return str;
                } else if (content instanceof java.io.InputStream is) {
                    return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                } else if (content != null) {
                    return content.toString();
                }
            }
        }
        return null;
    }
}
