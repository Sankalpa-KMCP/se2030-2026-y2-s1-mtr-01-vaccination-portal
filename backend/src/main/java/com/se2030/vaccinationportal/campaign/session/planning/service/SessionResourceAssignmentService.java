package com.se2030.vaccinationportal.campaign.session.planning.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assigns center resources to draft sessions while preventing conflicting use. */
@Service
@Transactional
public class SessionResourceAssignmentService {

    private final SessionResourceAssignmentRepository assignmentRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final OperationalResourceRepository resourceRepository;

    public SessionResourceAssignmentService(SessionResourceAssignmentRepository assignmentRepository,
                                            VaccinationSessionRepository sessionRepository,
                                            OperationalResourceRepository resourceRepository) {
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository, "assignmentRepository must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.resourceRepository = Objects.requireNonNull(resourceRepository, "resourceRepository must not be null");
    }

    /**
     * Creates an operational resource assignment for a DRAFT vaccination session.
     * Uses deterministic lock order: VaccinationSession -> OperationalResource.
     */
    public SessionResourceAssignment createAssignment(Long sessionId,
                                                      Long resourceId,
                                                      Instant startAt,
                                                      Instant endAt) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }
        if (resourceId == null) {
            throw new IllegalArgumentException("Resource ID is required");
        }
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("Start and end times are required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Assignment start time must be before end time");
        }

        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));

        if (session.getLifecycleStatus() != SessionLifecycleStatus.DRAFT) {
            throw new IllegalStateException("Operational resource assignments can only be configured for DRAFT sessions (current status: "
                    + session.getLifecycleStatus() + ")");
        }

        if (startAt.isBefore(session.getStartAt()) || endAt.isAfter(session.getEndAt())) {
            throw new IllegalArgumentException("Assignment interval [" + startAt + ", " + endAt
                    + ") must fall within session operating window [" + session.getStartAt() + ", " + session.getEndAt() + "]");
        }

        OperationalResource resource = resourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Operational resource not found with ID: " + resourceId));

        if (!resource.getCenter().getId().equals(session.getCenter().getId())) {
            throw new IllegalStateException("Operational resource " + resourceId + " does not belong to session center "
                    + session.getCenter().getId());
        }

        if (resource.getStatus() != OperationalResourceStatus.ACTIVE) {
            throw new IllegalStateException("Operational resource must be ACTIVE to be assigned (current status: "
                    + resource.getStatus() + ")");
        }

        if (!assignmentRepository.findOverlappingAssignmentsForUpdate(resourceId, null, startAt, endAt).isEmpty()) {
            throw new IllegalStateException("Operational resource " + resourceId
                    + " is already assigned to an overlapping active session window");
        }

        SessionResourceAssignment assignment = new SessionResourceAssignment(session, resource, startAt, endAt);
        return assignmentRepository.save(assignment);
    }

    /**
     * Retrieves all operational resource assignments for a session, deterministically ordered by startAt ASC, id ASC.
     */
    @Transactional(readOnly = true)
    public List<SessionResourceAssignment> getAssignmentsForSession(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId);
        }
        return assignmentRepository.findBySessionIdWithResource(sessionId);
    }

    /**
     * Retrieves a single operational resource assignment by ID with session ownership validation.
     */
    @Transactional(readOnly = true)
    public SessionResourceAssignment getAssignment(Long sessionId, Long assignmentId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }
        if (assignmentId == null) {
            throw new IllegalArgumentException("Assignment ID is required");
        }
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId);
        }

        SessionResourceAssignment assignment = assignmentRepository.findByIdWithResource(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource assignment not found with ID: " + assignmentId));

        if (!assignment.getSession().getId().equals(sessionId)) {
            throw new ResourceNotFoundException("Resource assignment with ID " + assignmentId + " does not belong to session " + sessionId);
        }

        return assignment;
    }

    /**
     * Deletes an operational resource assignment from a DRAFT vaccination session.
     * Uses deterministic lock order: VaccinationSession -> OperationalResource.
     */
    public void deleteAssignment(Long sessionId, Long assignmentId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }
        if (assignmentId == null) {
            throw new IllegalArgumentException("Assignment ID is required");
        }

        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));

        if (session.getLifecycleStatus() != SessionLifecycleStatus.DRAFT) {
            throw new IllegalStateException("Operational resource assignments can only be deleted from DRAFT sessions (current status: "
                    + session.getLifecycleStatus() + ")");
        }

        SessionResourceAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource assignment not found with ID: " + assignmentId));

        if (!assignment.getSession().getId().equals(sessionId)) {
            throw new ResourceNotFoundException("Resource assignment with ID " + assignmentId + " does not belong to session " + sessionId);
        }

        resourceRepository.findByIdForUpdate(assignment.getResource().getId());
        assignmentRepository.delete(assignment);
    }
}
