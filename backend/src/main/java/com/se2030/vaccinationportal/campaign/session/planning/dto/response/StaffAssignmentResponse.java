package com.se2030.vaccinationportal.campaign.session.planning.dto.response;

import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import java.time.Instant;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
 * Response payload representing a session staff assignment.
 */
public record StaffAssignmentResponse(
        Long id,
        Long sessionId,
        Long staffRosterId,
        Long userId,
        StaffRole role,
        Instant startAt,
        Instant endAt,
        Instant createdAt,
        Instant updatedAt
) {
}
