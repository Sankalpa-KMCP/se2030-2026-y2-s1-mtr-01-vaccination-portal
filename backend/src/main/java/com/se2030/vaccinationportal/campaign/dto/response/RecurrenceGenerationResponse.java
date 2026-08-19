package com.se2030.vaccinationportal.campaign.dto.response;

import java.util.List;

/**
 * Representation of the result of generating concrete session occurrences from a recurrence rule.
 */
public record RecurrenceGenerationResponse(
        int createdCount,
        int existingCount,
        List<SessionSummaryResponse> sessions
) {
}
