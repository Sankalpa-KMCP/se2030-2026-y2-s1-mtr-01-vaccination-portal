package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating mutable attributes of an existing VaccineType.
 */
public record UpdateVaccineTypeRequest(
        @NotBlank(message = "Vaccine type name is required")
        @Size(max = 128, message = "Vaccine type name must not exceed 128 characters")
        String name,

        @Size(max = 32, message = "Dose unit must not exceed 32 characters")
        String doseUnit
) {
}
