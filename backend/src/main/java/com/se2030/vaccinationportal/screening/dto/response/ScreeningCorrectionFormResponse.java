package com.se2030.vaccinationportal.screening.dto.response;

import com.se2030.vaccinationportal.screening.model.ScreeningSubmissionStatus;

import java.time.Instant;
import java.util.List;

/** Citizen-safe editable form for the questionnaire version stored on a correction submission. */
public record ScreeningCorrectionFormResponse(
        Long submissionId,
        Long campaignId,
        Long questionnaireVersionId,
        Integer questionnaireVersionNumber,
        ScreeningSubmissionStatus status,
        String correctionInstructions,
        Instant correctionRequestedAt,
        List<ScreeningCorrectionFormQuestionResponse> questions
) {}
