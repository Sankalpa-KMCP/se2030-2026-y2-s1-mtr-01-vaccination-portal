package com.se2030.vaccinationportal.campaign.reference.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new VaccineType.
 */
public record CreateVaccineTypeRequest(
        @NotBlank(message = "Vaccine type code is required")
        @Size(max = 64, message = "Vaccine type code must not exceed 64 characters")
        String code,

        @NotBlank(message = "Vaccine type name is required")
        @Size(max = 128, message = "Vaccine type name must not exceed 128 characters")
        String name,

        @Size(max = 32, message = "Dose unit must not exceed 32 characters")
        String doseUnit
) {
}
