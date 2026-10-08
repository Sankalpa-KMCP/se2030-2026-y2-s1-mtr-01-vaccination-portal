package com.se2030.vaccinationportal.campaign.session.planning.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Request payload for previewing a Vaccination Session center-transfer change.
 */
public record PreviewSessionTransferRequest(
        @NotNull(message = "New center ID is required")
        Long newCenterId,

        @NotNull(message = "New start timestamp is required")
        Instant newStartAt,

        @NotNull(message = "New end timestamp is required")
        Instant newEndAt,

        Instant newBookingCloseAt
) {
}
