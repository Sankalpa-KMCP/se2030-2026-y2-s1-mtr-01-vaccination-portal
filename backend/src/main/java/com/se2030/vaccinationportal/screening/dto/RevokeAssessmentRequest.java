package com.se2030.vaccinationportal.screening.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RevokeAssessmentRequest(
        @NotNull(message = "Reviewer ID is required")
        Long reviewerId,

        @NotBlank(message = "Revocation reason is required")
        String revocationReason
) {}
