package com.se2030.vaccinationportal.vaccination.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for appending a correction to finalized vaccination history. */
public record CreateVaccinationCorrectionRequest(
        @NotBlank(message = "Correction field is required")
        @Size(max = 64, message = "Correction field must not exceed 64 characters")
        String fieldName,

        @Size(max = 2000, message = "New value must not exceed 2000 characters")
        String newValue,

        @NotBlank(message = "Correction reason is required")
        @Size(max = 1000, message = "Correction reason must not exceed 1000 characters")
        String reason,

        Long correctedBy
) {
    public CreateVaccinationCorrectionRequest(String fieldName, String newValue, String reason) {
        this(fieldName, newValue, reason, null);
    }
}
