package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.SessionChangeType;
import java.time.Instant;

/**
 * Response view of an append-only SessionChangeEvent audit record.
 */
public record SessionChangeEventResponse(
        Long id,
        Long sessionId,
        SessionChangeType changeType,
        SessionScheduleSnapshotResponse oldSnapshot,
        SessionScheduleSnapshotResponse newSnapshot,
        String reason,
        int affectedAppointmentCount,
        int affectedWaitlistCount,
        String actorId,
        Instant createdAt
) {
}
