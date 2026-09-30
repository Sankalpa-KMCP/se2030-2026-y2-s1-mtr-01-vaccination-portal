package com.se2030.vaccinationportal.campaign.session.planning.mapper;

import com.se2030.vaccinationportal.campaign.session.planning.dto.response.SessionResourceAssignmentResponse;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionResourceAssignment;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
 * Pure DTO mapper for session resource assignment entities.
 */
public final class SessionResourceAssignmentDtoMapper {

    private SessionResourceAssignmentDtoMapper() {
    }

    public static SessionResourceAssignmentResponse toResponse(SessionResourceAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return new SessionResourceAssignmentResponse(
                assignment.getId(),
                assignment.getSession() != null ? assignment.getSession().getId() : null,
                assignment.getResource() != null ? assignment.getResource().getId() : null,
                assignment.getResource() != null ? assignment.getResource().getCode() : null,
                assignment.getResource() != null ? assignment.getResource().getResourceType() : null,
                assignment.getResource() != null ? assignment.getResource().getName() : null,
                assignment.getStartAt(),
                assignment.getEndAt(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }
}
