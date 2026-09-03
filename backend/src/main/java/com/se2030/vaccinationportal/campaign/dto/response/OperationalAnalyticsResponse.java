package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import java.time.Instant;
import java.util.Map;

/**
 * Reconciled descriptive operational analytics response for MF-02.
 * Canonical PB-10D, UC-10D, and Rule MF02-BR-19.
 */
public record OperationalAnalyticsResponse(
        Map<CampaignStatus, Long> campaignCountByStatus,
        Map<SessionLifecycleStatus, Long> sessionCountByLifecycleStatus,
        long totalApprovedCapacity,
        long totalRecommendedThroughput,
        long totalConfiguredSlotCapacity,
        long totalReservedSlots,
        long totalRemainingSlotCapacity,
        double slotUtilizationPercentage,
        long cancelledSessionCount,
        long cancelledCampaignCount,
        long assignedStationCount,
        long assignedVaccinatorCount,
        Map<ResourceType, Long> assignedResourceCountsByType,
        Instant calculatedAt
) {}
