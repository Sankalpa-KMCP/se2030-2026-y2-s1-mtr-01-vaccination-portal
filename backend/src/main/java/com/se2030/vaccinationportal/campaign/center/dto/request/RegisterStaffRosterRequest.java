package com.se2030.vaccinationportal.campaign.center.dto.request;

import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request payload for registering a staff member onto a center's operational staff roster.
 */
public record RegisterStaffRosterRequest(
        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be positive")
        Long userId,

        @NotNull(message = "Staff role is required")
        StaffRole role,

        OperationalStaffStatus status
) {
}
