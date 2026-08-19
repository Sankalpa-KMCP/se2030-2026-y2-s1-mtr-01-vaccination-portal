package com.se2030.vaccinationportal.campaign.dto.response;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Representation of an immutable recurrence rule schedule definition snapshot.
 */
public record RecurrenceRuleDefinitionSnapshotResponse(
        Set<DayOfWeek> weekdays,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        String timezone
) {
}
