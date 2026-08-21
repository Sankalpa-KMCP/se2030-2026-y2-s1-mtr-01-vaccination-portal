package com.se2030.vaccinationportal.campaign.mapper;

import com.se2030.vaccinationportal.campaign.dto.response.StaffAssignmentResponse;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;

/**
 * Pure DTO mapper for session staff assignment entities.
 */
public final class StaffAssignmentDtoMapper {

    private StaffAssignmentDtoMapper() {
    }

    public static StaffAssignmentResponse toResponse(StaffAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return new StaffAssignmentResponse(
                assignment.getId(),
                assignment.getSession() != null ? assignment.getSession().getId() : null,
                assignment.getStaffRoster() != null ? assignment.getStaffRoster().getId() : null,
                assignment.getUserId(),
                assignment.getRole(),
                assignment.getStartAt(),
                assignment.getEndAt(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }
}
