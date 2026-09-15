package com.se2030.vaccinationportal.vaccination.exception;

/**
 * Raised when no digital certificate matches a verification token.
 */
public class CertificateVerificationNotFoundException extends RuntimeException {

    public CertificateVerificationNotFoundException(String message) {
        super(message);
    }
}
