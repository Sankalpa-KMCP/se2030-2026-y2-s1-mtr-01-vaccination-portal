package com.se2030.vaccinationportal.screening.questionnaire.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateQuestionRequest(
        @NotBlank(message = "Question text is required")
        @Size(max = 1000, message = "Text must not exceed 1000 characters")
        String text,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        boolean defaultRequired,
        boolean active
) {}
