package com.se2030.vaccinationportal.vaccination.administration.dto;

import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionStatus;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionType;
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
        VaccinationCorrectionStatus status,
        VaccinationCorrectionType requestType
) {
    public VaccinationCorrectionResponse(
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
        this(id, vaccinationRecordId, fieldName, oldValue, newValue, reason, correctedBy, correctedAt, status, VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE);
    }
}
