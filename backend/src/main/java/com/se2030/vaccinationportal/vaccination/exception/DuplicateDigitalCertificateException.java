package com.se2030.vaccinationportal.vaccination.exception;

/**
 * Raised when a vaccination record already owns a digital certificate.
 */
public class DuplicateDigitalCertificateException extends RuntimeException {

    public DuplicateDigitalCertificateException(String message) {
        super(message);
    }

    public DuplicateDigitalCertificateException(String message, Throwable cause) {
        super(message, cause);
    }
}
