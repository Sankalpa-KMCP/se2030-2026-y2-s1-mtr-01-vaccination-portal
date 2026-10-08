package com.se2030.vaccinationportal.vaccination.administration.dto;

import com.se2030.vaccinationportal.vaccination.certificate.model.CertificateStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Meaningful representation of a finalized permanent vaccination record administered by a vaccinator.
 */
public record VaccinatorVaccinationRecordResponse(
        Long id,
        Long appointmentId,
        Long citizenId,
        String citizenName,
        Long campaignId,
        String campaignName,
        Long centerId,
        String centerName,
        Long vaccineBatchId,
        String batchNumber,
        String vaccineName,
        Integer doseNumber,
        Instant administrationTime,
        LocalDate nextDoseDate,
        String authorizedNotes,
        Instant finalizedAt,
        Long certificateId,
        String certificateReference,
        CertificateStatus certificateStatus
) {
}
