package com.se2030.vaccinationportal.shared.auth.exception;

public class TokenAlreadyUsedException extends RuntimeException {
    public TokenAlreadyUsedException() {
        super("The authentication token has already been used");
    }

    public TokenAlreadyUsedException(String message) {
        super(message);
    }
}
