package com.se2030.vaccinationportal.vaccination.query.model;

import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;


import java.time.Instant;
import java.time.LocalDate;

/**
 * Immutable, non-persistent view of a finalized vaccination record after applied corrections.
 */
public record EffectiveVaccinationRecordSnapshot(
        Long vaccinationRecordId,
        Long appointmentId,
        Long citizenId,
        Long campaignId,
        Long centerId,
        Long vaccinatorId,
        Long vaccineBatchId,
        Integer doseNumber,
        Instant administrationTime,
        LocalDate nextDoseDate,
        String authorizedNotes,
        Instant finalizedAt
) {

    public static EffectiveVaccinationRecordSnapshot from(VaccinationRecord record) {
        return new EffectiveVaccinationRecordSnapshot(
                record.getId(),
                record.getAppointmentId(),
                record.getCitizenId(),
                record.getCampaignId(),
                record.getCenterId(),
                record.getVaccinatorId(),
                record.getVaccineBatchId(),
                record.getDoseNumber(),
                record.getAdministrationTime(),
                record.getNextDoseDate(),
                record.getAuthorizedNotes(),
                record.getFinalizedAt()
        );
    }
}
