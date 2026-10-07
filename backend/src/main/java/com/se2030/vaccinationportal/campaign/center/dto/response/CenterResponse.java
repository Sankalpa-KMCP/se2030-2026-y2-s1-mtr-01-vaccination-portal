package com.se2030.vaccinationportal.campaign.center.dto.response;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import java.time.Instant;

/**
 * Detailed view of a VaccinationCenter domain entity.
 */
public record CenterResponse(
        Long id,
        String code,
        String name,
        String address,
        String contact,
        String timezone,
        CenterStatus status,
        Integer safeConcurrentOccupancy,
        Integer sessionServiceCeiling,
        Instant createdAt,
        Instant updatedAt
) {
}
