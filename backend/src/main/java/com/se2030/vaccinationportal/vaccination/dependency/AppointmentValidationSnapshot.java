package com.se2030.vaccinationportal.vaccination.dependency;

/**
 * Immutable appointment state exposed to MF-05 without leaking the MF-03 JPA entity.
 *
 * @param appointmentId resolved appointment identifier
 * @param checkedIn     whether the appointment is currently checked in
 */
public record AppointmentValidationSnapshot(
        Long appointmentId,
        boolean checkedIn
) {
}
