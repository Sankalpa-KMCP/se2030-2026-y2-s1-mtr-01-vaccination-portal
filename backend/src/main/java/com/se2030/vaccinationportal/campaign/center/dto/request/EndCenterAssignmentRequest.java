package com.se2030.vaccinationportal.campaign.center.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request payload for early-ending an effective campaign-center assignment.
 */
public record EndCenterAssignmentRequest(
        @NotNull(message = "Active to date is required")
        LocalDate activeTo
) {}
