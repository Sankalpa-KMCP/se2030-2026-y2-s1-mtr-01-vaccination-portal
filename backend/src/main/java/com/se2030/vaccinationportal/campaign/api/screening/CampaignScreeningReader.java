package com.se2030.vaccinationportal.campaign.api.screening;

import java.time.LocalDate;
import java.util.Optional;

/** Campaign facts required by screening form, submission, and eligibility workflows. */
public interface CampaignScreeningReader {
    CampaignScreeningSnapshot getRequiredCampaign(Long campaignId);

    Optional<QuestionnaireAssignmentSnapshot> getEffectiveAssignmentAt(Long campaignId, LocalDate date);
}
