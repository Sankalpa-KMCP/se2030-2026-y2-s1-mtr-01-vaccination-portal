package com.se2030.vaccinationportal.shared.auth.exception;

public class InvalidRejectionReasonException extends RuntimeException {
    public InvalidRejectionReasonException() {
        super("Rejection reason must be nonblank and at most 500 characters");
    }
}
