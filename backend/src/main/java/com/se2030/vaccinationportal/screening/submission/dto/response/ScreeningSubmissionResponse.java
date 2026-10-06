package com.se2030.vaccinationportal.screening.submission.dto.response;

import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;

import java.time.Instant;
import java.util.List;

public record ScreeningSubmissionResponse(
        Long id,
        Long citizenId,
        Long campaignId,
        Long questionnaireVersionId,
        Long consentNoticeVersionId,
        ScreeningSubmissionStatus status,
        PreliminaryScreeningResult preliminaryResult,
        Instant consentAt,
        Instant submittedAt,
        List<ScreeningAnswerResponse> answers,
        Instant createdAt,
        Instant updatedAt,
        String correctionInstructions,
        Instant correctionRequestedAt
) {}
