package com.se2030.vaccinationportal.campaign.core.dto.response;

import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleChangeType;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import java.time.Instant;

/**
 * Audit view of an append-only CampaignLifecycleEvent record.
 */
public record CampaignLifecycleEventResponse(
        Long id,
        Long campaignId,
        CampaignLifecycleChangeType changeType,
        CampaignStatus previousStatus,
        CampaignStatus newStatus,
        String reason,
        String actorId,
        Instant createdAt
) {
}
