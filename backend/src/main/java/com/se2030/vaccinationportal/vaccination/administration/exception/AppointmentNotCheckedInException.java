package com.se2030.vaccinationportal.vaccination.administration.exception;

/**
 * Raised when a vaccination workflow requires an appointment that is not checked in.
 */
public class AppointmentNotCheckedInException extends RuntimeException {

    public AppointmentNotCheckedInException(String message) {
        super(message);
    }
}
