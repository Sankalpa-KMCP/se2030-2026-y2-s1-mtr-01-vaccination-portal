package com.se2030.vaccinationportal.shared.auth.exception;

public class EmailOtpAttemptsExceededException extends RuntimeException {
    public EmailOtpAttemptsExceededException() {
        super("Too many incorrect verification attempts. Request a new code");
    }
}
