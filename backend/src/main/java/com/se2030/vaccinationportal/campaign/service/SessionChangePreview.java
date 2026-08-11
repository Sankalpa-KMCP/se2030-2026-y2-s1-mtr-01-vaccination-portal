package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.model.SessionScheduleSnapshot;

/**
 * Immutable preview of a proposed vaccination session schedule change, center transfer, or cancellation.
 * Contains the current authoritative snapshot, proposed new snapshot, cross-module citizen impact counts,
 * and a cryptographic preview token used for stale-preview protection at confirmation time.
 */
public record SessionChangePreview(
        Long sessionId,
        SessionChangeType changeType,
        SessionScheduleSnapshot oldSnapshot,
        SessionScheduleSnapshot newSnapshot,
        int affectedAppointmentCount,
        int affectedWaitlistCount,
        String previewToken
) {
    public SessionChangePreview {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }
        if (changeType == null) {
            throw new IllegalArgumentException("Change type is required");
        }
        if (oldSnapshot == null) {
            throw new IllegalArgumentException("Old snapshot is required");
        }
        if (newSnapshot == null) {
            throw new IllegalArgumentException("New snapshot is required");
        }
        if (affectedAppointmentCount < 0) {
            throw new IllegalArgumentException("Affected appointment count cannot be negative");
        }
        if (affectedWaitlistCount < 0) {
            throw new IllegalArgumentException("Affected waitlist count cannot be negative");
        }
        if (previewToken == null || previewToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Preview token is required");
        }
    }
}
