package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.SessionBookingStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Detailed view of a VaccinationSession domain entity including authoritative booking status.
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
        SessionBookingStatus bookingStatus,
        boolean bookingSuspended,
        Instant createdAt,
        Instant updatedAt
) {
}
