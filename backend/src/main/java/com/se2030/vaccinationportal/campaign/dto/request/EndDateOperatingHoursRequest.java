package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request payload for ending or shortening the effective validity of an operating hours schedule shift.
 */
public record EndDateOperatingHoursRequest(
        @NotNull(message = "End date is required")
        LocalDate endDate
) {
}
