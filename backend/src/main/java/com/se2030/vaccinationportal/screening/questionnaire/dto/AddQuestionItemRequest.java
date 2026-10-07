package com.se2030.vaccinationportal.screening.questionnaire.dto;

import com.se2030.vaccinationportal.screening.validation.StrictIntegerDeserializer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

public record AddQuestionItemRequest(
        @NotNull(message = "Question ID is required")
        Long questionId,

        @NotNull(message = "Display order is required")
        @Min(value = 1, message = "Display order must be at least 1")
        @JsonDeserialize(using = StrictIntegerDeserializer.class)
        Integer displayOrder,

        Boolean requiredOverride
) {}
