package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when a citizen already has an active appointment for the campaign (MF03-BR-02).
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Booking validation.</li>
 *   <li>US-05: Waitlist join and promotion validation.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-02: Exactly one active appointment per citizen per campaign is enforced.</li>
 * </ul>
 */
public class DuplicateActiveAppointmentException extends RuntimeException {

    public DuplicateActiveAppointmentException(String message) {
        super(message);
    }

    public DuplicateActiveAppointmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
