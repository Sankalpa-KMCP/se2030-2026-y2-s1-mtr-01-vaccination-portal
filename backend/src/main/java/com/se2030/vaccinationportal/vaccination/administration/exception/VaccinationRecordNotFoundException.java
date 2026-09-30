package com.se2030.vaccinationportal.vaccination.administration.exception;

/**
 * Raised when certificate issuance targets a vaccination record that does not exist.
 */
public class VaccinationRecordNotFoundException extends RuntimeException {

    public VaccinationRecordNotFoundException(String message) {
        super(message);
    }
}
