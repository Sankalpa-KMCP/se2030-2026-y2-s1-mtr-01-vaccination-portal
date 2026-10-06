package com.se2030.vaccinationportal.screening.questionnaire.dto.response;

public record ScreeningReviewAnswerOptionResponse(
        Long id,
        String code,
        String label,
        String value
) {}
