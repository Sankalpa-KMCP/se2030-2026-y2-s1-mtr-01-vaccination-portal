package com.se2030.vaccinationportal.vaccination.exception;

/** Raised when a correction targets a field outside the approved MF-05 whitelist. */
public class UnsupportedVaccinationCorrectionFieldException extends RuntimeException {

    public UnsupportedVaccinationCorrectionFieldException(String message) {
        super(message);
    }
}
