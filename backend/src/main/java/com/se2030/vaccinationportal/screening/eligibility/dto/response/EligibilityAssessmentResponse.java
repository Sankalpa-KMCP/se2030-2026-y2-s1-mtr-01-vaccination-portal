package com.se2030.vaccinationportal.screening.eligibility.dto.response;

import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;

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
