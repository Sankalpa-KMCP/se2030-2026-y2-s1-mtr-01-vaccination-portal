package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.dto.response.CapacityPlanPreviewResponse;
import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
 * Service managing immutable session capacity plan revisions, throughput preview, and historical inspection.
 */
@Service
public class SessionCapacityPlanService {

    private final VaccinationSessionRepository sessionRepository;
    private final VaccinationCenterRepository centerRepository;
    private final SessionCapacityPlanRepository planRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final SessionResourceAssignmentRepository resourceAssignmentRepository;
    private final CampaignDoseProgrammeRepository doseProgrammeRepository;
    private final SessionCapacityCalculationService calculationService;
    private final TimeSlotRepository timeSlotRepository;

    public SessionCapacityPlanService(VaccinationSessionRepository sessionRepository,
                                      VaccinationCenterRepository centerRepository,
                                      SessionCapacityPlanRepository planRepository,
                                      StaffAssignmentRepository staffAssignmentRepository,
                                      SessionResourceAssignmentRepository resourceAssignmentRepository,
                                      CampaignDoseProgrammeRepository doseProgrammeRepository,
                                      SessionCapacityCalculationService calculationService,
                                      TimeSlotRepository timeSlotRepository) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.centerRepository = Objects.requireNonNull(centerRepository, "centerRepository must not be null");
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.staffAssignmentRepository = Objects.requireNonNull(staffAssignmentRepository, "staffAssignmentRepository must not be null");
        this.resourceAssignmentRepository = Objects.requireNonNull(resourceAssignmentRepository, "resourceAssignmentRepository must not be null");
        this.doseProgrammeRepository = Objects.requireNonNull(doseProgrammeRepository, "doseProgrammeRepository must not be null");
        this.calculationService = Objects.requireNonNull(calculationService, "calculationService must not be null");
        this.timeSlotRepository = Objects.requireNonNull(timeSlotRepository, "timeSlotRepository must not be null");
    }

    /**
     * Previews throughput and occupancy parameters for a session from current operational state without persisting.
     */
    @Transactional(readOnly = true)
    public CapacityPlanPreviewResponse previewCapacityPlan(Long sessionId, PreviewCapacityPlanRequest request) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Preview request must not be null");
        }

        VaccinationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));

        int durationMinutes = calculateDurationMinutes(session);
        int expectedServiceMinutes = resolveExpectedServiceMinutes(session);
        int assignedVaccinators = countAssignedVaccinators(session);
        int availableStations = countAvailableStations(session);

        int fullyEquipped = request.fullyEquippedStationCount();
        if (fullyEquipped < 0) {
            throw new IllegalArgumentException("Fully equipped station count must be non-negative");
        }
        if (fullyEquipped > availableStations) {
            throw new IllegalArgumentException("Fully equipped station count (" + fullyEquipped
                    + ") cannot exceed available station count (" + availableStations + ")");
        }

        int effectiveStations = calculationService.calculateEffectiveStationCount(
                assignedVaccinators, availableStations, fullyEquipped);
        int recommendedThroughput = calculationService.calculateRecommendedThroughput(
                effectiveStations, durationMinutes, expectedServiceMinutes);

        int centerSafeLimit = session.getCenter().getSafeConcurrentOccupancy();
        int safeOccupancy = resolveSafeConcurrentOccupancy(request.safeConcurrentOccupancy(), centerSafeLimit);

        int dwellMinutes = (request.expectedDwellMinutes() != null && request.expectedDwellMinutes() > 0)
                ? request.expectedDwellMinutes()
                : SessionCapacityPlan.DEFAULT_EXPECTED_DWELL_MINUTES;

        double accompanying = (request.expectedAccompanyingPersonsPerBooking() != null)
                ? request.expectedAccompanyingPersonsPerBooking()
                : SessionCapacityPlan.DEFAULT_ACCOMPANYING_RATIO;

        Integer ceiling = session.getCenter().getSessionServiceCeiling();

        boolean overrideRequired = request.approvedCapacity() != null
                && request.approvedCapacity() > recommendedThroughput;
        boolean overrideValid = true;
        if (request.approvedCapacity() != null) {
            if (ceiling != null && request.approvedCapacity() > ceiling) {
                overrideValid = false;
            } else if (overrideRequired && (request.overrideReason() == null || request.overrideReason().trim().isEmpty())) {
                overrideValid = false;
            }
        }

        return new CapacityPlanPreviewResponse(
                sessionId,
                durationMinutes,
                expectedServiceMinutes,
                assignedVaccinators,
                availableStations,
                fullyEquipped,
                effectiveStations,
                recommendedThroughput,
                request.approvedCapacity(),
                safeOccupancy,
                dwellMinutes,
                accompanying,
                ceiling,
                overrideRequired,
                overrideValid,
                Instant.now()
        );
    }

    /**
     * Creates and approves a new immutable capacity plan revision for a session in DRAFT status.
     * Uses pessimistic locking on the session to serialize revision increments.
     */
    @Transactional
    public SessionCapacityPlan createCapacityPlan(Long sessionId, CreateSessionCapacityPlanRequest request) {
        return createCapacityPlan(sessionId, request, null);
    }

    /**
     * Creates and approves a new immutable capacity plan revision for a session in DRAFT status with trusted actor context.
     * Uses pessimistic locking on the session to serialize revision increments.
     * Enforces ROLE_CENTER_MANAGER authorization when approvedCapacity > recommendedThroughput.
     */
    @Transactional
    public SessionCapacityPlan createCapacityPlan(Long sessionId,
                                                  CreateSessionCapacityPlanRequest request,
                                                  com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor actor) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Create request must not be null");
        }

        Long centerId = sessionRepository.findCenterIdBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));
        VaccinationCenter lockedCenter = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + centerId));

        // Center -> session -> slots is the canonical safety-sensitive lock order.
        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));
        if (!session.getCenter().getId().equals(centerId)) {
            throw new IllegalStateException("Authoritative session parent center changed concurrently");
        }

        if (session.getLifecycleStatus() != SessionLifecycleStatus.DRAFT) {
            throw new IllegalStateException("Session capacity plans can only be created for sessions in DRAFT status");
        }

        List<TimeSlot> existingSlots = timeSlotRepository.findBySessionIdForUpdateOrderByIdAsc(sessionId);
        int currentSlotCapacity = existingSlots.stream().mapToInt(TimeSlot::getCapacity).sum();
        if (currentSlotCapacity > 0 && request.approvedCapacity() != null
                && request.approvedCapacity() < currentSlotCapacity) {
            throw new IllegalStateException("Approved capacity (" + request.approvedCapacity()
                    + ") cannot be reduced below currently configured slot capacity total (" + currentSlotCapacity + ")");
        }

        int durationMinutes = calculateDurationMinutes(session);
        int expectedServiceMinutes = resolveExpectedServiceMinutes(session);
        int assignedVaccinators = countAssignedVaccinators(session);
        int availableStations = countAvailableStations(session);

        int fullyEquipped = request.fullyEquippedStationCount();
        if (fullyEquipped < 0) {
            throw new IllegalArgumentException("Fully equipped station count must be non-negative");
        }
        if (fullyEquipped > availableStations) {
            throw new IllegalArgumentException("Fully equipped station count (" + fullyEquipped
                    + ") cannot exceed available station count (" + availableStations + ")");
        }

        int effectiveStations = calculationService.calculateEffectiveStationCount(
                assignedVaccinators, availableStations, fullyEquipped);
        int recommendedThroughput = calculationService.calculateRecommendedThroughput(
                effectiveStations, durationMinutes, expectedServiceMinutes);

        // Enforce capacity override authorization
        boolean overrideRequired = request.approvedCapacity() != null
                && request.approvedCapacity() > recommendedThroughput;
        if (overrideRequired) {
            if (actor == null) {
                throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                        "Full authentication is required to override session capacity");
            }
            if (!actor.hasRole("ROLE_CENTER_MANAGER")) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "User is not authorized to override session capacity. Role ROLE_CENTER_MANAGER is required.");
            }
        }

        int centerSafeLimit = lockedCenter.getSafeConcurrentOccupancy();
        int safeOccupancy = resolveSafeConcurrentOccupancy(request.safeConcurrentOccupancy(), centerSafeLimit);

        int dwellMinutes = (request.expectedDwellMinutes() != null && request.expectedDwellMinutes() > 0)
                ? request.expectedDwellMinutes()
                : SessionCapacityPlan.DEFAULT_EXPECTED_DWELL_MINUTES;

        double accompanying = (request.expectedAccompanyingPersonsPerBooking() != null)
                ? request.expectedAccompanyingPersonsPerBooking()
                : SessionCapacityPlan.DEFAULT_ACCOMPANYING_RATIO;

        Integer ceiling = lockedCenter.getSessionServiceCeiling();

        Optional<SessionCapacityPlan> latestPlan = planRepository.findFirstBySessionIdOrderByRevisionDesc(sessionId);
        int nextRevision = latestPlan.map(p -> p.getRevision() + 1).orElse(1);

        String approvedBy = actor != null ? String.valueOf(actor.userId()) : null;

        SessionCapacityPlan plan = new SessionCapacityPlan(
                session,
                nextRevision,
                durationMinutes,
                expectedServiceMinutes,
                assignedVaccinators,
                availableStations,
                fullyEquipped,
                effectiveStations,
                recommendedThroughput,
                request.approvedCapacity(),
                safeOccupancy,
                dwellMinutes,
                accompanying,
                ceiling,
                request.overrideReason(),
                approvedBy
        );

        return planRepository.save(plan);
    }

    /**
     * Lists all historical capacity plan revisions for a session, ordered by revision ascending.
     */
    @Transactional(readOnly = true)
    public List<SessionCapacityPlan> getCapacityPlansForSession(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null");
        }
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId);
        }
        return planRepository.findBySessionIdOrderByRevisionAsc(sessionId);
    }

    /**
     * Retrieves the latest / current capacity plan revision for a session.
     */
    @Transactional(readOnly = true)
    public SessionCapacityPlan getLatestCapacityPlan(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null");
        }
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId);
        }
        return planRepository.findTopBySessionIdOrderByRevisionDesc(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session capacity plan not found for session ID: " + sessionId));
    }

    /**
     * Retrieves a specific historical revision for a session.
     */
    @Transactional(readOnly = true)
    public SessionCapacityPlan getCapacityPlanByRevision(Long sessionId, int revision) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null");
        }
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId);
        }
        return planRepository.findBySessionIdAndRevision(sessionId, revision)
                .orElseThrow(() -> new ResourceNotFoundException("Session capacity plan revision " + revision + " not found for session ID: " + sessionId));
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

    private int countAssignedVaccinators(VaccinationSession session) {
        List<StaffAssignment> assignments = staffAssignmentRepository.findBySessionIdOrderByStartAtAscIdAsc(session.getId());
        List<StaffAssignment> qualifying = assignments.stream()
                .filter(a -> a.getRole() == StaffRole.VACCINATOR
                        && a.getStaffRoster() != null
                        && a.getStaffRoster().getStatus() == OperationalStaffStatus.ACTIVE)
                .toList();
        return calculationService.calculateMinimumConcurrentIdentities(
                session.getStartAt(),
                session.getEndAt(),
                qualifying,
                StaffAssignment::getUserId,
                StaffAssignment::getStartAt,
                StaffAssignment::getEndAt
        );
    }

    private int resolveSafeConcurrentOccupancy(Integer requestedSafeOccupancy, int centerSafeLimit) {
        if (requestedSafeOccupancy == null) {
            return centerSafeLimit;
        }
        if (requestedSafeOccupancy <= 0) {
            throw new IllegalArgumentException("Safe concurrent occupancy must be positive if specified");
        }
        if (requestedSafeOccupancy > centerSafeLimit) {
            throw new IllegalArgumentException(String.format(
                    "Requested safe concurrent occupancy (%d) cannot exceed center safety limit (%d)",
                    requestedSafeOccupancy, centerSafeLimit));
        }
        return requestedSafeOccupancy;
    }

    private int countAvailableStations(VaccinationSession session) {
        List<SessionResourceAssignment> assignments = resourceAssignmentRepository.findBySessionIdOrderByStartAtAscIdAsc(session.getId());
        List<SessionResourceAssignment> qualifying = assignments.stream()
                .filter(a -> a.getResource() != null
                        && a.getResource().getResourceType() == ResourceType.VACCINATION_STATION
                        && a.getResource().getStatus() == OperationalResourceStatus.ACTIVE)
                .toList();
        return calculationService.calculateMinimumConcurrentIdentities(
                session.getStartAt(),
                session.getEndAt(),
                qualifying,
                a -> a.getResource().getId(),
                SessionResourceAssignment::getStartAt,
                SessionResourceAssignment::getEndAt
        );
    }
}
