package com.se2030.vaccinationportal.screening.integration;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityLookupPort;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.screening.service.EligibilityReviewService;
import org.springframework.stereotype.Component;

/**
 * Exposes MF-01's current campaign eligibility decision through MF-03's lookup port.
 */
@Component
public class Mf01EligibilityLookupAdapter implements EligibilityLookupPort {

    private final EligibilityReviewService eligibilityReviewService;

    public Mf01EligibilityLookupAdapter(EligibilityReviewService eligibilityReviewService) {
        this.eligibilityReviewService = eligibilityReviewService;
    }

    @Override
    public EligibilityStatus getCurrentEligibility(Long citizenId, Long campaignId) {
        return eligibilityReviewService.isCitizenCurrentlyEligible(citizenId, campaignId)
                ? EligibilityStatus.ELIGIBLE
                : EligibilityStatus.NOT_ELIGIBLE;
    }
}
