package com.se2030.vaccinationportal.appointment.dependency;

/**
 * Cross-module integration port to read a citizen's current campaign-specific screening eligibility assessment (MF03-BR-01, MF03-BR-07).
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Gating initial appointment booking against verified screening assessment.</li>
 *   <li>US-05: Re-verifying eligibility before promoting a queued citizen from the waitlist.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-01: Booking requires a currently valid ELIGIBLE assessment for the same campaign.</li>
 *   <li>MF03-BR-07: Promotion re-checks screening eligibility to ensure medical assessment remains valid.</li>
 * </ul>
 *
 * <p>
 * Implemented by MF-01 (Screening &amp; Eligibility). MF-03 must not implement eligibility logic itself.
 * </p>
 */
public interface EligibilityLookupPort {

    /**
     * Reads a citizen's current campaign-specific screening eligibility status.
     *
     * @param citizenId  Identifier of the citizen attempting to book
     * @param campaignId Identifier of the target vaccination campaign
     * @return Current {@link EligibilityStatus} assessment from MF-01
     */
    EligibilityStatus getCurrentEligibility(Long citizenId, Long campaignId);
}
