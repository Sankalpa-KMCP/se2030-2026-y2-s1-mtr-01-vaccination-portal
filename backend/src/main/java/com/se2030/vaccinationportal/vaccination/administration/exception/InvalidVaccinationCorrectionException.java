package com.se2030.vaccinationportal.vaccination.administration.exception;

/** Raised when a requested vaccination correction violates MF-05 business rules. */
public class InvalidVaccinationCorrectionException extends RuntimeException {

    public InvalidVaccinationCorrectionException(String message) {
        super(message);
    }
}
