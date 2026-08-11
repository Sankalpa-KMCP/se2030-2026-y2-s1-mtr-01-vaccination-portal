package com.se2030.vaccinationportal.campaign.service;

/**
 * Immutable summary of citizen appointments and waitlist entries affected by a session change.
 */
public record SessionImpactSummary(
        int affectedAppointmentCount,
        int affectedWaitlistCount
) {
    public SessionImpactSummary {
        if (affectedAppointmentCount < 0) {
            throw new IllegalArgumentException("Affected appointment count cannot be negative");
        }
        if (affectedWaitlistCount < 0) {
            throw new IllegalArgumentException("Affected waitlist count cannot be negative");
        }
    }
}
