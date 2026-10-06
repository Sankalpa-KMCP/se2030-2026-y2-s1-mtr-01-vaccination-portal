package com.se2030.vaccinationportal.vaccination.administration.exception;

/**
 * Thrown when an active (PENDING or APPROVED) correction request already exists for a vaccination record.
 */
public class DuplicateVaccinationCorrectionException extends RuntimeException {

    public DuplicateVaccinationCorrectionException(String message) {
        super(message);
    }
}
