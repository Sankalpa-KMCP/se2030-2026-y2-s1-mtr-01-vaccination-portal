package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for suspending a published or active Vaccination Campaign.
 */
public record SuspendCampaignRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason,

        @NotBlank(message = "Actor ID is required")
        @Size(max = 64, message = "Actor ID cannot exceed 64 characters")
        String actorId
) {
}
