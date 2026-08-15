package com.se2030.vaccinationportal.screening.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestCorrectionsRequest(
        @NotBlank(message = "Correction instructions are required")
        String instructions
) {}
