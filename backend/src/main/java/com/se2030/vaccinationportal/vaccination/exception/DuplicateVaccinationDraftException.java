package com.se2030.vaccinationportal.vaccination.exception;

/**
 * Raised when an appointment already owns an active vaccination draft.
 */
public class DuplicateVaccinationDraftException extends RuntimeException {

    public DuplicateVaccinationDraftException(String message) {
        super(message);
    }

    public DuplicateVaccinationDraftException(String message, Throwable cause) {
        super(message, cause);
    }
}
