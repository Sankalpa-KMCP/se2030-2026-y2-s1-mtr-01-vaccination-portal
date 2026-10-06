package com.se2030.vaccinationportal.appointment.api.vaccination;

/**
 * Immutable appointment state exposed to MF-05 without leaking the MF-03 JPA entity.
 *
 * @param appointmentId        resolved appointment identifier
 * @param checkedIn            whether the appointment is currently checked in
 * @param citizenId            identifier of the citizen
 * @param campaignId           identifier of the campaign
 * @param slotId               identifier of the scheduled time slot
 * @param centerId             identifier of the vaccination center
 * @param campaignVaccineTypeId authoritative vaccine type of the appointment's campaign, used to
 *                             verify batch compatibility during finalization (backend-resolved;
 *                             never trusted from the request)
 */
public record AppointmentValidationSnapshot(
        Long appointmentId,
        boolean checkedIn,
        Long citizenId,
        Long campaignId,
        Long slotId,
        Long centerId,
        Long campaignVaccineTypeId
) {
    public AppointmentValidationSnapshot(Long appointmentId, boolean checkedIn) {
        this(appointmentId, checkedIn, null, null, null, null, null);
    }

    public AppointmentValidationSnapshot(
            Long appointmentId,
            boolean checkedIn,
            Long citizenId,
            Long campaignId,
            Long slotId,
            Long centerId
    ) {
        this(appointmentId, checkedIn, citizenId, campaignId, slotId, centerId, null);
    }
}
