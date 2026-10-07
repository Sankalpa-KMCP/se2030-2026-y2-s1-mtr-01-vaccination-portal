package com.se2030.vaccinationportal.campaign.center.mapper;

import com.se2030.vaccinationportal.campaign.center.dto.response.OperationalResourceResponse;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResource;

/**
 * Pure transformation utility mapping OperationalResource domain models to REST response DTOs.
 */
public final class OperationalResourceDtoMapper {

    private OperationalResourceDtoMapper() {
    }

    public static OperationalResourceResponse toResponse(OperationalResource resource) {
        if (resource == null) {
            return null;
        }
        Long centerId = resource.getCenter() != null ? resource.getCenter().getId() : null;
        return new OperationalResourceResponse(
                resource.getId(),
                centerId,
                resource.getCode(),
                resource.getResourceType(),
                resource.getName(),
                resource.getStatus(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}
