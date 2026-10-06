package com.se2030.vaccinationportal.vaccination.administration.exception;

/**
 * Raised when MF-05 cannot resolve the appointment referenced by a vaccination workflow.
 */
public class VaccinationAppointmentNotFoundException extends RuntimeException {

    public VaccinationAppointmentNotFoundException(String message) {
        super(message);
    }
}
