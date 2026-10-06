package com.se2030.vaccinationportal.screening.submission.dto.response;

import com.se2030.vaccinationportal.screening.questionnaire.dto.response.ScreeningFormAnswerOptionResponse;


import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;

import java.util.List;

public record ScreeningFormQuestionResponse(
        Long id,
        String code,
        String text,
        String description,
        QuestionType questionType,
        boolean required,
        int displayOrder,
        List<ScreeningFormAnswerOptionResponse> options
) {}
