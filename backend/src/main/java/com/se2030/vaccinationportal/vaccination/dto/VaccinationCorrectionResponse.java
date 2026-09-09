package com.se2030.vaccinationportal.vaccination.dto;

import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus;
import java.time.Instant;

/** Safe API representation of an append-only vaccination correction. */
public record VaccinationCorrectionResponse(
        Long id,
        Long vaccinationRecordId,
        String fieldName,
        String oldValue,
        String newValue,
        String reason,
        Long correctedBy,
        Instant correctedAt,
        VaccinationCorrectionStatus status
) {
}
