package com.se2030.vaccinationportal.screening.correction.dto.response;

public record ScreeningCorrectionFormAnswerOptionResponse(
        Long id,
        String code,
        String label,
        String typedValue,
        int displayOrder
) {}
