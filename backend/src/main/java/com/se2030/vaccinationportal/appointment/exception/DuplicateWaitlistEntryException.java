package com.se2030.vaccinationportal.appointment.exception;

/**
 * Exception thrown when a citizen attempts to join a waitlist for a campaign in which
 * they already possess an active WAITING waitlist entry (MF03-BR-06 / MF03-BR-14).
 */
public class DuplicateWaitlistEntryException extends RuntimeException {

    public DuplicateWaitlistEntryException(String message) {
        super(message);
    }

    public DuplicateWaitlistEntryException(String message, Throwable cause) {
        super(message, cause);
    }
}
