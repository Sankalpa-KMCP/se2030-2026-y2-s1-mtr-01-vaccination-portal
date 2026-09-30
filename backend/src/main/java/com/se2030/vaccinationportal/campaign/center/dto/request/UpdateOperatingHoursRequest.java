package com.se2030.vaccinationportal.campaign.center.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request payload for updating an existing operating hours schedule shift.
 */
public record UpdateOperatingHoursRequest(
        @NotNull(message = "Day of week is required")
        DayOfWeek dayOfWeek,

        @NotNull(message = "Opening time is required")
        LocalTime opening,

        @NotNull(message = "Closing time is required")
        LocalTime closing,

        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom,

        LocalDate effectiveTo
) {
}
