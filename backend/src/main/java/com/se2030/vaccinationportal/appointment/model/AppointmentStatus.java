package com.se2030.vaccinationportal.appointment.model;

/**
 * Canonical appointment status enumeration.
 * Canonical section 7.4: CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW.
 * Separate status model - do not reuse for WaitlistEntry or CheckInEvent.
 */
public enum AppointmentStatus {
    CONFIRMED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
