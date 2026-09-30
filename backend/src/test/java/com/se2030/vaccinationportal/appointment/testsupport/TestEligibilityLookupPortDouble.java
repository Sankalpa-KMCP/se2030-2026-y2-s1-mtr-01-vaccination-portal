package com.se2030.vaccinationportal.appointment.testsupport;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityLookupPort;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Stands in for MF-01/MF-02 until their real port implementations exist.
 * Contains no real business logic — do not reuse in production code.
 */
public class TestEligibilityLookupPortDouble implements EligibilityLookupPort {

    private EligibilityStatus defaultStatus = EligibilityStatus.ELIGIBLE;
    private final ConcurrentMap<String, EligibilityStatus> citizenCampaignStatusMap = new ConcurrentHashMap<>();

    public void setDefaultStatus(EligibilityStatus defaultStatus) {
        this.defaultStatus = defaultStatus;
    }

    public void setEligibility(Long citizenId, Long campaignId, EligibilityStatus status) {
        citizenCampaignStatusMap.put(citizenId + ":" + campaignId, status);
    }

    @Override
    public EligibilityStatus getCurrentEligibility(Long citizenId, Long campaignId) {
        return citizenCampaignStatusMap.getOrDefault(citizenId + ":" + campaignId, defaultStatus);
    }
}
