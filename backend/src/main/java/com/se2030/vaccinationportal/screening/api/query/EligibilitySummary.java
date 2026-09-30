package com.se2030.vaccinationportal.screening.api.query;

import java.time.Instant;

public record EligibilitySummary(Long campaignId, String recordedStatus, Instant reviewedAt,
        Instant validUntil, String lifecycleState) {
}
