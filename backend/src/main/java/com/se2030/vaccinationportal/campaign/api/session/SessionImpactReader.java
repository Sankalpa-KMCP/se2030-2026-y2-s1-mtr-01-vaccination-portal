package com.se2030.vaccinationportal.campaign.api.session;

/**
 * Cross-module integration port (MF-02 -> MF-03) used to read citizen appointment and waitlist impact
 * for vaccination sessions undergoing modification or cancellation.
 *
 * <p>Canonical Architecture Boundaries (WBVP-SE2030 Section 2, 5, 12):</p>
 * <ul>
 *   <li>MF-03 owns citizen bookings, appointments, and waitlists.</li>
 *   <li>MF-02 owns campaigns, centers, and concrete vaccination sessions.</li>
 *   <li>MF-02 must never duplicate MF-03 appointment/waitlist entities or queries.</li>
 *   <li>MF-03 provides the production adapter implementing this reader.</li>
 *   <li>No production component may return fake zero counts for scheduled sessions.</li>
 * </ul>
 */
public interface SessionImpactReader {

    /**
     * Retrieves the citizen appointment and waitlist impact for the specified vaccination session.
     *
     * @param sessionId the unique identifier of the vaccination session
     * @return the impact summary containing affected appointment and waitlist counts
     */
    SessionImpactSummary getSessionImpact(Long sessionId);
}
