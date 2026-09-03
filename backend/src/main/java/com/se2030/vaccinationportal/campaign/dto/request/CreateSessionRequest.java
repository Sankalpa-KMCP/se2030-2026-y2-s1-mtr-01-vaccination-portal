package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Request payload for creating a new one-off Vaccination Session.
 */
public record CreateSessionRequest(
        @NotNull(message = "Campaign ID is required")
        Long campaignId,

        @NotNull(message = "Center ID is required")
        Long centerId,

        @NotNull(message = "Session start timestamp is required")
        Instant startAt,

        @NotNull(message = "Session end timestamp is required")
        Instant endAt,

        @NotNull(message = "Booking close timestamp is required")
        Instant bookingCloseAt
) {
}
