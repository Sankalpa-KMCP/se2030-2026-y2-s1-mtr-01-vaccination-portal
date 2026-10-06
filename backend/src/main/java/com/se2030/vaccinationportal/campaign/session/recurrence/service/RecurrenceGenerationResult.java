package com.se2030.vaccinationportal.campaign.session.recurrence.service;

import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic result record representing the outcome of a recurrence occurrence generation request.
 * Contains the count of newly created sessions, previously existing sessions, and the full
 * materialized list in ascending origin occurrence date order.
 */
public record RecurrenceGenerationResult(
        int createdCount,
        int existingCount,
        List<VaccinationSession> sessions
) {
    public RecurrenceGenerationResult {
        if (createdCount < 0) {
            throw new IllegalArgumentException("Created count cannot be negative");
        }
        if (existingCount < 0) {
            throw new IllegalArgumentException("Existing count cannot be negative");
        }
        sessions = sessions != null ? Collections.unmodifiableList(sessions) : List.of();
    }
}
