package com.se2030.vaccinationportal.campaign.core.dto.response;

import com.se2030.vaccinationportal.campaign.reference.dto.response.TargetGroupSummaryResponse;
import com.se2030.vaccinationportal.campaign.reference.dto.response.VaccineTypeSummaryResponse;


import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
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
        Instant updatedAt,
        Long questionnaireVersionId
) {
    public CampaignResponse(
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
        this(id, code, name, description, vaccineType, targetGroup, serviceStartDate, serviceEndDate, bookingOpenAt, bookingCloseAt, status, createdAt, updatedAt, null);
    }
}
