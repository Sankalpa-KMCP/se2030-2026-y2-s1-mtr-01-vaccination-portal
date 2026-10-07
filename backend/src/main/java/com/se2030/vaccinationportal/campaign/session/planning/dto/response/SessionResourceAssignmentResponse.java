package com.se2030.vaccinationportal.campaign.session.planning.dto.response;

import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
import java.time.Instant;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
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
