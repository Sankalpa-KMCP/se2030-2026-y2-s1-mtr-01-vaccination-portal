package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionResourceAssignmentRequest;
import com.se2030.vaccinationportal.campaign.dto.request.CreateStaffAssignmentRequest;
import com.se2030.vaccinationportal.campaign.dto.response.SessionResourceAssignmentResponse;
import com.se2030.vaccinationportal.campaign.dto.response.StaffAssignmentResponse;
import com.se2030.vaccinationportal.campaign.mapper.SessionResourceAssignmentDtoMapper;
import com.se2030.vaccinationportal.campaign.mapper.StaffAssignmentDtoMapper;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.service.SessionResourceAssignmentService;
import com.se2030.vaccinationportal.campaign.service.StaffAssignmentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
 * REST Controller exposing operational resource and staff assignment endpoints for vaccination sessions.
 */
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
public class SessionOperationalAssignmentController {

    private final SessionResourceAssignmentService resourceAssignmentService;
    private final StaffAssignmentService staffAssignmentService;

    public SessionOperationalAssignmentController(SessionResourceAssignmentService resourceAssignmentService,
                                                  StaffAssignmentService staffAssignmentService) {
        this.resourceAssignmentService = Objects.requireNonNull(resourceAssignmentService, "resourceAssignmentService must not be null");
        this.staffAssignmentService = Objects.requireNonNull(staffAssignmentService, "staffAssignmentService must not be null");
    }

    // --- Resource Assignments ---

    @PostMapping("/resource-assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<SessionResourceAssignmentResponse> createResourceAssignment(
            @PathVariable("sessionId") Long sessionId,
            @Valid @RequestBody CreateSessionResourceAssignmentRequest request
    ) {
        SessionResourceAssignment created = resourceAssignmentService.createAssignment(
                sessionId,
                request.resourceId(),
                request.startAt(),
                request.endAt()
        );
        SessionResourceAssignmentResponse response = SessionResourceAssignmentDtoMapper.toResponse(created);
        URI location = URI.create(String.format("/api/v1/sessions/%d/resource-assignments/%d", sessionId, created.getId()));
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/resource-assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<List<SessionResourceAssignmentResponse>> listResourceAssignments(
            @PathVariable("sessionId") Long sessionId
    ) {
        List<SessionResourceAssignment> assignments = resourceAssignmentService.getAssignmentsForSession(sessionId);
        List<SessionResourceAssignmentResponse> response = assignments.stream()
                .map(SessionResourceAssignmentDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/resource-assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<SessionResourceAssignmentResponse> getResourceAssignment(
            @PathVariable("sessionId") Long sessionId,
            @PathVariable("assignmentId") Long assignmentId
    ) {
        SessionResourceAssignment assignment = resourceAssignmentService.getAssignment(sessionId, assignmentId);
        return ResponseEntity.ok(SessionResourceAssignmentDtoMapper.toResponse(assignment));
    }

    @DeleteMapping("/resource-assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<Void> deleteResourceAssignment(
            @PathVariable("sessionId") Long sessionId,
            @PathVariable("assignmentId") Long assignmentId
    ) {
        resourceAssignmentService.deleteAssignment(sessionId, assignmentId);
        return ResponseEntity.noContent().build();
    }

    // --- Staff Assignments ---

    @PostMapping("/staff-assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<StaffAssignmentResponse> createStaffAssignment(
            @PathVariable("sessionId") Long sessionId,
            @Valid @RequestBody CreateStaffAssignmentRequest request
    ) {
        StaffAssignment created = staffAssignmentService.createAssignment(
                sessionId,
                request.staffRosterId(),
                request.role(),
                request.startAt(),
                request.endAt()
        );
        StaffAssignmentResponse response = StaffAssignmentDtoMapper.toResponse(created);
        URI location = URI.create(String.format("/api/v1/sessions/%d/staff-assignments/%d", sessionId, created.getId()));
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/staff-assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<List<StaffAssignmentResponse>> listStaffAssignments(
            @PathVariable("sessionId") Long sessionId
    ) {
        List<StaffAssignment> assignments = staffAssignmentService.getAssignmentsForSession(sessionId);
        List<StaffAssignmentResponse> response = assignments.stream()
                .map(StaffAssignmentDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/staff-assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<StaffAssignmentResponse> getStaffAssignment(
            @PathVariable("sessionId") Long sessionId,
            @PathVariable("assignmentId") Long assignmentId
    ) {
        StaffAssignment assignment = staffAssignmentService.getAssignment(sessionId, assignmentId);
        return ResponseEntity.ok(StaffAssignmentDtoMapper.toResponse(assignment));
    }

    @DeleteMapping("/staff-assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<Void> deleteStaffAssignment(
            @PathVariable("sessionId") Long sessionId,
            @PathVariable("assignmentId") Long assignmentId
    ) {
        staffAssignmentService.deleteAssignment(sessionId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
