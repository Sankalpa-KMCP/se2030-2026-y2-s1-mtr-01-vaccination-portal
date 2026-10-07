package com.se2030.vaccinationportal.screening.questionnaire.dto;

import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(
        @NotBlank(message = "Question code is required")
        @Size(max = 64, message = "Code must not exceed 64 characters")
        String code,

        @NotBlank(message = "Question text is required")
        @Size(max = 1000, message = "Text must not exceed 1000 characters")
        String text,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Question type is required")
        QuestionType questionType,

        boolean defaultRequired
) {}
