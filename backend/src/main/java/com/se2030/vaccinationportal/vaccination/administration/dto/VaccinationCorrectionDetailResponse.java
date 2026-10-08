package com.se2030.vaccinationportal.vaccination.administration.dto;

import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionStatus;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionType;
import java.time.Instant;

/**
 * Rich audit view of a vaccination correction request including citizen, vaccinator, and certificate details.
 */
public record VaccinationCorrectionDetailResponse(
        Long id,
        Long vaccinationRecordId,
        Long citizenId,
        String citizenName,
        Long vaccinatorId,
        String vaccinatorName,
        String certificateReference,
        String certificateStatus,
        Long certificateId,
        String fieldName,
        String oldValue,
        String newValue,
        String reason,
        Long correctedBy,
        Instant correctedAt,
        VaccinationCorrectionStatus status,
        Long reviewedBy,
        Instant reviewedAt,
        String reviewReason,
        VaccinationCorrectionType requestType
) {
    public VaccinationCorrectionDetailResponse(
            Long id,
            Long vaccinationRecordId,
            Long citizenId,
            String citizenName,
            Long vaccinatorId,
            String vaccinatorName,
            String certificateReference,
            String certificateStatus,
            Long certificateId,
            String fieldName,
            String oldValue,
            String newValue,
            String reason,
            Long correctedBy,
            Instant correctedAt,
            VaccinationCorrectionStatus status,
            Long reviewedBy,
            Instant reviewedAt,
            String reviewReason
    ) {
        this(
                id,
                vaccinationRecordId,
                citizenId,
                citizenName,
                vaccinatorId,
                vaccinatorName,
                certificateReference,
                certificateStatus,
                certificateId,
                fieldName,
                oldValue,
                newValue,
                reason,
                correctedBy,
                correctedAt,
                status,
                reviewedBy,
                reviewedAt,
                reviewReason,
                VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE
        );
    }
}
