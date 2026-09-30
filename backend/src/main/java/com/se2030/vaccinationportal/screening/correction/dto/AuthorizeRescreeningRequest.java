package com.se2030.vaccinationportal.screening.correction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthorizeRescreeningRequest(
        @NotNull(message = "Reviewer ID is required")
        Long authorizedByReviewerId,

        @NotBlank(message = "Rescreening authorization reason is required")
        String reason
) {}
