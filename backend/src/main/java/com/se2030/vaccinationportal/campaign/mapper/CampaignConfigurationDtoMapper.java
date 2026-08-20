package com.se2030.vaccinationportal.campaign.mapper;

import com.se2030.vaccinationportal.campaign.dto.response.CampaignCenterAssignmentResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CampaignDoseProgrammeResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CampaignLocationTargetResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CampaignQuestionnaireAssignmentResponse;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignLocationTarget;
import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;

/**
 * Pure transformation utility mapping campaign configuration domain models to REST response DTOs.
 */
public final class CampaignConfigurationDtoMapper {

    private CampaignConfigurationDtoMapper() {
    }

    public static CampaignDoseProgrammeResponse toResponse(CampaignDoseProgramme programme) {
        if (programme == null) {
            return null;
        }
        Long campaignId = programme.getCampaign() != null ? programme.getCampaign().getId() : null;
        return new CampaignDoseProgrammeResponse(
                programme.getId(),
                campaignId,
                programme.getVersionNumber(),
                programme.getDoseCategory(),
                programme.getPermittedDoseNumbers(),
                programme.getMinimumIntervalDays(),
                programme.isPreviousEvidenceRequired(),
                programme.getExpectedServiceMinutes(),
                programme.getActiveFrom(),
                programme.getActiveTo()
        );
    }

    public static CampaignQuestionnaireAssignmentResponse toResponse(CampaignQuestionnaireAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        Long campaignId = assignment.getCampaign() != null ? assignment.getCampaign().getId() : null;
        return new CampaignQuestionnaireAssignmentResponse(
                assignment.getId(),
                campaignId,
                assignment.getQuestionnaireVersionId(),
                assignment.getActiveFrom(),
                assignment.getActiveTo()
        );
    }

    public static CampaignCenterAssignmentResponse toResponse(CampaignCenterAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        Long campaignId = assignment.getCampaign() != null ? assignment.getCampaign().getId() : null;
        Long centerId = assignment.getCenter() != null ? assignment.getCenter().getId() : null;
        String centerCode = assignment.getCenter() != null ? assignment.getCenter().getCode() : null;
        String centerName = assignment.getCenter() != null ? assignment.getCenter().getName() : null;
        return new CampaignCenterAssignmentResponse(
                assignment.getId(),
                campaignId,
                centerId,
                centerCode,
                centerName,
                assignment.getStatus(),
                assignment.getActiveFrom(),
                assignment.getActiveTo()
        );
    }

    public static CampaignLocationTargetResponse toResponse(CampaignLocationTarget target) {
        if (target == null) {
            return null;
        }
        Long campaignId = target.getCampaign() != null ? target.getCampaign().getId() : null;
        return new CampaignLocationTargetResponse(
                target.getId(),
                campaignId,
                target.getTargetType(),
                target.getTargetCode()
        );
    }
}
