package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when a citizen is not eligible to book an appointment (MF03-BR-01).
 */
public class NotEligibleException extends RuntimeException {

    public NotEligibleException(String message) {
        super(message);
    }

    public NotEligibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
