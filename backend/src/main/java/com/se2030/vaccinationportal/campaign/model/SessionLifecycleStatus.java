package com.se2030.vaccinationportal.campaign.model;

import java.util.EnumSet;

/**
 * Authoritative lifecycle status of a concrete vaccination session.
 */
public enum SessionLifecycleStatus {
    DRAFT,
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    /**
     * Session states whose future or live assignments remain operational dependencies.
     */
    public static EnumSet<SessionLifecycleStatus> blockingOperationalDependencyStatuses() {
        return EnumSet.of(DRAFT, SCHEDULED, IN_PROGRESS);
    }
}
