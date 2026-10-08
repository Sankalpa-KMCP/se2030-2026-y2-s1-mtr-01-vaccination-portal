package com.se2030.vaccinationportal.campaign.session.recurrence.dto.response;

/**
 * Representation of a series-wide schedule change confirmation result.
 */
public record SeriesChangeResultResponse(
        SessionRecurrenceRuleResponse rule,
        SeriesChangePreviewResponse preview,
        int rescheduledCount,
        int cancelledCount,
        int newCount,
        int preservedCount,
        int ineligibleCount,
        int eventsCreatedCount
) {
}
