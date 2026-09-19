package com.se2030.vaccinationportal.shared.auth.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
        super("The authentication token has expired");
    }

    public TokenExpiredException(String message) {
        super(message);
    }
}
