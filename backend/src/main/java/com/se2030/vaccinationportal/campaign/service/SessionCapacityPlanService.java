package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.dto.response.CapacityPlanPreviewResponse;
import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
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
 * Service managing immutable session capacity plan revisions, throughput preview, and historical inspection.
 */
@Service
public class SessionCapacityPlanService {

    private final VaccinationSessionRepository sessionRepository;
    private final SessionCapacityPlanRepository planRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final SessionResourceAssignmentRepository resourceAssignmentRepository;
    private final CampaignDoseProgrammeRepository doseProgrammeRepository;
    private final SessionCapacityCalculationService calculationService;
    private final TimeSlotRepository timeSlotRepository;

    public SessionCapacityPlanService(VaccinationSessionRepository sessionRepository,
                                      SessionCapacityPlanRepository planRepository,
                                      StaffAssignmentRepository staffAssignmentRepository,
                                      SessionResourceAssignmentRepository resourceAssignmentRepository,
                                      CampaignDoseProgrammeRepository doseProgrammeRepository,
                                      SessionCapacityCalculationService calculationService,
                                      TimeSlotRepository timeSlotRepository) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
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
        int assignedVaccinators = countAssignedVaccinators(sessionId);
        int availableStations = countAvailableStations(sessionId);

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

        int safeOccupancy = (request.safeConcurrentOccupancy() != null && request.safeConcurrentOccupancy() > 0)
                ? request.safeConcurrentOccupancy()
                : session.getCenter().getSafeConcurrentOccupancy();

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
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Create request must not be null");
        }

        // Lock session to serialize concurrent revision increments
        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sessionId));

        if (session.getLifecycleStatus() != SessionLifecycleStatus.DRAFT) {
            throw new IllegalStateException("Session capacity plans can only be created for sessions in DRAFT status");
        }

        Integer currentSlotCapacity = timeSlotRepository.sumCapacityBySessionId(sessionId);
        if (currentSlotCapacity != null && currentSlotCapacity > 0 && request.approvedCapacity() != null
                && request.approvedCapacity() < currentSlotCapacity) {
            throw new IllegalStateException("Approved capacity (" + request.approvedCapacity()
                    + ") cannot be reduced below currently configured slot capacity total (" + currentSlotCapacity + ")");
        }

        int durationMinutes = calculateDurationMinutes(session);
        int expectedServiceMinutes = resolveExpectedServiceMinutes(session);
        int assignedVaccinators = countAssignedVaccinators(sessionId);
        int availableStations = countAvailableStations(sessionId);

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

        int safeOccupancy = (request.safeConcurrentOccupancy() != null && request.safeConcurrentOccupancy() > 0)
                ? request.safeConcurrentOccupancy()
                : session.getCenter().getSafeConcurrentOccupancy();

        int dwellMinutes = (request.expectedDwellMinutes() != null && request.expectedDwellMinutes() > 0)
                ? request.expectedDwellMinutes()
                : SessionCapacityPlan.DEFAULT_EXPECTED_DWELL_MINUTES;

        double accompanying = (request.expectedAccompanyingPersonsPerBooking() != null)
                ? request.expectedAccompanyingPersonsPerBooking()
                : SessionCapacityPlan.DEFAULT_ACCOMPANYING_RATIO;

        Integer ceiling = session.getCenter().getSessionServiceCeiling();

        Optional<SessionCapacityPlan> latestPlan = planRepository.findTopBySessionIdOrderByRevisionDesc(sessionId);
        int nextRevision = latestPlan.map(p -> p.getRevision() + 1).orElse(1);

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
                request.approvedBy()
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

    private int countAssignedVaccinators(Long sessionId) {
        List<StaffAssignment> assignments = staffAssignmentRepository.findBySessionIdOrderByStartAtAscIdAsc(sessionId);
        return (int) assignments.stream()
                .filter(a -> a.getRole() == StaffRole.VACCINATOR)
                .count();
    }

    private int countAvailableStations(Long sessionId) {
        List<SessionResourceAssignment> assignments = resourceAssignmentRepository.findBySessionIdOrderByStartAtAscIdAsc(sessionId);
        return (int) assignments.stream()
                .filter(a -> a.getResource() != null
                        && a.getResource().getResourceType() == ResourceType.VACCINATION_STATION
                        && a.getResource().getStatus() == OperationalResourceStatus.ACTIVE)
                .count();
    }
}
