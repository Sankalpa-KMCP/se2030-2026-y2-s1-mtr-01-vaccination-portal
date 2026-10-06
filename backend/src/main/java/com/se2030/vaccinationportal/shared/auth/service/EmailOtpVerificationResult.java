package com.se2030.vaccinationportal.shared.auth.service;

/**
 * Result of an OTP verification attempt. Returned instead of thrown so the
 * transaction commits the durable attempt increment for failed submissions.
 */
public enum EmailOtpVerificationResult {
    VERIFIED,
    ALREADY_VERIFIED,
    INVALID,
    EXPIRED,
    ATTEMPTS_EXCEEDED
}
