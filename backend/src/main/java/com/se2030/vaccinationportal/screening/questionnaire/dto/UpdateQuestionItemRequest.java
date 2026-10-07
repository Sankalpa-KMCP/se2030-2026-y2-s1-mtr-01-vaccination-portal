package com.se2030.vaccinationportal.screening.questionnaire.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateQuestionItemRequest(
        @NotNull(message = "Question ID is required")
        Long questionId,

        Boolean requiredOverride
) {}
