package com.se2030.vaccinationportal.vaccination.certificate.exception;

/**
 * Raised when a certificate verification token is absent or blank.
 */
public class InvalidCertificateVerificationTokenException extends RuntimeException {

    public InvalidCertificateVerificationTokenException(String message) {
        super(message);
    }
}
