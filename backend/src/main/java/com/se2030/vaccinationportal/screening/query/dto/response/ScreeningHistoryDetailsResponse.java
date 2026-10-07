package com.se2030.vaccinationportal.screening.query.dto.response;

import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;

import java.time.Instant;
import java.util.List;

/** Citizen-safe historical display, deliberately excluding clinical review data. */
public record ScreeningHistoryDetailsResponse(
        Long id,
        Long citizenId,
        Long campaignId,
        Long questionnaireVersionId,
        Integer questionnaireVersionNumber,
        ScreeningSubmissionStatus status,
        PreliminaryScreeningResult preliminaryResult,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt,
        String correctionInstructions,
        Instant correctionRequestedAt,
        List<ScreeningHistoryAnswerResponse> answers
) {}
