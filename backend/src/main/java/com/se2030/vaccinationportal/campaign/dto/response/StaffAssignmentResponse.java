package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.StaffRole;
import java.time.Instant;

/**
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
