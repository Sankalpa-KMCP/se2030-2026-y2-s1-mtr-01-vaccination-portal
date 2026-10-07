package com.se2030.vaccinationportal.screening.questionnaire.dto.response;

public record ScreeningHistoryAnswerOptionResponse(
        Long id,
        String code,
        String label,
        String value
) {}
