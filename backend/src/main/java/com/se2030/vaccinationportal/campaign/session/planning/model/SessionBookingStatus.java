package com.se2030.vaccinationportal.campaign.session.planning.model;

/**
 * Derived booking projection status of a vaccination session.
 * This is a read-model projection concept rather than an independently writable state.
 */
public enum SessionBookingStatus {
    NOT_OPEN,
    OPEN,
    FULL,
    CLOSED,
    SUSPENDED
}
