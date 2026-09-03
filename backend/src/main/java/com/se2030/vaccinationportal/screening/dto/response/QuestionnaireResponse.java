package com.se2030.vaccinationportal.screening.dto.response;

import java.time.Instant;

public record QuestionnaireResponse(
        Long id,
        String code,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
