package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRuleStatus;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Detailed view of a SessionRecurrenceRule domain entity.
 */
public record SessionRecurrenceRuleResponse(
        Long id,
        Long campaignId,
        Long centerId,
        Set<DayOfWeek> weekdays,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        String timezone,
        SessionRecurrenceRuleStatus status,
        String generationKey,
        Instant createdAt,
        Instant updatedAt
) {
}
