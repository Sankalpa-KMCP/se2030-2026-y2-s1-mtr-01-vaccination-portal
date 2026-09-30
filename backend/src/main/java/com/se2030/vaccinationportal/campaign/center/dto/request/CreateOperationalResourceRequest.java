package com.se2030.vaccinationportal.campaign.center.dto.request;

import com.se2030.vaccinationportal.campaign.center.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a physical operational resource under a vaccination center.
 */
public record CreateOperationalResourceRequest(
        @NotBlank(message = "Resource code is required")
        @Size(max = 64, message = "Resource code must not exceed 64 characters")
        String code,

        @NotNull(message = "Resource type is required")
        ResourceType resourceType,

        @NotBlank(message = "Resource name is required")
        @Size(max = 128, message = "Resource name must not exceed 128 characters")
        String name,

        OperationalResourceStatus status
) {
}
