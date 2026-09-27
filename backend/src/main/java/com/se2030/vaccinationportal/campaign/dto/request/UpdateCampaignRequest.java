package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Request payload for updating an existing Vaccination Campaign.
 * Mutability is restricted strictly to approved configuration fields.
 */
public record UpdateCampaignRequest(
        @NotBlank(message = "Campaign name is required")
        @Size(max = 128, message = "Campaign name must not exceed 128 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Service start date is required")
        LocalDate serviceStartDate,

        @NotNull(message = "Service end date is required")
        LocalDate serviceEndDate,

        @NotNull(message = "Booking open timestamp is required")
        Instant bookingOpenAt,

        @NotNull(message = "Booking close timestamp is required")
        Instant bookingCloseAt
) {
}
