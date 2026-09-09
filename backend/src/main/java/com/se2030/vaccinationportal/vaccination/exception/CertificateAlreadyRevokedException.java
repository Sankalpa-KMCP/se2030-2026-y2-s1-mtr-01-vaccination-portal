package com.se2030.vaccinationportal.vaccination.exception;

/** Raised when a revocation is requested for a certificate that is already revoked. */
public class CertificateAlreadyRevokedException extends RuntimeException {

    public CertificateAlreadyRevokedException(String message) {
        super(message);
    }
}
