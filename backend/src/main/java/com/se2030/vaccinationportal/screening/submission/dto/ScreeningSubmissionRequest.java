package com.se2030.vaccinationportal.screening.submission.dto;

import com.se2030.vaccinationportal.screening.validation.ScreeningAnswerMapDeserializer;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

public record ScreeningSubmissionRequest(
        @NotNull(message = "Citizen ID is required")
        Long citizenId,

        @NotNull(message = "Campaign ID is required")
        Long campaignId,

        @NotNull(message = "Questionnaire Version ID is required")
        Long questionnaireVersionId,

        @NotNull(message = "Consent Notice Version ID is required")
        Long consentNoticeVersionId,

        @NotEmpty(message = "Screening answers are required")
        @JsonDeserialize(using = ScreeningAnswerMapDeserializer.class)
        Map<Long, String> answers
) {}
