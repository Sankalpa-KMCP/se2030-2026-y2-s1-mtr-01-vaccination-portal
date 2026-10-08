package com.se2030.vaccinationportal.appointment.exception;

/**
 * Exception thrown when a citizen attempts to join a waitlist for a campaign in which
 * they already possess an active WAITING waitlist entry (MF03-BR-06 / MF03-BR-14).
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-05: Waitlist join validation.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-06: Only one active WAITING entry per citizen per campaign is permitted.</li>
 *   <li>MF03-BR-14: Duplicate waitlist protection across sessions within the same campaign.</li>
 * </ul>
 */
public class DuplicateWaitlistEntryException extends RuntimeException {

    public DuplicateWaitlistEntryException(String message) {
        super(message);
    }

    public DuplicateWaitlistEntryException(String message, Throwable cause) {
        super(message, cause);
    }
}
