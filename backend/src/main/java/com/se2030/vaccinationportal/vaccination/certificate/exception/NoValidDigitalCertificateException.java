package com.se2030.vaccinationportal.vaccination.certificate.exception;

/** Raised when a lifecycle operation requires a current valid certificate but none exists. */
public class NoValidDigitalCertificateException extends RuntimeException {

    public NoValidDigitalCertificateException(String message) {
        super(message);
    }
}
