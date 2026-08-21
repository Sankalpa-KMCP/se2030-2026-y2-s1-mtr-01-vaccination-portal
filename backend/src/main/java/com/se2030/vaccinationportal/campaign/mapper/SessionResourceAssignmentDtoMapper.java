package com.se2030.vaccinationportal.campaign.mapper;

import com.se2030.vaccinationportal.campaign.dto.response.SessionResourceAssignmentResponse;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;

/**
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
