package com.se2030.vaccinationportal.screening.questionnaire.dto.response;

import java.time.Instant;

public record AnswerOptionResponse(
        Long id,
        Long questionId,
        String code,
        String label,
        String typedValue,
        int displayOrder,
        Instant createdAt,
        Instant updatedAt
) {}
