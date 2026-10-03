package com.se2030.vaccinationportal.shared.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Non-SMTP delivery of the email verification OTP code for development and
 * evaluation environments. Only enabled when app.security.auth.delivery-mode=console.
 * The plaintext code is never written to the console or any log; connect a real
 * SMTP mailbox (smtp mode) when the code itself must be observed.
 */
@Service
@ConditionalOnProperty(
        name = "app.security.auth.delivery-mode",
        havingValue = "console"
)
public class ConsoleEmailOtpDeliveryService implements EmailOtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailOtpDeliveryService.class);

    @Override
    public void sendVerificationOtp(String email, String otp, Duration validity) {
        log.info("[AUTH OTP DELIVERY] Console delivery mode: verification code email simulated for [{}]. "
                + "Codes are never logged; use SMTP delivery to receive the message.", email);
    }
}
