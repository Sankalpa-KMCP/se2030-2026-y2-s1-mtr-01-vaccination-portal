package com.se2030.vaccinationportal.campaign.center.dto.response;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Detailed view of an OperatingHours schedule record.
 */
public record OperatingHoursResponse(
        Long id,
        Long centerId,
        DayOfWeek dayOfWeek,
        LocalTime opening,
        LocalTime closing,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Instant createdAt,
        Instant updatedAt
) {
}
