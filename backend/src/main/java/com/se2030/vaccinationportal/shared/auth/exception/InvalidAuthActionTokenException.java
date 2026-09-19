package com.se2030.vaccinationportal.shared.auth.exception;

public class InvalidAuthActionTokenException extends RuntimeException {
    public InvalidAuthActionTokenException() {
        super("Invalid or unrecognizable authentication action token");
    }

    public InvalidAuthActionTokenException(String message) {
        super(message);
    }
}
