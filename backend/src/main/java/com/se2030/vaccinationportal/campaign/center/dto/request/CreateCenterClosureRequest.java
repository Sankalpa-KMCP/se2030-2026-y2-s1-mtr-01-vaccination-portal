package com.se2030.vaccinationportal.campaign.center.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request payload for registering a temporary vaccination center closure interval.
 */
public record CreateCenterClosureRequest(
        @NotNull(message = "Closure start is required")
        Instant startAt,

        @NotNull(message = "Closure end is required")
        Instant endAt,

        @NotBlank(message = "Closure reason is required")
        @Size(max = 255, message = "Closure reason must not exceed 255 characters")
        String reason
) {
}
