package com.se2030.vaccinationportal.screening.dto.response;

import com.se2030.vaccinationportal.screening.model.QuestionType;

import java.time.Instant;
import java.util.List;

public record ScreeningReviewAnswerResponse(
        Long id,
        Long questionnaireItemId,
        Long questionId,
        String questionCode,
        String questionText,
        QuestionType questionType,
        int displayOrder,
        boolean required,
        String typedAnswer,
        Instant createdAt,
        List<ScreeningReviewAnswerOptionResponse> selectedOptions
) {}
