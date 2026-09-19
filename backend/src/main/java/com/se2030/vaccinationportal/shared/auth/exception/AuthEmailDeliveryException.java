package com.se2030.vaccinationportal.shared.auth.exception;

/**
 * Thrown when an authentication email delivery attempt via SMTP fails.
 */
public class AuthEmailDeliveryException extends RuntimeException {

    public AuthEmailDeliveryException(String message) {
        super(message);
    }

    public AuthEmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
