package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Request payload for creating a campaign-center assignment.
 */
public record CreateCenterAssignmentRequest(
        @NotNull(message = "Center ID is required")
        @Positive(message = "Center ID must be positive")
        Long centerId,

        @NotNull(message = "Active from date is required")
        LocalDate activeFrom,

        LocalDate activeTo
) {}
