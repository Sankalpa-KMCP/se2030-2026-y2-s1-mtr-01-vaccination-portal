package com.se2030.vaccinationportal.screening.correction.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestCorrectionsRequest(
        @NotBlank(message = "Correction instructions are required")
        String instructions
) {}
