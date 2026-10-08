package com.se2030.vaccinationportal.campaign.session.recurrence.dto.response;

import com.se2030.vaccinationportal.campaign.session.planning.dto.response.SessionScheduleSnapshotResponse;


import com.se2030.vaccinationportal.campaign.session.recurrence.model.SeriesOccurrenceCategory;
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
