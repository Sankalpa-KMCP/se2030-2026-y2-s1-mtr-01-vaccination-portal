package com.se2030.vaccinationportal.campaign.session.planning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request payload for confirming a Vaccination Session center-transfer change.
 */
public record ConfirmSessionTransferRequest(
        @NotNull(message = "New center ID is required")
        Long newCenterId,

        @NotNull(message = "New start timestamp is required")
        Instant newStartAt,

        @NotNull(message = "New end timestamp is required")
        Instant newEndAt,

        Instant newBookingCloseAt,

        @NotBlank(message = "Preview token is required")
        String previewToken,

        @NotBlank(message = "Change reason is required")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason,

        @Size(max = 64, message = "Actor ID cannot exceed 64 characters")
        String actorId
) {
}
