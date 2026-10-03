package com.se2030.vaccinationportal.shared.auth.exception;

public class EmailAlreadyVerifiedException extends RuntimeException {
    public EmailAlreadyVerifiedException() {
        super("Email is already verified");
    }
}
