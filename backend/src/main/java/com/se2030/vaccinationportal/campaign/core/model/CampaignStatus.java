package com.se2030.vaccinationportal.campaign.core.model;

/**
 * Separate lifecycle status model for vaccination campaigns.
 * Defined by canonical specification WBVP-SE2030 section 6.6 / E.5.
 */
public enum CampaignStatus {
    /**
     * Editable and hidden from citizens.
     */
    DRAFT,

    /**
     * Internally approved with future publication/service dates.
     */
    SCHEDULED,

    /**
     * Citizen-visible; screening and booking depend on configured windows.
     */
    PUBLISHED,

    /**
     * Service period is underway.
     */
    ACTIVE,

    /**
     * Temporarily blocks new screening and booking while preserving records.
     */
    SUSPENDED,

    /**
     * Service work closed; no new bookings; history retained.
     */
    COMPLETED,

    /**
     * Cancelled with recorded reason and impact preview.
     */
    CANCELLED,

    /**
     * Retention policy permits archival; immutable historical record.
     */
    ARCHIVED;

    /**
     * Checks if the campaign status permits active booking/screening intake (MF02-BR-03).
     * Only PUBLISHED and ACTIVE campaigns may accept new bookings/screening.
     *
     * @return true if status is PUBLISHED or ACTIVE
     */
    public boolean isIntakePermitted() {
        return this == PUBLISHED || this == ACTIVE;
    }

    /**
     * Checks whether campaign operations and new intake have permanently closed.
     * COMPLETED and CANCELLED remain eligible for the separate archival transition.
     *
     * @return true if status is COMPLETED, CANCELLED, or ARCHIVED
     */
    public boolean isOperationallyClosed() {
        return this == COMPLETED || this == CANCELLED || this == ARCHIVED;
    }

    /**
     * Checks whether new session planning or recurrence expansion is blocked.
     * New session planning/progression is blocked when status is SUSPENDED, COMPLETED,
     * CANCELLED, or ARCHIVED.
     *
     * @return true if status is SUSPENDED, COMPLETED, CANCELLED, or ARCHIVED
     */
    public boolean isNewSessionPlanningBlocked() {
        return this == SUSPENDED || isOperationallyClosed();
    }

    /**
     * Checks whether the canonical archival transition may originate from this status.
     *
     * @return true if status is COMPLETED or CANCELLED
     */
    public boolean isArchivable() {
        return this == COMPLETED || this == CANCELLED;
    }
}
