package com.se2030.vaccinationportal.shared.auth.exception;

public class EmailOtpExpiredException extends RuntimeException {
    public EmailOtpExpiredException() {
        super("The verification code has expired");
    }
}
