package com.se2030.vaccinationportal.campaign.session.recurrence.dto.request;

import java.time.LocalDate;

/**
 * Request payload for materializing session occurrences within an optional date window.
 */
public record GenerateOccurrencesRequest(
        LocalDate requestedStartDate,
        LocalDate requestedEndDate
) {
}
