package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Request payload for previewing a Vaccination Session reschedule change.
 */
public record PreviewSessionRescheduleRequest(
        @NotNull(message = "New start timestamp is required")
        Instant newStartAt,

        @NotNull(message = "New end timestamp is required")
        Instant newEndAt,

        Instant newBookingCloseAt
) {
}
