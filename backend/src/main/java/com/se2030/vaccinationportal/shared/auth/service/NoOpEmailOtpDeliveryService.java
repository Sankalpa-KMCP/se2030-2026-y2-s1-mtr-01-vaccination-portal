package com.se2030.vaccinationportal.shared.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Disabled delivery of the email verification OTP code.
 * Does not log the plaintext code. Active strictly when
 * app.security.auth.delivery-mode=disabled (test execution).
 */
@Service
@ConditionalOnProperty(
        name = "app.security.auth.delivery-mode",
        havingValue = "disabled"
)
public class NoOpEmailOtpDeliveryService implements EmailOtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailOtpDeliveryService.class);

    @Override
    public void sendVerificationOtp(String email, String otp, Duration validity) {
        log.debug("[AUTH OTP DELIVERY] Delivery mode is disabled. Verification code email withheld.");
    }
}
