package com.se2030.vaccinationportal.vaccination.exception;

/**
 * Raised when draft data or a requested draft mutation violates MF-05 rules.
 */
public class InvalidVaccinationDraftException extends RuntimeException {

    public InvalidVaccinationDraftException(String message) {
        super(message);
    }
}
