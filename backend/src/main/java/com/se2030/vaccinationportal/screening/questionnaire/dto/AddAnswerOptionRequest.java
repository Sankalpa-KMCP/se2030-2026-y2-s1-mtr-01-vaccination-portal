package com.se2030.vaccinationportal.screening.questionnaire.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddAnswerOptionRequest(
        @NotBlank(message = "Option code is required")
        @Size(max = 64, message = "Code must not exceed 64 characters")
        String code,

        @NotBlank(message = "Option label is required")
        @Size(max = 255, message = "Label must not exceed 255 characters")
        String label,

        @NotBlank(message = "Option typed value is required")
        @Size(max = 255, message = "Typed value must not exceed 255 characters")
        String typedValue,

        int displayOrder
) {}
