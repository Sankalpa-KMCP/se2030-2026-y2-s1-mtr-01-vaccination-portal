package com.se2030.vaccinationportal.campaign.center.dto.request;

import com.se2030.vaccinationportal.campaign.center.model.OperationalResourceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating the name and status of an operational resource.
 * Resource code and resourceType are immutable.
 */
public record UpdateOperationalResourceRequest(
        @NotBlank(message = "Resource name is required")
        @Size(max = 128, message = "Resource name must not exceed 128 characters")
        String name,

        @NotNull(message = "Resource status is required")
        OperationalResourceStatus status
) {
}
