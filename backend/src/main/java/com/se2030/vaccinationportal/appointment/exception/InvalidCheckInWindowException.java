package com.se2030.vaccinationportal.appointment.exception;

/**
 * Exception thrown when a check-in or no-show attempt violates the allowable check-in window (MF03-BR-09).
 */
public class InvalidCheckInWindowException extends RuntimeException {

    public InvalidCheckInWindowException(String message) {
        super(message);
    }

    public InvalidCheckInWindowException(String message, Throwable cause) {
        super(message, cause);
    }
}
