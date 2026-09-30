package com.se2030.vaccinationportal.campaign.center.dto.request;

import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating the role and status of an operational staff roster entry.
 * Roster userId is immutable.
 */
public record UpdateStaffRosterRequest(
        @NotNull(message = "Staff role is required")
        StaffRole role,

        @NotNull(message = "Staff status is required")
        OperationalStaffStatus status
) {
}
