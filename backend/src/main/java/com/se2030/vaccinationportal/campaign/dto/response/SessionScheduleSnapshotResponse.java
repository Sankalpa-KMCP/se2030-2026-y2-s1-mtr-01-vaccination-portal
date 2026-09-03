package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import java.time.Instant;

/**
 * Response projection of an immutable SessionScheduleSnapshot.
 */
public record SessionScheduleSnapshotResponse(
        Long centerId,
        String scheduleTimezone,
        Instant startAt,
        Instant endAt,
        Instant bookingCloseAt,
        SessionLifecycleStatus lifecycleStatus
) {
}
