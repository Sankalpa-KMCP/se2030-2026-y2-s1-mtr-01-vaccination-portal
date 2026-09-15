package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new TargetGroup.
 */
public record CreateTargetGroupRequest(
        @NotBlank(message = "Target group code is required")
        @Size(max = 64, message = "Target group code must not exceed 64 characters")
        String code,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}
