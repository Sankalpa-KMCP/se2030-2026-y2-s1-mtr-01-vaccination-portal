package com.se2030.vaccinationportal.screening.dto;

import jakarta.validation.constraints.NotNull;

public record AddQuestionItemRequest(
        @NotNull(message = "Question ID is required")
        Long questionId,

        int displayOrder,
        Boolean requiredOverride
) {}
