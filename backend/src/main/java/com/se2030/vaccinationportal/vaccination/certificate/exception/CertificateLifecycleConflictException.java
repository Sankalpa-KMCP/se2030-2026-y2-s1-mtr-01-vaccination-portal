package com.se2030.vaccinationportal.vaccination.certificate.exception;

/** Raised when persisted certificate state cannot support a safe lifecycle transition. */
public class CertificateLifecycleConflictException extends RuntimeException {

    public CertificateLifecycleConflictException(String message) {
        super(message);
    }
}
