package com.se2030.vaccinationportal.campaign.session.planning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for authoritatively suspending booking intake on a vaccination session.
 */
public record SuspendSessionBookingRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason
) {
}
