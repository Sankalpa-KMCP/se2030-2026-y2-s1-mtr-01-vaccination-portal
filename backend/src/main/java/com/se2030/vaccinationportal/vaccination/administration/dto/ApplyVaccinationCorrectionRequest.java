package com.se2030.vaccinationportal.vaccination.administration.dto;

import jakarta.validation.constraints.Size;

/**
 * Payload submitted by an authorized Vaccinator when applying an approved correction.
 */
public record ApplyVaccinationCorrectionRequest(
        @Size(max = 64, message = "Correction field must not exceed 64 characters")
        String fieldName,

        @Size(max = 2000, message = "New value must not exceed 2000 characters")
        String newValue
) {
    public ApplyVaccinationCorrectionRequest() {
        this(null, null);
    }
}
