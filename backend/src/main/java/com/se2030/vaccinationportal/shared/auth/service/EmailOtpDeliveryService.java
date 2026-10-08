package com.se2030.vaccinationportal.shared.auth.service;

import java.time.Duration;

/**
 * Auth-owned abstraction for delivering citizen email verification OTP codes.
 * Implementations are selected by {@code app.security.auth.delivery-mode}:
 * smtp (default production delivery), console (local-profile demo delivery),
 * and disabled (test execution). Only local console delivery may log the code.
 */
public interface EmailOtpDeliveryService {

    /**
     * Delivers the verification code for the given target email address.
     *
     * @param email    the citizen's registered email address
     * @param otp      the plaintext six-digit verification code for delivery
     * @param validity how long the code remains verifiable
     * @throws com.se2030.vaccinationportal.shared.auth.exception.AuthEmailDeliveryException
     *         when the delivery transport fails
     */
    void sendVerificationOtp(String email, String otp, Duration validity);
}
