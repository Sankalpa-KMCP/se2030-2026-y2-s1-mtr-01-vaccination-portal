package com.se2030.vaccinationportal.screening.dto.response;

public record ScreeningReviewAnswerOptionResponse(
        Long id,
        String code,
        String label,
        String value
) {}
