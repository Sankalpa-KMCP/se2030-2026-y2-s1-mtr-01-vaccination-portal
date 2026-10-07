package com.se2030.vaccinationportal.screening.submission.dto.response;

import java.time.Instant;

public record ScreeningAnswerResponse(
        Long id,
        Long questionId,
        String typedAnswer,
        Instant createdAt
) {}
