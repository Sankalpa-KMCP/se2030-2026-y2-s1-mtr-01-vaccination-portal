package com.se2030.vaccinationportal.campaign.session.recurrence.dto.response;

import com.se2030.vaccinationportal.campaign.session.planning.dto.response.SessionSummaryResponse;


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
