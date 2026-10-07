package com.se2030.vaccinationportal.shared.auth.exception;

public class AccountPendingVerificationException extends RuntimeException {
    public AccountPendingVerificationException() {
        super("Account email verification is pending");
    }

    public AccountPendingVerificationException(String message) {
        super(message);
    }
}
