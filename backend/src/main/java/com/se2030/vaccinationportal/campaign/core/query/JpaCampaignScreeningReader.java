package com.se2030.vaccinationportal.campaign.core.query;

import com.se2030.vaccinationportal.campaign.api.screening.CampaignScreeningReader;
import com.se2030.vaccinationportal.campaign.api.screening.CampaignScreeningSnapshot;
import com.se2030.vaccinationportal.campaign.api.screening.QuestionnaireAssignmentSnapshot;


import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.questionnaire.repository.CampaignQuestionnaireAssignmentRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaCampaignScreeningReader implements CampaignScreeningReader {
    private final VaccinationCampaignRepository campaigns;
    private final CampaignQuestionnaireAssignmentRepository assignments;

    public JpaCampaignScreeningReader(VaccinationCampaignRepository campaigns,
            CampaignQuestionnaireAssignmentRepository assignments) {
        this.campaigns = campaigns;
        this.assignments = assignments;
    }

    @Override
    public CampaignScreeningSnapshot getRequiredCampaign(Long campaignId) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        var campaign = campaigns.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));
        return new CampaignScreeningSnapshot(campaign.getId(), campaign.getServiceEndDate(),
                campaign.getBookingOpenAt(), campaign.getBookingCloseAt(),
                campaign.getStatus() != null && campaign.getStatus().isIntakePermitted());
    }

    @Override
    public Optional<QuestionnaireAssignmentSnapshot> getEffectiveAssignmentAt(Long campaignId, LocalDate date) {
        if (campaignId == null || date == null) {
            return Optional.empty();
        }
        return assignments.findEffectiveAssignmentAt(campaignId, date)
                .map(assignment -> new QuestionnaireAssignmentSnapshot(assignment.getId(),
                        campaignId, assignment.getQuestionnaireVersionId()));
    }
}
