package com.se2030.vaccinationportal.appointment.dependency;

/**
 * Cross-module integration port to read a citizen's current campaign-specific screening eligibility assessment (MF03-BR-01).
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
