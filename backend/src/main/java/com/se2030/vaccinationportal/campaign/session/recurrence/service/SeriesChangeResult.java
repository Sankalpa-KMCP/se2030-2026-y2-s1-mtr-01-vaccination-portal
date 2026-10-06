package com.se2030.vaccinationportal.campaign.session.recurrence.service;

import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRule;

/**
 * Immutable summary result of an atomic series-wide change confirmation operation.
 *
 * @param rule                Updated {@link SessionRecurrenceRule} entity
 * @param preview             Calculated {@link SeriesChangePreview} used for confirmation
 * @param rescheduledCount    Number of retained sessions whose schedule was updated
 * @param cancelledCount      Number of removed sessions transitioned to CANCELLED
 * @param newCount            Number of new DRAFT sessions created
 * @param preservedCount      Number of individual override sessions preserved as exceptions
 * @param ineligibleCount     Number of past/terminal sessions kept as ineligible
 * @param eventsCreatedCount  Total number of append-only {@link com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeEvent} audit records persisted
 */
public record SeriesChangeResult(
        SessionRecurrenceRule rule,
        SeriesChangePreview preview,
        int rescheduledCount,
        int cancelledCount,
        int newCount,
        int preservedCount,
        int ineligibleCount,
        int eventsCreatedCount
) {}
