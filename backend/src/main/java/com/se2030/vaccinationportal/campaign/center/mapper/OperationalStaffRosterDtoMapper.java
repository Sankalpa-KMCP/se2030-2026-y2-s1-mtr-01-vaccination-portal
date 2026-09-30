package com.se2030.vaccinationportal.campaign.center.mapper;

import com.se2030.vaccinationportal.campaign.center.dto.response.OperationalStaffRosterResponse;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffRoster;

/**
 * Pure transformation utility mapping OperationalStaffRoster domain models to REST response DTOs.
 */
public final class OperationalStaffRosterDtoMapper {

    private OperationalStaffRosterDtoMapper() {
    }

    public static OperationalStaffRosterResponse toResponse(OperationalStaffRoster roster) {
        if (roster == null) {
            return null;
        }
        Long centerId = roster.getCenter() != null ? roster.getCenter().getId() : null;
        return new OperationalStaffRosterResponse(
                roster.getId(),
                centerId,
                roster.getUserId(),
                roster.getRole(),
                roster.getStatus(),
                roster.getCreatedAt(),
                roster.getUpdatedAt()
        );
    }
}
