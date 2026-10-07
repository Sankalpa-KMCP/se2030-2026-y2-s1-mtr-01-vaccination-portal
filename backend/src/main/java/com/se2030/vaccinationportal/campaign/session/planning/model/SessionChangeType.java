package com.se2030.vaccinationportal.campaign.session.planning.model;

/**
 * Enumerates types of material operational changes applied to a vaccination session.
 */
public enum SessionChangeType {
    RESCHEDULED,
    CENTER_TRANSFERRED,
    CANCELLED,
    SERIES_RESCHEDULED,
    SERIES_CANCELLED,
    BOOKING_SUSPENDED,
    BOOKING_RESUMED
}
