package com.se2030.vaccinationportal.campaign.center.dto.response;

import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignmentStatus;
import java.time.LocalDate;

/**
 * Response DTO representing a CampaignCenterAssignment entity.
 */
public record CampaignCenterAssignmentResponse(
        Long id,
        Long campaignId,
        Long centerId,
        String centerCode,
        String centerName,
        CampaignCenterAssignmentStatus status,
        LocalDate activeFrom,
        LocalDate activeTo
) {}
