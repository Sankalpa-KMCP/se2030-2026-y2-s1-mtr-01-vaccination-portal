package com.se2030.vaccinationportal.campaign.model;

/**
 * Categorizes session occurrences during series-wide recurrence changes.
 */
public enum SeriesOccurrenceCategory {
    /**
     * Eligible existing session whose origin date matches the new recurrence pattern.
     * Schedule is updated if times change; bookingCloseAt is set to new startAt.
     */
    RETAINED,

    /**
     * Eligible existing session whose origin date no longer matches the new recurrence pattern.
     * Session will be cancelled, preserving history and provenance; never hard-deleted.
     */
    REMOVED,

    /**
     * Newly introduced recurrence date matching the new recurrence pattern with no existing session.
     * Will be materialized as a new DRAFT session.
     */
    NEW,

    /**
     * Existing session that was individually modified (rescheduled, center-transferred, or cancelled).
     * Preserved unchanged on its individual schedule/center and excluded from series mutations.
     */
    PRESERVED_EXCEPTION,

    /**
     * Existing session that is elapsed (past) or in an ineligible lifecycle status (IN_PROGRESS, COMPLETED, or existing CANCELLED).
     * Excluded from series mutations and preserved unchanged.
     */
    INELIGIBLE
}
