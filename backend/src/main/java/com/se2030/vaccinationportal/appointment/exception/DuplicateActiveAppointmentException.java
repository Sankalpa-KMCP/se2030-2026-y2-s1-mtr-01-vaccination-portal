package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when a citizen already has an active appointment for the campaign (MF03-BR-02).
 */
public class DuplicateActiveAppointmentException extends RuntimeException {

    public DuplicateActiveAppointmentException(String message) {
        super(message);
    }

    public DuplicateActiveAppointmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
