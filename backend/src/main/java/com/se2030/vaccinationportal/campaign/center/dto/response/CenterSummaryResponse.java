package com.se2030.vaccinationportal.campaign.center.dto.response;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;

/**
 * Summary view of a VaccinationCenter domain entity.
 */
public record CenterSummaryResponse(
        Long id,
        String code,
        String name,
        CenterStatus status,
        String timezone
) {
}
