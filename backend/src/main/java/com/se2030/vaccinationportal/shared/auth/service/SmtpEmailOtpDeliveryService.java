package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * Production SMTP delivery of the email verification OTP code.
 * Active by default or when app.security.auth.delivery-mode=smtp.
 * Sends a deliberately simple message containing only the code, its expiry,
 * and an ignore note: no credentials, tokens, health data, or identity data.
 * The plaintext code is never logged; failures report only the recipient and error class.
 */
@Service
@ConditionalOnProperty(
        name = "app.security.auth.delivery-mode",
        havingValue = "smtp",
        matchIfMissing = true
)
public class SmtpEmailOtpDeliveryService implements EmailOtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailOtpDeliveryService.class);

    private final JavaMailSender mailSender;
    private final String mailFrom;

    public SmtpEmailOtpDeliveryService(
            JavaMailSender mailSender,
            @Value("${app.security.auth.mail-from:noreply@vaccinationportal.gov.lk}") String mailFrom
    ) {
        this.mailSender = Objects.requireNonNull(mailSender, "JavaMailSender must not be null");
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("SMTP sender email address (app.security.auth.mail-from) must be configured");
        }
        this.mailFrom = mailFrom.trim();
    }

    @Override
    public void sendVerificationOtp(String email, String otp, Duration validity) {
        String subject = "Vaccination Portal - Verify your email";
        String body = "Your verification code is:\n\n"
                + otp + "\n\n"
                + "This code expires in " + toMinutes(validity) + " minutes.\n\n"
                + "If you did not create this account, you can ignore this email.";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(mailFrom);
            helper.setTo(email.trim());
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(mimeMessage);
            log.info("[AUTH OTP DELIVERY] Dispatched verification code email to [{}] via SMTP", email);
        } catch (MessagingException | MailException ex) {
            log.error("[AUTH OTP DELIVERY] Failed to deliver verification code email to [{}] via SMTP: {}",
                    email, ex.getClass().getSimpleName());
            throw new AuthEmailDeliveryException("Failed to deliver verification email. Please try again later.", ex);
        }
    }

    private static long toMinutes(Duration validity) {
        return Math.max(1, validity.toMinutes());
    }
}
