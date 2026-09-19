package com.se2030.vaccinationportal.vaccination.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload for approving or rejecting a pending vaccination correction.
 */
public record ReviewVaccinationCorrectionRequest(
        @Size(max = 1000, message = "Review reason must not exceed 1000 characters")
        String reason
) {
}
