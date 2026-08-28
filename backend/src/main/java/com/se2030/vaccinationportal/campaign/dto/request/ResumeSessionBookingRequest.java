package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for authoritatively resuming booking intake on a vaccination session.
 */
public record ResumeSessionBookingRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason
) {
}
