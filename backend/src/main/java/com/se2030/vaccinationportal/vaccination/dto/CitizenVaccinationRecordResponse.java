package com.se2030.vaccinationportal.vaccination.dto;

import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Safe representation of a citizen's finalized vaccination history record with certificate status.
 */
public record CitizenVaccinationRecordResponse(
        Long id,
        Long appointmentId,
        Long citizenId,
        Long campaignId,
        Long centerId,
        Long vaccineBatchId,
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
