package com.se2030.vaccinationportal.vaccination.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Optional JSON payload for deleting an unfinalized vaccination draft with a non-blank audit reason.
 */
public record DeleteVaccinationDraftRequest(
        @NotBlank(message = "Draft deletion reason is required")
        @Size(max = 1000, message = "Draft deletion reason must not exceed 1000 characters")
        String reason
) {
}
