package com.se2030.vaccinationportal.vaccination.dependency;

/**
 * Immutable appointment state exposed to MF-05 without leaking the MF-03 JPA entity.
 *
 * @param appointmentId resolved appointment identifier
 * @param checkedIn     whether the appointment is currently checked in
 * @param citizenId     identifier of the citizen
 * @param campaignId    identifier of the campaign
 * @param slotId        identifier of the scheduled time slot
 * @param centerId      identifier of the vaccination center
 */
public record AppointmentValidationSnapshot(
        Long appointmentId,
        boolean checkedIn,
        Long citizenId,
        Long campaignId,
        Long slotId,
        Long centerId
) {
    public AppointmentValidationSnapshot(Long appointmentId, boolean checkedIn) {
        this(appointmentId, checkedIn, null, null, null, null);
    }
}
