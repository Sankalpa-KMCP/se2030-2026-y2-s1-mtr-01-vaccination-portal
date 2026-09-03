package com.se2030.vaccinationportal.screening.dto.response;

public record ScreeningFormAnswerOptionResponse(
        Long id,
        String code,
        String label,
        String typedValue,
        int displayOrder
) {}
