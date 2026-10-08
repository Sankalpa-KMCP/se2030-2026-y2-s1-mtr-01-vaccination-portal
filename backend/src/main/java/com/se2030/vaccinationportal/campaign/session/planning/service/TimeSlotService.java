package com.se2030.vaccinationportal.campaign.session.planning.service;

import com.se2030.vaccinationportal.campaign.session.planning.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Manages session slots within approved capacity and occupancy limits. */
@Service
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final VaccinationCenterRepository centerRepository;
    private final SessionCapacityPlanRepository capacityPlanRepository;
    private final SessionCapacityCalculationService calculationService;

    public TimeSlotService(
            TimeSlotRepository timeSlotRepository,
            VaccinationSessionRepository sessionRepository,
            VaccinationCenterRepository centerRepository,
            SessionCapacityPlanRepository capacityPlanRepository,
            SessionCapacityCalculationService calculationService
    ) {
        this.timeSlotRepository = Objects.requireNonNull(timeSlotRepository, "TimeSlotRepository must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "VaccinationSessionRepository must not be null");
        this.centerRepository = Objects.requireNonNull(centerRepository, "VaccinationCenterRepository must not be null");
        this.capacityPlanRepository = Objects.requireNonNull(capacityPlanRepository, "SessionCapacityPlanRepository must not be null");
        this.calculationService = Objects.requireNonNull(calculationService, "SessionCapacityCalculationService must not be null");
    }

    @Transactional
    public TimeSlot createSlot(Long sessionId, Instant startTime, Instant endTime, int capacity) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Slot start and end times must not be null");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Slot capacity must be strictly positive");
        }

        Long centerId = sessionRepository.findCenterIdBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));
        VaccinationCenter lockedCenter = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new IllegalStateException("Vaccination center not found with ID: " + centerId));
        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));
        if (!session.getCenter().getId().equals(centerId)) {
            throw new IllegalStateException("Authoritative session parent center changed concurrently");
        }
        requireTimeSlotConfigurationAllowed(session);

        if (startTime.isBefore(session.getStartAt()) || endTime.isAfter(session.getEndAt())) {
            throw new IllegalArgumentException("Slot interval must fall within session operating window");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Slot start time must be strictly before end time");
        }

        List<TimeSlot> existingSlots = timeSlotRepository.findBySessionIdForUpdateOrderByIdAsc(sessionId);
        int currentSum = existingSlots.stream().mapToInt(TimeSlot::getCapacity).sum();
        int newTotal = currentSum + capacity;

        Optional<SessionCapacityPlan> planOpt = capacityPlanRepository.findTopBySessionIdOrderByRevisionDesc(sessionId);
        planOpt.ifPresent(plan -> {
            if (newTotal > plan.getApprovedCapacity()) {
                throw new IllegalStateException("Total slot capacity (" + newTotal + ") cannot exceed approved session capacity (" + plan.getApprovedCapacity() + ")");
            }
        });

        int centerSafeLimit = lockedCenter.getSafeConcurrentOccupancy();
        int safeConcurrentOccupancy = calculationService.resolveEffectiveSafeConcurrentOccupancy(
                planOpt.map(SessionCapacityPlan::getSafeConcurrentOccupancy).orElse(null),
                centerSafeLimit
        );
        int expectedDwellMinutes = planOpt.map(SessionCapacityPlan::getExpectedDwellMinutes)
                .orElse(SessionCapacityPlan.DEFAULT_EXPECTED_DWELL_MINUTES);
        double accompanyingRatio = planOpt.map(SessionCapacityPlan::getExpectedAccompanyingPersonsPerBooking)
                .orElse(SessionCapacityPlan.DEFAULT_ACCOMPANYING_RATIO);

        List<OccupancyInterval> candidateIntervals = new ArrayList<>(existingSlots.size() + 1);
        candidateIntervals.addAll(calculationService.mapSlotsToOccupancyIntervals(existingSlots, expectedDwellMinutes, accompanyingRatio));
        candidateIntervals.add(new OccupancyInterval(startTime, endTime, capacity, expectedDwellMinutes, accompanyingRatio));

        OccupancyEvaluationResult occupancyResult = calculationService.evaluateConcurrentOccupancy(candidateIntervals, safeConcurrentOccupancy);
        if (!occupancyResult.safe()) {
            throw new IllegalStateException(String.format(
                    "Peak concurrent occupancy (%.1f) exceeds safe concurrent occupancy limit (%d) at %s",
                    occupancyResult.peakOccupancyUnits(),
                    occupancyResult.safeConcurrentOccupancy(),
                    occupancyResult.peakIntervalStart()
            ));
        }

        TimeSlot slot = new TimeSlot(session, startTime, endTime, capacity);
        return timeSlotRepository.save(slot);
    }

    @Transactional
    public TimeSlot updateSlotCapacity(Long slotId, int newCapacity) {
        if (slotId == null) {
            throw new IllegalArgumentException("Slot ID must not be null");
        }
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("Slot capacity must be strictly positive");
        }
        Long sessionId = timeSlotRepository.findSessionIdBySlotId(slotId)
                .orElseThrow(() -> new IllegalArgumentException("TimeSlot not found with ID: " + slotId));
        Long centerId = sessionRepository.findCenterIdBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));
        VaccinationCenter lockedCenter = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new IllegalStateException("Vaccination center not found with ID: " + centerId));
        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));
        if (!session.getCenter().getId().equals(centerId)) {
            throw new IllegalStateException("Authoritative session parent center changed concurrently");
        }
        requireTimeSlotConfigurationAllowed(session);

        List<TimeSlot> sessionSlots = timeSlotRepository.findBySessionIdForUpdateOrderByIdAsc(sessionId);
        TimeSlot slot = sessionSlots.stream()
                .filter(s -> s.getId().equals(slotId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("TimeSlot not found with ID: " + slotId));

        if (!slot.getSession().getId().equals(session.getId())) {
            throw new IllegalStateException("TimeSlot session mismatch");
        }

        if (newCapacity < slot.getReservedCount()) {
            throw new IllegalStateException("Cannot reduce slot capacity (" + newCapacity + ") below existing reserved bookings (" + slot.getReservedCount() + ")");
        }

        int currentSum = sessionSlots.stream().mapToInt(TimeSlot::getCapacity).sum();
        int diff = newCapacity - slot.getCapacity();
        int newTotal = currentSum + diff;

        Optional<SessionCapacityPlan> planOpt = capacityPlanRepository.findTopBySessionIdOrderByRevisionDesc(sessionId);
        planOpt.ifPresent(plan -> {
            if (newTotal > plan.getApprovedCapacity()) {
                throw new IllegalStateException("Total slot capacity (" + newTotal + ") cannot exceed approved session capacity (" + plan.getApprovedCapacity() + ")");
            }
        });

        int centerSafeLimit = lockedCenter.getSafeConcurrentOccupancy();
        int safeConcurrentOccupancy = calculationService.resolveEffectiveSafeConcurrentOccupancy(
                planOpt.map(SessionCapacityPlan::getSafeConcurrentOccupancy).orElse(null),
                centerSafeLimit
        );
        int expectedDwellMinutes = planOpt.map(SessionCapacityPlan::getExpectedDwellMinutes)
                .orElse(SessionCapacityPlan.DEFAULT_EXPECTED_DWELL_MINUTES);
        double accompanyingRatio = planOpt.map(SessionCapacityPlan::getExpectedAccompanyingPersonsPerBooking)
                .orElse(SessionCapacityPlan.DEFAULT_ACCOMPANYING_RATIO);

        List<OccupancyInterval> candidateIntervals = sessionSlots.stream()
                .map(s -> new OccupancyInterval(
                        s.getStartTime(),
                        s.getEndTime(),
                        s.getId().equals(slotId) ? newCapacity : s.getCapacity(),
                        expectedDwellMinutes,
                        accompanyingRatio
                ))
                .toList();

        OccupancyEvaluationResult occupancyResult = calculationService.evaluateConcurrentOccupancy(candidateIntervals, safeConcurrentOccupancy);
        if (!occupancyResult.safe()) {
            throw new IllegalStateException(String.format(
                    "Peak concurrent occupancy (%.1f) exceeds safe concurrent occupancy limit (%d) at %s",
                    occupancyResult.peakOccupancyUnits(),
                    occupancyResult.safeConcurrentOccupancy(),
                    occupancyResult.peakIntervalStart()
            ));
        }

        slot.setCapacity(newCapacity);
        return timeSlotRepository.save(slot);
    }

    @Transactional
    public TimeSlot updateSlotCapacity(Long sessionId, Long slotId, int newCapacity) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null");
        }
        if (slotId == null) {
            throw new IllegalArgumentException("Slot ID must not be null");
        }
        Long actualSessionId = timeSlotRepository.findSessionIdBySlotId(slotId)
                .orElseThrow(() -> new IllegalArgumentException("TimeSlot not found with ID: " + slotId));
        if (!actualSessionId.equals(sessionId)) {
            throw new IllegalArgumentException("TimeSlot " + slotId + " does not belong to session " + sessionId);
        }
        return updateSlotCapacity(slotId, newCapacity);
    }

    @Transactional(readOnly = true)
    public List<TimeSlot> getSlotsForSession(Long sessionId) {
        return timeSlotRepository.findBySessionIdOrderByStartTimeAsc(sessionId);
    }

    private void requireTimeSlotConfigurationAllowed(VaccinationSession session) {
        SessionLifecycleStatus status = session.getLifecycleStatus();
        if (status != SessionLifecycleStatus.DRAFT && status != SessionLifecycleStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot configure time slots for session in " + status + " lifecycle status");
        }
    }
}
