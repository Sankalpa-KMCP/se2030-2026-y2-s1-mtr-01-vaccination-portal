package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request payload for updating the active period of a future campaign-center assignment.
 */
public record UpdateCenterAssignmentRequest(
        @NotNull(message = "Active from date is required")
        LocalDate activeFrom,

        LocalDate activeTo
) {}
