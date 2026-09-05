package com.se2030.vaccinationportal.screening.dto.response;

import com.se2030.vaccinationportal.screening.model.QuestionType;

import java.util.List;

public record ScreeningCorrectionFormQuestionResponse(
        Long id,
        String code,
        String text,
        String description,
        QuestionType questionType,
        boolean required,
        int displayOrder,
        List<ScreeningCorrectionFormAnswerOptionResponse> options,
        ScreeningCorrectionFormCurrentAnswerResponse currentAnswer
) {}
