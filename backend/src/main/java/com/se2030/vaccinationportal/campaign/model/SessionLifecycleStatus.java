package com.se2030.vaccinationportal.campaign.model;

/**
 * Authoritative lifecycle status of a concrete vaccination session.
 */
public enum SessionLifecycleStatus {
    DRAFT,
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
