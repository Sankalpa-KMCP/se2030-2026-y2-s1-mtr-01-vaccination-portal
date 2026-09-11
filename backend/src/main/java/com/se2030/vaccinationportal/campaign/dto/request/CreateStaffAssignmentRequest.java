package com.se2030.vaccinationportal.campaign.dto.request;

import com.se2030.vaccinationportal.campaign.model.StaffRole;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
 * Request payload for assigning a rostered staff member to a vaccination session.
 */
public record CreateStaffAssignmentRequest(
        @NotNull(message = "Staff roster ID is required")
        Long staffRosterId,

        @NotNull(message = "Staff role is required")
        StaffRole role,

        @NotNull(message = "Start time is required")
        Instant startAt,

        @NotNull(message = "End time is required")
        Instant endAt
) {
}
