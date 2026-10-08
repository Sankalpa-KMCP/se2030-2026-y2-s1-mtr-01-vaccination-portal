package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException;
import jakarta.mail.Message;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class SmtpEmailOtpDeliveryServiceTests {

    private static final String MAIL_FROM = "noreply@vaccinationportal.gov.lk";

    private JavaMailSender mailSender;
    private SmtpEmailOtpDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        deliveryService = new SmtpEmailOtpDeliveryService(mailSender, MAIL_FROM);
    }

    @Test
    @DisplayName("Sends the code to the registered address with expiry wording and a minimal body")
    void sendVerificationOtp_success() throws Exception {
        String recipient = "citizen@example.com";
        String otp = "004281";

        deliveryService.sendVerificationOtp(recipient, otp, Duration.ofMinutes(5));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage message = captor.getValue();
        assertNotNull(message);
        assertEquals(recipient, message.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals(MAIL_FROM, message.getFrom()[0].toString());
        assertEquals("Vaccination Portal - Verify your email", message.getSubject());

        String body = bodyOf(message);
        assertTrue(body.contains(otp), "The outgoing message must carry the verification code");
        assertTrue(body.contains("5 minutes"), "The outgoing message must state the expiry");
        assertTrue(body.contains("If you did not create this account, you can ignore this email."));
        assertFalse(body.toLowerCase().contains("password"), "No password material belongs in a verification email");
        assertFalse(body.contains("Bearer"), "No token material belongs in a verification email");
    }

    @Test
    @DisplayName("Delivery failure raises AuthEmailDeliveryException and never logs the code")
    void sendVerificationOtp_failure_neverLogsCode(CapturedOutput output) {
        String otp = "713902";
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        org.mockito.Mockito.doThrow(new MailSendException("Connection refused"))
                .when(mailSender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));

        assertThrows(AuthEmailDeliveryException.class,
                () -> deliveryService.sendVerificationOtp("citizen@example.com", otp, Duration.ofMinutes(5)));

        String logs = output.getAll();
        assertFalse(logs.contains(otp), "The plaintext code must NEVER appear in logs");
        assertTrue(logs.contains("MailSendException"), "Only the failure class may be reported");
    }

    @Test
    @DisplayName("Expiry wording follows the configured validity")
    void sendVerificationOtp_reportsConfiguredValidity() throws Exception {
        deliveryService.sendVerificationOtp("citizen@example.com", "000001", Duration.ofSeconds(300));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        assertTrue(bodyOf(captor.getValue()).contains("5 minutes"));
    }

    private static String bodyOf(MimeMessage message) throws Exception {
        Object content = message.getDataHandler().getContent();
        if (content instanceof String text) {
            return text;
        }
        return new String(message.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
