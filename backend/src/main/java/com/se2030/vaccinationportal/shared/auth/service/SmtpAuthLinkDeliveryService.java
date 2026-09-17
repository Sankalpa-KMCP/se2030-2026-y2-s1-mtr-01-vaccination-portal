package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Production SMTP delivery implementation for verification and password reset links.
 * Active by default or when app.security.auth.delivery-mode=smtp.
 * Never logs raw tokens, token hashes, or links.
 */
@Service
@ConditionalOnProperty(
        name = "app.security.auth.delivery-mode",
        havingValue = "smtp",
        matchIfMissing = true
)
public class SmtpAuthLinkDeliveryService implements AuthLinkDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(SmtpAuthLinkDeliveryService.class);

    private final JavaMailSender mailSender;
    private final String frontendBaseUrl;
    private final String mailFrom;
    private final int verificationTokenExpirationHours;
    private final int resetTokenExpirationMinutes;

    public SmtpAuthLinkDeliveryService(
            JavaMailSender mailSender,
            @Value("${app.auth.frontend-base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${app.security.auth.mail-from:noreply@vaccinationportal.gov.lk}") String mailFrom,
            @Value("${app.security.auth.verification-token-expiration-hours:24}") int verificationTokenExpirationHours,
            @Value("${app.security.auth.reset-token-expiration-minutes:30}") int resetTokenExpirationMinutes
    ) {
        this.mailSender = Objects.requireNonNull(mailSender, "JavaMailSender must not be null");
        this.frontendBaseUrl = frontendBaseUrl == null ? "http://localhost:5173" : frontendBaseUrl.replaceAll("/+$", "");
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("SMTP sender email address (app.security.auth.mail-from) must be configured");
        }
        this.mailFrom = mailFrom.trim();
        this.verificationTokenExpirationHours = verificationTokenExpirationHours;
        this.resetTokenExpirationMinutes = resetTokenExpirationMinutes;
    }

    @Override
    public void sendEmailVerificationLink(String email, String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String verificationUrl = frontendBaseUrl + "/verify-email?token=" + encodedToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email.trim());
        message.setSubject("Vaccination Portal - Verify your email address");
        message.setText(
                "Vaccination Portal\n"
                        + "Verify your email address\n\n"
                        + "Please verify your email address to secure your citizen vaccination account by clicking the link below:\n"
                        + verificationUrl + "\n\n"
                        + "This verification link expires in " + verificationTokenExpirationHours + " hours.\n\n"
                        + "If you did not register for an account, please ignore this email."
        );

        try {
            mailSender.send(message);
            log.info("[AUTH DELIVERY] Dispatched verification email to [{}] via SMTP", email);
        } catch (MailException ex) {
            log.error("[AUTH DELIVERY] Failed to deliver verification email to [{}] via SMTP: {}", email, ex.getMessage());
            throw new AuthEmailDeliveryException("Failed to deliver verification email. Please try again later.", ex);
        }
    }

    @Override
    public void sendPasswordResetLink(String email, String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + encodedToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email.trim());
        message.setSubject("Vaccination Portal - Password Reset Request");
        message.setText(
                "Vaccination Portal\n"
                        + "Reset your password\n\n"
                        + "A password reset request was received for your account. You can set a new password by clicking the link below:\n"
                        + resetUrl + "\n\n"
                        + "This password reset link expires in " + resetTokenExpirationMinutes + " minutes.\n\n"
                        + "If you did not request a password reset, please ignore this email."
        );

        try {
            mailSender.send(message);
            log.info("[AUTH DELIVERY] Dispatched password reset email to [{}] via SMTP", email);
        } catch (MailException ex) {
            log.error("[AUTH DELIVERY] Failed to deliver password reset email to [{}] via SMTP: {}", email, ex.getMessage());
            throw new AuthEmailDeliveryException("Failed to deliver password reset email. Please try again later.", ex);
        }
    }
}
