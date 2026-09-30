package com.se2030.vaccinationportal.campaign.session.planning.mapper;

import com.se2030.vaccinationportal.campaign.session.planning.dto.response.StaffAssignmentResponse;
import com.se2030.vaccinationportal.campaign.session.planning.model.StaffAssignment;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
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
