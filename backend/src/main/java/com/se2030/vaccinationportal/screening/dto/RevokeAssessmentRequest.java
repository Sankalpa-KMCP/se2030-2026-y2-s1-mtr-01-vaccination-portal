package com.se2030.vaccinationportal.screening.dto;

import jakarta.validation.constraints.NotBlank;

public record RevokeAssessmentRequest(
        @NotBlank(message = "Revocation reason is required")
        String revocationReason
) {}
