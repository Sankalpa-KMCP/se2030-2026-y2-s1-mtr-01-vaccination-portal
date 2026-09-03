package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.SessionBookingStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import java.time.Instant;

/**
 * Compact summary view of a VaccinationSession for collection and list endpoints.
 */
public record SessionSummaryResponse(
        Long id,
        Long campaignId,
        Long centerId,
        Instant startAt,
        Instant endAt,
        Instant bookingCloseAt,
        String scheduleTimezone,
        SessionLifecycleStatus lifecycleStatus,
        boolean generated,
        SessionBookingStatus bookingStatus,
        boolean bookingSuspended
) {
}
