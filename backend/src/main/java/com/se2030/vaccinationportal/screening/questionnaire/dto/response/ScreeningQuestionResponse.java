package com.se2030.vaccinationportal.screening.questionnaire.dto.response;

import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;

import java.time.Instant;

public record ScreeningQuestionResponse(
        Long id,
        String code,
        String text,
        String description,
        QuestionType questionType,
        boolean defaultRequired,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
