package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Detailed view of a VaccinationCampaign domain entity.
 */
public record CampaignResponse(
        Long id,
        String code,
        String name,
        String description,
        VaccineTypeSummaryResponse vaccineType,
        TargetGroupSummaryResponse targetGroup,
        LocalDate serviceStartDate,
        LocalDate serviceEndDate,
        Instant bookingOpenAt,
        Instant bookingCloseAt,
        CampaignStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
