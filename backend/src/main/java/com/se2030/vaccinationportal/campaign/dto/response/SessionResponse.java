package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Detailed view of a VaccinationSession domain entity.
 */
public record SessionResponse(
        Long id,
        Long campaignId,
        Long centerId,
        Long recurrenceRuleId,
        String occurrenceKey,
        LocalDate originOccurrenceDate,
        Instant startAt,
        Instant endAt,
        Instant bookingCloseAt,
        String scheduleTimezone,
        SessionLifecycleStatus lifecycleStatus,
        boolean generated,
        Instant createdAt,
        Instant updatedAt
) {
}
