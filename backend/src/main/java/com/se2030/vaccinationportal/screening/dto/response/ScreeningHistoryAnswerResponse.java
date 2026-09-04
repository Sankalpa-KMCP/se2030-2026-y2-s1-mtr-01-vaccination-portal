package com.se2030.vaccinationportal.screening.dto.response;

import com.se2030.vaccinationportal.screening.model.QuestionType;

import java.util.List;

public record ScreeningHistoryAnswerResponse(
        Long id,
        Long questionId,
        String questionCode,
        String questionText,
        QuestionType questionType,
        Integer displayOrder,
        String typedAnswer,
        String displayAnswer,
        List<ScreeningHistoryAnswerOptionResponse> selectedOptions
) {}
