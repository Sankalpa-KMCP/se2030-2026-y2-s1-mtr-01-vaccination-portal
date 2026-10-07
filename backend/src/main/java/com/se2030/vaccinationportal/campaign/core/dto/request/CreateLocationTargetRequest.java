package com.se2030.vaccinationportal.campaign.core.dto.request;

import com.se2030.vaccinationportal.campaign.core.model.LocationTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for creating an administrative location target for a campaign.
 */
public record CreateLocationTargetRequest(
        @NotNull(message = "Location target type is required")
        LocationTargetType targetType,

        @NotBlank(message = "Target code is required")
        String targetCode
) {}
