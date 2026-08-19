package com.se2030.vaccinationportal.screening.dto.response;

import java.time.Instant;

public record QuestionnaireItemResponse(
        Long id,
        Long questionnaireVersionId,
        Long questionId,
        int displayOrder,
        Boolean requiredOverride,
        Instant createdAt
) {}
