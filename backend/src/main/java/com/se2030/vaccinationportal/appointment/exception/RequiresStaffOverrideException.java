package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when a citizen attempts to cancel an appointment within 24 hours of session start (MF03-BR-04).
 */
public class RequiresStaffOverrideException extends RuntimeException {

    public RequiresStaffOverrideException(String message) {
        super(message);
    }

    public RequiresStaffOverrideException(String message, Throwable cause) {
        super(message, cause);
    }
}
