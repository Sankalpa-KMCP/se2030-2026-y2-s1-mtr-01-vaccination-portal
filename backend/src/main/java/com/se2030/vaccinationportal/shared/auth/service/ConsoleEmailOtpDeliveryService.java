package com.se2030.vaccinationportal.shared.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Local-demo delivery of the email verification OTP code. The plaintext code
 * is logged only when both the local profile and console delivery mode are
 * explicitly enabled; production must use SMTP delivery instead.
 */
@Service
@Profile("local")
@ConditionalOnProperty(
        name = "app.security.auth.delivery-mode",
        havingValue = "console"
)
public class ConsoleEmailOtpDeliveryService implements EmailOtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailOtpDeliveryService.class);

    @Override
    public void sendVerificationOtp(String email, String otp, Duration validity) {
        log.info("[AUTH OTP DELIVERY][LOCAL ONLY] Verification code for [{}]: {} (expires in {} seconds)",
                email, otp, validity.toSeconds());
    }
}
