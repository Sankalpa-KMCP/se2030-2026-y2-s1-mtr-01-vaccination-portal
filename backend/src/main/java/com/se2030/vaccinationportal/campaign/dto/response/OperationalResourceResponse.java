package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import java.time.Instant;

/**
 * Public REST response DTO representing an operational resource under a vaccination center.
 */
public record OperationalResourceResponse(
        Long id,
        Long centerId,
        String code,
        ResourceType resourceType,
        String name,
        OperationalResourceStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
