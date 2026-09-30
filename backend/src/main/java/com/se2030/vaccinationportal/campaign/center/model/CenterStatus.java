package com.se2030.vaccinationportal.campaign.center.model;

/**
 * Lifecycle status model for physical vaccination centers.
 * Defined by canonical specification WBVP-SE2030 section 6.6 / 12 (Table 12.1).
 *
 * <p>Canonical Rule:</p>
 * <ul>
 *   <li>Contains exactly two values: {@code ACTIVE} and {@code INACTIVE}.</li>
 *   <li>Temporary closures (e.g. public holidays, facility repairs) must NOT be represented
 *       by changing {@code CenterStatus}; temporary unavailability belongs to {@code CenterClosure}.</li>
 * </ul>
 */
public enum CenterStatus {
    /**
     * Center is active and available for operational planning, session hosting, and appointments.
     */
    ACTIVE,

    /**
     * Center is deactivated or decommissioned from operational planning.
     */
    INACTIVE
}
