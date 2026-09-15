package com.se2030.vaccinationportal.vaccination.exception;

/**
 * Raised when an appointment already has a permanent vaccination record.
 */
public class VaccinationRecordAlreadyExistsException extends RuntimeException {

    public VaccinationRecordAlreadyExistsException(String message) {
        super(message);
    }
}
