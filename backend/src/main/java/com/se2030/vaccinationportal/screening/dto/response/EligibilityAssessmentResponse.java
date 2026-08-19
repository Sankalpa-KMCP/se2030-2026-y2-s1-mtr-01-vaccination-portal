package com.se2030.vaccinationportal.screening.dto.response;

import com.se2030.vaccinationportal.screening.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.model.PreliminaryScreeningResult;

import java.time.Instant;

public record EligibilityAssessmentResponse(
        Long id,
        Long submissionId,
        Long citizenId,
        Long campaignId,
        Long reviewerId,
        EligibilityAssessmentStatus status,
        PreliminaryScreeningResult preliminaryResult,
        String reason,
        String clinicalNotes,
        Instant reviewedAt,
        Instant validUntil,
        Instant revokedAt,
        String revocationReason,
        Instant rescreeningAuthorizedAt,
        Long rescreeningAuthorizedBy,
        String rescreeningReason,
        Instant createdAt,
        Instant updatedAt
) {}
