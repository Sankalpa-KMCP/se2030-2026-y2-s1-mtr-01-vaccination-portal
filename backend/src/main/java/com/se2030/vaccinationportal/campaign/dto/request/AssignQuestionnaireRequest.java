package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Request payload for assigning a published questionnaire version to a campaign.
 */
public record AssignQuestionnaireRequest(
        @NotNull(message = "Questionnaire version ID is required")
        @Positive(message = "Questionnaire version ID must be positive")
        Long questionnaireVersionId,

        @NotNull(message = "Effective start date is required")
        LocalDate effectiveStartDate
) {}
