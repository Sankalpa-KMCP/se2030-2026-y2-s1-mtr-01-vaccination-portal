package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import java.time.Instant;

/**
 * Public REST response DTO representing an operational staff roster entry under a vaccination center.
 */
public record OperationalStaffRosterResponse(
        Long id,
        Long centerId,
        Long userId,
        StaffRole role,
        OperationalStaffStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
