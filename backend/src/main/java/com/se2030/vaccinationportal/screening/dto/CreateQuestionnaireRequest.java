package com.se2030.vaccinationportal.screening.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuestionnaireRequest(
        @NotBlank(message = "Questionnaire code is required")
        @Size(max = 64, message = "Code must not exceed 64 characters")
        String code,

        @NotBlank(message = "Questionnaire name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        String description
) {}
