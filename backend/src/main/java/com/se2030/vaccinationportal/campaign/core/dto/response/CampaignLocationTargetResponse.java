package com.se2030.vaccinationportal.campaign.core.dto.response;

import com.se2030.vaccinationportal.campaign.core.model.LocationTargetType;

/**
 * Response DTO representing a CampaignLocationTarget entity.
 */
public record CampaignLocationTargetResponse(
        Long id,
        Long campaignId,
        LocationTargetType targetType,
        String targetCode
) {}
