package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when a citizen is not eligible to book an appointment or join/promote from a waitlist (MF03-BR-01, MF03-BR-07).
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Booking appointment gate.</li>
 *   <li>US-05: Waitlist join and promotion gate.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-01: Booking and waitlist joining require a currently valid ELIGIBLE assessment for the same campaign.</li>
 *   <li>MF03-BR-07: Waitlist promotion re-checks screening eligibility before converting to a confirmed appointment.</li>
 * </ul>
 */
public class NotEligibleException extends RuntimeException {

    public NotEligibleException(String message) {
        super(message);
    }

    public NotEligibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
