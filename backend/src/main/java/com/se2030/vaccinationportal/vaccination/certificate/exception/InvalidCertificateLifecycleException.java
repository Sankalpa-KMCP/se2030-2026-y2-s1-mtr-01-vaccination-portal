package com.se2030.vaccinationportal.vaccination.certificate.exception;

/** Raised when certificate lifecycle request data violates MF-05 business validation. */
public class InvalidCertificateLifecycleException extends RuntimeException {

    public InvalidCertificateLifecycleException(String message) {
        super(message);
    }
}
