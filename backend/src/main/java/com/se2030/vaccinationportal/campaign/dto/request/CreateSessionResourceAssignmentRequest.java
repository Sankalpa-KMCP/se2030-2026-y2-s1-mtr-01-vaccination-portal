package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Request payload for assigning an operational resource to a vaccination session.
 */
public record CreateSessionResourceAssignmentRequest(
        @NotNull(message = "Resource ID is required")
        Long resourceId,

        @NotNull(message = "Start time is required")
        Instant startAt,

        @NotNull(message = "End time is required")
        Instant endAt
) {
}
