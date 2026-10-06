package com.se2030.vaccinationportal.screening.questionnaire.dto.response;

import java.time.Instant;

public record ScreeningFormConsentNoticeResponse(
        Long id,
        String noticeCode,
        Integer versionNumber,
        String title,
        String content,
        String contentHash,
        Instant publishedAt
) {}
