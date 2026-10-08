package com.se2030.vaccinationportal.campaign.center.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating mutable attributes of an existing Vaccination Center.
 */
public record UpdateCenterRequest(
        @NotBlank(message = "Center name is required")
        @Size(max = 128, message = "Center name must not exceed 128 characters")
        String name,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @NotBlank(message = "Contact information is required")
        @Size(max = 128, message = "Contact information must not exceed 128 characters")
        String contact,

        @NotNull(message = "Safe concurrent occupancy is required")
        @Positive(message = "Safe concurrent occupancy must be a positive integer")
        Integer safeConcurrentOccupancy,

        @Positive(message = "Session service ceiling must be a positive integer when specified")
        Integer sessionServiceCeiling
) {
}
