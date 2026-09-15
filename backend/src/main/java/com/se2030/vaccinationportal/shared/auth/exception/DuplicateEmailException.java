package com.se2030.vaccinationportal.shared.auth.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("An account already exists for this email address");
    }
}
