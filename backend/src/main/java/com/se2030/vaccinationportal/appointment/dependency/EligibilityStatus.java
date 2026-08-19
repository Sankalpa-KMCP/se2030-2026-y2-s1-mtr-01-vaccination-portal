package com.se2030.vaccinationportal.appointment.dependency;

/**
 * Result enum representing a citizen's campaign-specific screening eligibility status (MF03-BR-01).
 * <p>
 * Evaluation values:
 * <ul>
 *   <li>{@link #ELIGIBLE}: Citizen has a valid, active ELIGIBLE screening assessment for the campaign.</li>
 *   <li>{@link #NOT_ELIGIBLE}: Citizen was assessed as NOT ELIGIBLE for the campaign.</li>
 *   <li>{@link #EXPIRED}: Citizen's previous assessment has expired.</li>
 *   <li>{@link #NO_ASSESSMENT}: Citizen has no recorded screening assessment for the campaign.</li>
 * </ul>
 * </p>
 */
public enum EligibilityStatus {
    ELIGIBLE,
    NOT_ELIGIBLE,
    EXPIRED,
    NO_ASSESSMENT
}
