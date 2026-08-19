package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when a time slot has no available bookable capacity (MF03-BR-03).
 */
public class SlotFullException extends RuntimeException {

    public SlotFullException(String message) {
        super(message);
    }

    public SlotFullException(String message, Throwable cause) {
        super(message, cause);
    }
}
