package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
 * Domain service managing operational staff assignments for vaccination sessions.
 * Enforces DRAFT-only mutation, operating interval bounds, center roster qualification, ACTIVE staff status,
 * and deterministic global user pessimistic concurrency locking across all centers.
 */
@Service
@Transactional
public class StaffAssignmentService {

    private final StaffAssignmentRepository assignmentRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final OperationalStaffRosterRepository staffRosterRepository;
    private final OperationalStaffIdentityLockService identityLockService;

    public StaffAssignmentService(StaffAssignmentRepository assignmentRepository,
                                  VaccinationSessionRepository sessionRepository,
                                  OperationalStaffRosterRepository staffRosterRepository,
                                  OperationalStaffIdentityLockService identityLockService) {
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository, "assignmentRepository must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.staffRosterRepository = Objects.requireNonNull(staffRosterRepository, "staffRosterRepository must not be null");
        this.identityLockService = Objects.requireNonNull(identityLockService, "identityLockService must not be null");
    }

    /**
     * Creates a staff assignment for a DRAFT vaccination session.
     * Uses deterministic lock order: VaccinationSession -> global user OperationalStaffRoster set (ordered by ID ASC).
     */
    public StaffAssignment createAssignment(Long sessionId,
                                            Long staffRosterId,
                                            StaffRole role,
                                            Instant startAt,
                                            Instant endAt) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }
        if (staffRosterId == null) {
            throw new IllegalArgumentException("Staff roster ID is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("Staff role is required");
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
            throw new IllegalStateException("Staff assignments can only be configured for DRAFT sessions (current status: "
                    + session.getLifecycleStatus() + ")");
        }

        if (startAt.isBefore(session.getStartAt()) || endAt.isAfter(session.getEndAt())) {
            throw new IllegalArgumentException("Assignment interval [" + startAt + ", " + endAt
                    + ") must fall within session operating window [" + session.getStartAt() + ", " + session.getEndAt() + "]");
        }

        OperationalStaffRoster targetRoster = staffRosterRepository.findById(staffRosterId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff roster entry not found with ID: " + staffRosterId));

        identityLockService.lockUser(targetRoster.getUserId());

        List<OperationalStaffRoster> userRosters = staffRosterRepository.findByUserIdForUpdateOrderByIdAsc(targetRoster.getUserId());

        OperationalStaffRoster lockedRoster = userRosters.stream()
                .filter(r -> r.getId().equals(staffRosterId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Staff roster entry not found with ID: " + staffRosterId));

        if (!lockedRoster.getCenter().getId().equals(session.getCenter().getId())) {
            throw new IllegalStateException("Staff roster entry " + staffRosterId + " does not belong to session center "
                    + session.getCenter().getId());
        }

        if (lockedRoster.getStatus() != OperationalStaffStatus.ACTIVE) {
            throw new IllegalStateException("Staff roster entry must be ACTIVE to be assigned (current status: "
                    + lockedRoster.getStatus() + ")");
        }

        if (lockedRoster.getRole() != role) {
            throw new IllegalStateException("Requested assignment role " + role + " does not match roster qualification "
                    + lockedRoster.getRole());
        }

        if (!assignmentRepository.findOverlappingAssignmentsForUpdate(lockedRoster.getUserId(), null, startAt, endAt).isEmpty()) {
            throw new IllegalStateException("Staff member with user ID " + lockedRoster.getUserId()
                    + " is already assigned to an overlapping active session window");
        }

        StaffAssignment assignment = new StaffAssignment(session, lockedRoster, role, startAt, endAt);
        return assignmentRepository.save(assignment);
    }

    /**
     * Retrieves all staff assignments for a session, deterministically ordered by startAt ASC, id ASC.
     */
    @Transactional(readOnly = true)
    public List<StaffAssignment> getAssignmentsForSession(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId);
        }
        return assignmentRepository.findBySessionIdOrderByStartAtAscIdAsc(sessionId);
    }

    /**
     * Retrieves a single staff assignment by ID with session ownership validation.
     */
    @Transactional(readOnly = true)
    public StaffAssignment getAssignment(Long sessionId, Long assignmentId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }
        if (assignmentId == null) {
            throw new IllegalArgumentException("Assignment ID is required");
        }
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId);
        }

        StaffAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff assignment not found with ID: " + assignmentId));

        if (!assignment.getSession().getId().equals(sessionId)) {
            throw new ResourceNotFoundException("Staff assignment with ID " + assignmentId + " does not belong to session " + sessionId);
        }

        return assignment;
    }

    /**
     * Deletes a staff assignment from a DRAFT vaccination session.
     * Uses deterministic lock order: VaccinationSession -> OperationalStaffIdentity -> user OperationalStaffRoster set (ordered by ID ASC).
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
            throw new IllegalStateException("Staff assignments can only be deleted from DRAFT sessions (current status: "
                    + session.getLifecycleStatus() + ")");
        }

        StaffAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff assignment not found with ID: " + assignmentId));

        if (!assignment.getSession().getId().equals(sessionId)) {
            throw new ResourceNotFoundException("Staff assignment with ID " + assignmentId + " does not belong to session " + sessionId);
        }

        identityLockService.lockUser(assignment.getUserId());
        staffRosterRepository.findByUserIdForUpdateOrderByIdAsc(assignment.getUserId());
        assignmentRepository.delete(assignment);
    }
}
