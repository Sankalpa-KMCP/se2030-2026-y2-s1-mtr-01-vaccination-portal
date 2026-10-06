package com.se2030.vaccinationportal.shared.auth.service;

import java.time.Duration;

/**
 * Transaction-bound request to email the current verification code for one account.
 * Published inside the issuing transaction and dispatched only after that
 * transaction commits, so no SMTP network call can leave account or challenge
 * persistence half-complete. Carries the plaintext code in memory only.
 *
 * @param userId      the account the challenge belongs to (safe to log)
 * @param targetEmail the registered email address the code is bound to
 * @param otp         the plaintext six-digit code, never logged or persisted
 * @param validity    how long the code stays verifiable
 */
record EmailOtpDispatchEvent(Long userId, String targetEmail, String otp, Duration validity) {

    EmailOtpDispatchEvent {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalArgumentException("targetEmail must not be blank");
        }
        if (otp == null || otp.isBlank()) {
            throw new IllegalArgumentException("otp must not be blank");
        }
        if (validity == null || validity.isNegative() || validity.isZero()) {
            throw new IllegalArgumentException("validity must be positive");
        }
    }
}
