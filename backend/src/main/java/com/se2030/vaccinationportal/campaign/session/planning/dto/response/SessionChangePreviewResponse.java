package com.se2030.vaccinationportal.campaign.session.planning.dto.response;

import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeType;

/**
 * Response payload representing a calculated session change preview with cryptographic verification token.
 */
public record SessionChangePreviewResponse(
        Long sessionId,
        SessionChangeType changeType,
        SessionScheduleSnapshotResponse oldSnapshot,
        SessionScheduleSnapshotResponse newSnapshot,
        int affectedAppointmentCount,
        int affectedWaitlistCount,
        String previewToken
) {
}
