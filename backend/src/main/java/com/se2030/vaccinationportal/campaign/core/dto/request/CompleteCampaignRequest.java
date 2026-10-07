package com.se2030.vaccinationportal.campaign.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for completing an active, published, or suspended Vaccination Campaign.
 */
public record CompleteCampaignRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason
) {
}
