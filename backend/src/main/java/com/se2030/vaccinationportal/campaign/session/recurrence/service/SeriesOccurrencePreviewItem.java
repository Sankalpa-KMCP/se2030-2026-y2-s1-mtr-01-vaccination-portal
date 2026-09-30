package com.se2030.vaccinationportal.campaign.session.recurrence.service;

import com.se2030.vaccinationportal.campaign.session.recurrence.model.SeriesOccurrenceCategory;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionScheduleSnapshot;
import java.time.LocalDate;

/**
 * Detailed preview entry for a single session occurrence or candidate date under a series change.
 *
 * @param occurrenceKey             Stable occurrence identity (generationKey:originOccurrenceDate)
 * @param originOccurrenceDate      Local origin calendar date for the occurrence
 * @param category                  Occurrence classification under the proposed series definition
 * @param sessionId                 ID of the existing VaccinationSession entity, or null if NEW
 * @param currentSnapshot           Snapshot of current session schedule/status, or null if NEW
 * @param proposedSnapshot          Snapshot of proposed session schedule/status, or null/unchanged if not mutated
 * @param affectedAppointmentCount  Count of affected confirmed appointments on this occurrence
 * @param affectedWaitlistCount     Count of affected waitlist entries on this occurrence
 */
public record SeriesOccurrencePreviewItem(
        String occurrenceKey,
        LocalDate originOccurrenceDate,
        SeriesOccurrenceCategory category,
        Long sessionId,
        SessionScheduleSnapshot currentSnapshot,
        SessionScheduleSnapshot proposedSnapshot,
        int affectedAppointmentCount,
        int affectedWaitlistCount
) {
    public SeriesOccurrencePreviewItem {
        if (occurrenceKey == null || occurrenceKey.isBlank()) {
            throw new IllegalArgumentException("Occurrence key is required");
        }
        if (originOccurrenceDate == null) {
            throw new IllegalArgumentException("Origin occurrence date is required");
        }
        if (category == null) {
            throw new IllegalArgumentException("Series occurrence category is required");
        }
        if (affectedAppointmentCount < 0) {
            throw new IllegalArgumentException("Affected appointment count cannot be negative");
        }
        if (affectedWaitlistCount < 0) {
            throw new IllegalArgumentException("Affected waitlist count cannot be negative");
        }
    }
}
