package com.se2030.vaccinationportal.shared.auth.exception;

public class EmailOtpInvalidException extends RuntimeException {
    public EmailOtpInvalidException() {
        super("The verification code is incorrect");
    }
}
