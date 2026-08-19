package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when an appointment is not found by ID.
 */
public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException(String message) {
        super(message);
    }

    public AppointmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
