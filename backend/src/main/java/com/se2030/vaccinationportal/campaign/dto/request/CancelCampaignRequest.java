package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for cancelling a Vaccination Campaign.
 */
public record CancelCampaignRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason
) {
}
