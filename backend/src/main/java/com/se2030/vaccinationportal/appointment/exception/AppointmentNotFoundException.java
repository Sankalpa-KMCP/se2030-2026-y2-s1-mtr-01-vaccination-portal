package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when an appointment or waitlist entry is not found by ID.
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Cancellation and rescheduling endpoint lookups.</li>
 *   <li>US-05: Waitlist leave endpoint lookups.</li>
 *   <li>US-26: Check-in, reverse check-in, and no-show endpoint lookups.</li>
 * </ul>
 */
public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException(String message) {
        super(message);
    }

    public AppointmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
