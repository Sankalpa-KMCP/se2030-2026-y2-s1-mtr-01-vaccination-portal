package com.se2030.vaccinationportal.screening.dto.response;

import java.util.List;

public record ScreeningCorrectionFormCurrentAnswerResponse(
        String typedAnswer,
        List<ScreeningCorrectionFormAnswerOptionResponse> selectedOptions
) {}
