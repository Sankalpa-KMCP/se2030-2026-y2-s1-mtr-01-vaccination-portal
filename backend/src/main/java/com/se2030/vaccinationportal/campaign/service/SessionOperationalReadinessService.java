package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.validation.VaccinationSessionValidator;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authoritative service evaluating operational readiness for vaccination session scheduling.
 * Enforces resource, staff, and capacity plan invariants under deterministic global pessimistic lock ordering.
 */
@Service
public class SessionOperationalReadinessService {

    private final VaccinationSessionValidator sessionValidator;
    private final VaccinationSessionRepository sessionRepository;
    private final OperationalResourceRepository resourceRepository;
    private final SessionResourceAssignmentRepository resourceAssignmentRepository;
    private final OperationalStaffRosterRepository staffRosterRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final SessionCapacityPlanRepository planRepository;
    private final CampaignDoseProgrammeRepository doseProgrammeRepository;
    private final SessionCapacityCalculationService calculationService;
    private final OperationalStaffIdentityLockService identityLockService;

    public SessionOperationalReadinessService(
            VaccinationSessionValidator sessionValidator,
            VaccinationSessionRepository sessionRepository,
            OperationalResourceRepository resourceRepository,
            SessionResourceAssignmentRepository resourceAssignmentRepository,
            OperationalStaffRosterRepository staffRosterRepository,
            StaffAssignmentRepository staffAssignmentRepository,
            SessionCapacityPlanRepository planRepository,
            CampaignDoseProgrammeRepository doseProgrammeRepository,
            SessionCapacityCalculationService calculationService,
            OperationalStaffIdentityLockService identityLockService
    ) {
        this.sessionValidator = Objects.requireNonNull(sessionValidator, "sessionValidator must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.resourceRepository = Objects.requireNonNull(resourceRepository, "resourceRepository must not be null");
        this.resourceAssignmentRepository = Objects.requireNonNull(resourceAssignmentRepository, "resourceAssignmentRepository must not be null");
        this.staffRosterRepository = Objects.requireNonNull(staffRosterRepository, "staffRosterRepository must not be null");
        this.staffAssignmentRepository = Objects.requireNonNull(staffAssignmentRepository, "staffAssignmentRepository must not be null");
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.doseProgrammeRepository = Objects.requireNonNull(doseProgrammeRepository, "doseProgrammeRepository must not be null");
        this.calculationService = Objects.requireNonNull(calculationService, "calculationService must not be null");
        this.identityLockService = Objects.requireNonNull(identityLockService, "identityLockService must not be null");
    }

    /**
     * Authoritatively validates operational readiness of a locked session within the scheduling transaction.
     * Acquires operational resource and global staff user pessimistic locks in deterministic order.
     *
     * @param lockedCampaign Locked parent VaccinationCampaign
     * @param lockedSession  Locked target VaccinationSession
     * @throws IllegalStateException    if operational readiness checks fail
     * @throws IllegalArgumentException if campaign/session lifecycle precludes scheduling
     */
    public void assertReadyForScheduling(VaccinationCampaign lockedCampaign, VaccinationSession lockedSession) {
        Objects.requireNonNull(lockedCampaign, "lockedCampaign must not be null");
        Objects.requireNonNull(lockedSession, "lockedSession must not be null");

        // 1. Basic campaign & session pre-conditions
        if (lockedCampaign.getStatus() == CampaignStatus.SUSPENDED) {
            throw new IllegalArgumentException("Cannot schedule session for suspended campaign with ID: " + lockedCampaign.getId());
        }
        if (lockedCampaign.getStatus().isOperationallyClosed()) {
            throw new IllegalArgumentException("Cannot schedule session for operationally closed campaign with ID: " + lockedCampaign.getId());
        }

        SessionLifecycleStatus status = lockedSession.getLifecycleStatus();
        if (status == SessionLifecycleStatus.SCHEDULED) {
            throw new IllegalStateException("Session is already in SCHEDULED lifecycle status");
        }
        if (status != SessionLifecycleStatus.DRAFT) {
            throw new IllegalStateException("Invalid session lifecycle transition from " + status + " to SCHEDULED");
        }

        // 2. Revalidate schedule against campaign service dates, center active status, operating hours, and closures
        sessionValidator.validateSessionSchedule(
                lockedCampaign,
                lockedSession.getCenter(),
                lockedSession.getStartAt(),
                lockedSession.getEndAt(),
                lockedSession.getBookingCloseAt()
        );

        Long sessionId = lockedSession.getId();
        Long centerId = lockedSession.getCenter().getId();

        // 3. Lock and validate Operational Resources in ascending ID order
        List<SessionResourceAssignment> resourceAssignments = resourceAssignmentRepository
                .findBySessionIdOrderByStartAtAscIdAsc(sessionId);

        List<Long> resourceIds = resourceAssignments.stream()
                .map(a -> a.getResource().getId())
                .distinct()
                .sorted()
                .toList();

        for (Long resourceId : resourceIds) {
            OperationalResource resource = resourceRepository.findByIdForUpdate(resourceId)
                    .orElseThrow(() -> new IllegalStateException("Operational resource not found with ID: " + resourceId));

            if (!resource.getCenter().getId().equals(centerId)) {
                throw new IllegalStateException("Assigned operational resource " + resourceId + " does not belong to session center " + centerId);
            }
            if (resource.getStatus() != OperationalResourceStatus.ACTIVE) {
                throw new IllegalStateException("Assigned operational resource '" + resource.getCode()
                        + "' is not ACTIVE (current status: " + resource.getStatus() + ")");
            }

            List<SessionResourceAssignment> overlapping = resourceAssignmentRepository
                    .findOverlappingAssignmentsForUpdate(resourceId, null, lockedSession.getStartAt(), lockedSession.getEndAt());
            for (SessionResourceAssignment ov : overlapping) {
                if (!ov.getSession().getId().equals(sessionId)) {
                    throw new IllegalStateException("Operational resource '" + resource.getCode()
                            + "' has an overlapping assignment on active session " + ov.getSession().getId());
                }
            }
        }

        // 4. Lock and validate Staff Roster entries and Staff Assignments in ascending userId order
        List<StaffAssignment> staffAssignments = staffAssignmentRepository
                .findBySessionIdOrderByStartAtAscIdAsc(sessionId);

        List<Long> userIds = staffAssignments.stream()
                .map(StaffAssignment::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        identityLockService.lockUsersOrdered(userIds);

        for (Long userId : userIds) {
            List<OperationalStaffRoster> userRosters = staffRosterRepository.findByUserIdForUpdateOrderByIdAsc(userId);

            List<StaffAssignment> overlappingStaff = staffAssignmentRepository
                    .findOverlappingAssignmentsForUpdate(userId, null, lockedSession.getStartAt(), lockedSession.getEndAt());
            for (StaffAssignment ov : overlappingStaff) {
                if (!ov.getSession().getId().equals(sessionId)) {
                    throw new IllegalStateException("Staff member with user ID " + userId
                            + " has an overlapping assignment on active session " + ov.getSession().getId());
                }
            }

            for (StaffAssignment assignment : staffAssignments) {
                if (assignment.getUserId().equals(userId)) {
                    OperationalStaffRoster roster = userRosters.stream()
                            .filter(r -> r.getId().equals(assignment.getStaffRoster().getId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Staff roster entry not found with ID: "
                                    + assignment.getStaffRoster().getId()));

                    if (!roster.getCenter().getId().equals(centerId)) {
                        throw new IllegalStateException("Assigned staff roster entry " + roster.getId()
                                + " does not belong to session center " + centerId);
                    }
                    if (roster.getStatus() != OperationalStaffStatus.ACTIVE) {
                        throw new IllegalStateException("Assigned staff member user ID " + userId
                                + " is not ACTIVE in center roster (current status: " + roster.getStatus() + ")");
                    }
                    if (roster.getRole() != assignment.getRole()) {
                        throw new IllegalStateException("Assigned staff member user ID " + userId
                                + " roster role (" + roster.getRole() + ") no longer matches assignment qualification ("
                                + assignment.getRole() + ")");
                    }
                }
            }
        }

        // 5. Evaluate qualifying counts
        int availableStations = (int) resourceAssignments.stream()
                .filter(a -> a.getResource() != null
                        && a.getResource().getResourceType() == ResourceType.VACCINATION_STATION
                        && a.getResource().getStatus() == OperationalResourceStatus.ACTIVE)
                .count();

        if (availableStations <= 0) {
            throw new IllegalStateException("Session has no active vaccination station resources assigned");
        }

        int assignedVaccinators = (int) staffAssignments.stream()
                .filter(a -> a.getRole() == StaffRole.VACCINATOR
                        && a.getStaffRoster().getStatus() == OperationalStaffStatus.ACTIVE)
                .count();

        if (assignedVaccinators <= 0) {
            throw new IllegalStateException("Session has no active vaccinator staff assigned");
        }

        // 6. Evaluate latest SessionCapacityPlan
        SessionCapacityPlan latestPlan = planRepository.findTopBySessionIdOrderByRevisionDesc(sessionId)
                .orElseThrow(() -> new IllegalStateException("No approved capacity plan exists for session " + sessionId));

        int durationMinutes = calculateDurationMinutes(lockedSession);
        int expectedServiceMinutes = resolveExpectedServiceMinutes(lockedSession);

        if (latestPlan.getDurationMinutes() != durationMinutes) {
            throw new IllegalStateException("Latest capacity plan revision " + latestPlan.getRevision()
                    + " duration (" + latestPlan.getDurationMinutes() + "m) is stale relative to session duration ("
                    + durationMinutes + "m)");
        }
        if (latestPlan.getExpectedServiceMinutes() != expectedServiceMinutes) {
            throw new IllegalStateException("Latest capacity plan revision " + latestPlan.getRevision()
                    + " expected service minutes (" + latestPlan.getExpectedServiceMinutes()
                    + "m) is stale relative to current programme (" + expectedServiceMinutes + "m)");
        }
        if (latestPlan.getAssignedVaccinatorCount() != assignedVaccinators) {
            throw new IllegalStateException("Latest capacity plan revision " + latestPlan.getRevision()
                    + " assigned vaccinator count (" + latestPlan.getAssignedVaccinatorCount()
                    + ") is stale relative to current assigned vaccinators (" + assignedVaccinators + ")");
        }
        if (latestPlan.getAvailableStationCount() != availableStations) {
            throw new IllegalStateException("Latest capacity plan revision " + latestPlan.getRevision()
                    + " available station count (" + latestPlan.getAvailableStationCount()
                    + ") is stale relative to current available stations (" + availableStations + ")");
        }
        if (latestPlan.getFullyEquippedStationCount() > availableStations) {
            throw new IllegalStateException("Latest capacity plan revision " + latestPlan.getRevision()
                    + " fully equipped station count (" + latestPlan.getFullyEquippedStationCount()
                    + ") exceeds current available stations (" + availableStations + ")");
        }

        int effectiveStations = calculationService.calculateEffectiveStationCount(
                assignedVaccinators,
                availableStations,
                latestPlan.getFullyEquippedStationCount()
        );
        if (effectiveStations <= 0) {
            throw new IllegalStateException("Effective station count is zero; cannot schedule session");
        }

        int recommendedThroughput = calculationService.calculateRecommendedThroughput(
                effectiveStations,
                durationMinutes,
                expectedServiceMinutes
        );
        if (recommendedThroughput <= 0) {
            throw new IllegalStateException("Recommended throughput is zero; cannot schedule session");
        }

        if (latestPlan.getApprovedCapacity() <= 0) {
            throw new IllegalStateException("Approved capacity must be positive; cannot schedule session");
        }

        if (latestPlan.getApprovedCapacity() > recommendedThroughput) {
            if (latestPlan.getOverrideReason() == null || latestPlan.getOverrideReason().trim().isEmpty()) {
                throw new IllegalStateException("Capacity plan approved capacity (" + latestPlan.getApprovedCapacity()
                        + ") exceeds recommended throughput (" + recommendedThroughput + ") but lacks an override reason");
            }
        }

        Integer ceiling = lockedSession.getCenter().getSessionServiceCeiling();
        if (ceiling != null && latestPlan.getApprovedCapacity() > ceiling) {
            throw new IllegalStateException("Approved capacity (" + latestPlan.getApprovedCapacity()
                    + ") exceeds center session service ceiling (" + ceiling + ")");
        }

        if (latestPlan.getSafeConcurrentOccupancy() <= 0) {
            throw new IllegalStateException("Safe concurrent occupancy must be positive");
        }
    }

    /**
     * Read-only evaluation of session operational readiness returning structured diagnostic results.
     */
    @Transactional(readOnly = true)
    public OperationalReadinessResult evaluateReadiness(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }

        VaccinationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));

        List<String> violations = new ArrayList<>();

        if (session.getCampaign().getStatus() == CampaignStatus.SUSPENDED) {
            violations.add("Campaign is SUSPENDED");
        }
        if (session.getCampaign().getStatus().isOperationallyClosed()) {
            violations.add("Campaign is operationally closed");
        }
        if (session.getLifecycleStatus() != SessionLifecycleStatus.DRAFT) {
            violations.add("Session is not in DRAFT status (current: " + session.getLifecycleStatus() + ")");
        }

        try {
            sessionValidator.validateSessionSchedule(
                    session.getCampaign(),
                    session.getCenter(),
                    session.getStartAt(),
                    session.getEndAt(),
                    session.getBookingCloseAt()
            );
        } catch (RuntimeException ex) {
            violations.add(ex.getMessage());
        }

        List<SessionResourceAssignment> resourceAssignments = resourceAssignmentRepository
                .findBySessionIdOrderByStartAtAscIdAsc(sessionId);
        for (SessionResourceAssignment a : resourceAssignments) {
            if (a.getResource().getStatus() != OperationalResourceStatus.ACTIVE) {
                violations.add("Resource " + a.getResource().getCode() + " is " + a.getResource().getStatus());
            }
        }

        List<StaffAssignment> staffAssignments = staffAssignmentRepository
                .findBySessionIdOrderByStartAtAscIdAsc(sessionId);
        for (StaffAssignment a : staffAssignments) {
            if (a.getStaffRoster().getStatus() != OperationalStaffStatus.ACTIVE) {
                violations.add("Staff user " + a.getUserId() + " is " + a.getStaffRoster().getStatus());
            }
            if (a.getStaffRoster().getRole() != a.getRole()) {
                violations.add("Staff user " + a.getUserId() + " qualification mismatch");
            }
        }

        int availableStations = (int) resourceAssignments.stream()
                .filter(a -> a.getResource() != null
                        && a.getResource().getResourceType() == ResourceType.VACCINATION_STATION
                        && a.getResource().getStatus() == OperationalResourceStatus.ACTIVE)
                .count();
        if (availableStations <= 0) {
            violations.add("No active vaccination stations assigned");
        }

        int assignedVaccinators = (int) staffAssignments.stream()
                .filter(a -> a.getRole() == StaffRole.VACCINATOR
                        && a.getStaffRoster().getStatus() == OperationalStaffStatus.ACTIVE)
                .count();
        if (assignedVaccinators <= 0) {
            violations.add("No active vaccinators assigned");
        }

        Optional<SessionCapacityPlan> latestPlan = planRepository.findTopBySessionIdOrderByRevisionDesc(sessionId);
        if (latestPlan.isEmpty()) {
            violations.add("No approved capacity plan exists");
            return new OperationalReadinessResult(
                    sessionId,
                    violations.isEmpty(),
                    violations,
                    assignedVaccinators,
                    availableStations,
                    0,
                    0,
                    null,
                    null
            );
        }

        SessionCapacityPlan plan = latestPlan.get();
        int durationMinutes = calculateDurationMinutes(session);
        int expectedServiceMinutes = resolveExpectedServiceMinutes(session);

        if (plan.getDurationMinutes() != durationMinutes) {
            violations.add("Capacity plan duration is stale");
        }
        if (plan.getExpectedServiceMinutes() != expectedServiceMinutes) {
            violations.add("Capacity plan expected service minutes is stale");
        }
        if (plan.getAssignedVaccinatorCount() != assignedVaccinators) {
            violations.add("Capacity plan vaccinator count is stale");
        }
        if (plan.getAvailableStationCount() != availableStations) {
            violations.add("Capacity plan station count is stale");
        }
        if (plan.getFullyEquippedStationCount() > availableStations) {
            violations.add("Fully equipped stations exceed available stations");
        }

        int effectiveStations = calculationService.calculateEffectiveStationCount(
                assignedVaccinators,
                availableStations,
                plan.getFullyEquippedStationCount()
        );
        int recommendedThroughput = calculationService.calculateRecommendedThroughput(
                effectiveStations,
                durationMinutes,
                expectedServiceMinutes
        );

        if (plan.getApprovedCapacity() <= 0) {
            violations.add("Approved capacity must be positive");
        } else if (plan.getApprovedCapacity() > recommendedThroughput && (plan.getOverrideReason() == null || plan.getOverrideReason().trim().isEmpty())) {
            violations.add("Approved capacity exceeds recommended throughput without override reason");
        }

        Integer ceiling = session.getCenter().getSessionServiceCeiling();
        if (ceiling != null && plan.getApprovedCapacity() > ceiling) {
            violations.add("Approved capacity exceeds center service ceiling");
        }

        return new OperationalReadinessResult(
                sessionId,
                violations.isEmpty(),
                violations,
                assignedVaccinators,
                availableStations,
                effectiveStations,
                recommendedThroughput,
                plan.getApprovedCapacity(),
                plan.getRevision()
        );
    }

    private int calculateDurationMinutes(VaccinationSession session) {
        long minutes = Duration.between(session.getStartAt(), session.getEndAt()).toMinutes();
        if (minutes <= 0) {
            throw new IllegalStateException("Session duration must be positive");
        }
        if (minutes > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) minutes;
    }

    private int resolveExpectedServiceMinutes(VaccinationSession session) {
        LocalDate sessionDate = LocalDate.ofInstant(session.getStartAt(), ZoneId.of(session.getScheduleTimezone()));
        return doseProgrammeRepository.findEffectiveProgrammeForDate(session.getCampaign().getId(), sessionDate)
                .map(CampaignDoseProgramme::getExpectedServiceMinutes)
                .orElse(CampaignDoseProgramme.DEFAULT_EXPECTED_SERVICE_MINUTES);
    }

    public record OperationalReadinessResult(
            Long sessionId,
            boolean ready,
            List<String> violations,
            Integer assignedVaccinators,
            Integer availableStations,
            Integer effectiveStations,
            Integer recommendedThroughput,
            Integer approvedCapacity,
            Integer revision
    ) {}
}
