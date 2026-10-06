package com.se2030.vaccinationportal.campaign.core.dto.response;

import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import java.time.LocalDate;

/**
 * Minimal projection for authenticated citizen campaign discovery (MF02-R-001).
 * Excludes internal planning, audit, booking window timestamps, and master-data internals.
 */
public record CitizenCampaignDiscoveryResponse(
        Long id,
        String code,
        String name,
        String vaccineTypeCode,
        String targetGroupCode,
        LocalDate serviceStartDate,
        LocalDate serviceEndDate,
        CampaignStatus status,
        boolean currentlyAcceptingScreening
) {
}
