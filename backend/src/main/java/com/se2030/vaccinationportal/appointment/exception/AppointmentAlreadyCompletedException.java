package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when attempting to cancel an appointment that is already COMPLETED (MF03-BR-11).
 */
public class AppointmentAlreadyCompletedException extends RuntimeException {

    public AppointmentAlreadyCompletedException(String message) {
        super(message);
    }

    public AppointmentAlreadyCompletedException(String message, Throwable cause) {
        super(message, cause);
    }
}
