package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.SeriesOccurrenceCategory;
import java.time.LocalDate;

/**
 * Detailed preview entry for a single occurrence under a proposed series change.
 */
public record SeriesOccurrencePreviewItemResponse(
        String occurrenceKey,
        LocalDate originOccurrenceDate,
        SeriesOccurrenceCategory category,
        Long sessionId,
        SessionScheduleSnapshotResponse currentSnapshot,
        SessionScheduleSnapshotResponse proposedSnapshot,
        int affectedAppointmentCount,
        int affectedWaitlistCount
) {
}
