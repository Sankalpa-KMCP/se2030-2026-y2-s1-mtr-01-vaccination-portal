package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when attempting to cancel, reschedule, or check in an appointment
 * that is already COMPLETED (MF03-BR-11).
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Cancellation and rescheduling protection.</li>
 *   <li>US-26: Check-in queue protection.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-11: A COMPLETED appointment cannot be cancelled, rescheduled, or checked in again.</li>
 * </ul>
 */
public class AppointmentAlreadyCompletedException extends RuntimeException {

    public AppointmentAlreadyCompletedException(String message) {
        super(message);
    }

    public AppointmentAlreadyCompletedException(String message, Throwable cause) {
        super(message, cause);
    }
}
