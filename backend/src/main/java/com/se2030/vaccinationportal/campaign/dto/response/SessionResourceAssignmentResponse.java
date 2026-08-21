package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.ResourceType;
import java.time.Instant;

/**
 * Response payload representing a session operational resource assignment.
 */
public record SessionResourceAssignmentResponse(
        Long id,
        Long sessionId,
        Long resourceId,
        String resourceCode,
        ResourceType resourceType,
        String resourceName,
        Instant startAt,
        Instant endAt,
        Instant createdAt,
        Instant updatedAt
) {
}
