package com.se2030.vaccinationportal.vaccination.exception;

/**
 * Raised when a requested digital vaccination certificate does not exist.
 */
public class DigitalCertificateNotFoundException extends RuntimeException {

    public DigitalCertificateNotFoundException(String message) {
        super(message);
    }
}
