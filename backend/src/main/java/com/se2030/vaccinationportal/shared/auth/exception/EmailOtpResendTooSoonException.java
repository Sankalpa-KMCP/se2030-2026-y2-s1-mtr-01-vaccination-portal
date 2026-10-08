package com.se2030.vaccinationportal.shared.auth.exception;

public class EmailOtpResendTooSoonException extends RuntimeException {

    private final long retryAfterSeconds;

    public EmailOtpResendTooSoonException(long retryAfterSeconds) {
        super("Please wait before requesting another verification code");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
