package com.se2030.vaccinationportal.shared.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Non-SMTP delivery implementation logging verification and recovery links
 * to the console for development and evaluation.
 * Only enabled when app.security.auth.delivery-mode=console.
 */
@Service
@ConditionalOnProperty(
        name = "app.security.auth.delivery-mode",
        havingValue = "console"
)
public class ConsoleAuthLinkDeliveryService implements AuthLinkDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleAuthLinkDeliveryService.class);

    private final String frontendBaseUrl;

    public ConsoleAuthLinkDeliveryService(
            @Value("${app.auth.frontend-base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void sendEmailVerificationLink(String email, String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String verificationUrl = frontendBaseUrl + "/verify-email?token=" + encodedToken;
        log.info("[AUTH DELIVERY] Citizen Email Verification Link for [{}]: {}", email, verificationUrl);
    }

    @Override
    public void sendPasswordResetLink(String email, String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + encodedToken;
        log.info("[AUTH DELIVERY] Password Reset Link for [{}]: {}", email, resetUrl);
    }
}
