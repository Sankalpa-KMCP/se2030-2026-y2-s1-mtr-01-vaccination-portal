package com.se2030.vaccinationportal.campaign.questionnaire.dto.response;

import java.time.LocalDate;

/**
 * Response DTO representing a CampaignQuestionnaireAssignment entity.
 */
public record CampaignQuestionnaireAssignmentResponse(
        Long id,
        Long campaignId,
        Long questionnaireVersionId,
        LocalDate activeFrom,
        LocalDate activeTo
) {}
