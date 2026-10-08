package com.se2030.vaccinationportal.appointment.exception;

/**
 * Exception thrown when an appointment operation (check-in, reversal, no-show) is attempted on an appointment
 * whose current status is incompatible with the requested operation (MF03-BR-10, MF03-BR-11, MF03-BR-15).
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-26: Check-in, reverse check-in, and no-show processing.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-10: Only CONFIRMED appointments can be checked in.</li>
 *   <li>MF03-BR-11: COMPLETED appointments cannot be mutated.</li>
 *   <li>MF03-BR-15: Only CHECKED_IN appointments can be reversed.</li>
 * </ul>
 */
public class InvalidAppointmentStateException extends RuntimeException {

    public InvalidAppointmentStateException(String message) {
        super(message);
    }

    public InvalidAppointmentStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
