package com.se2030.vaccinationportal.campaign.reference.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating mutable attributes of an existing TargetGroup.
 */
public record UpdateTargetGroupRequest(
        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}
