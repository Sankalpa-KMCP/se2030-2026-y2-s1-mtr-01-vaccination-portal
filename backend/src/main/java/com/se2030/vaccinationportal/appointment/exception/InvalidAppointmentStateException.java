package com.se2030.vaccinationportal.appointment.exception;

/**
 * Exception thrown when an appointment operation (check-in, reversal, no-show) is attempted on an appointment
 * whose current status is incompatible with the requested operation (MF03-BR-10, MF03-BR-11, MF03-BR-15).
 */
public class InvalidAppointmentStateException extends RuntimeException {

    public InvalidAppointmentStateException(String message) {
        super(message);
    }

    public InvalidAppointmentStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
