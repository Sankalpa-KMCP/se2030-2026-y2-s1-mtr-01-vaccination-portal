package com.se2030.vaccinationportal.vaccination.exception;

/**
 * Raised when a requested vaccination draft does not exist.
 */
public class VaccinationDraftNotFoundException extends RuntimeException {

    public VaccinationDraftNotFoundException(String message) {
        super(message);
    }
}
