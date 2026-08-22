package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Service for managing MF-02 TimeSlots within vaccination sessions.
 * Enforces session approved capacity bounds and booking floor validation.
 */
@Service
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final SessionCapacityPlanRepository capacityPlanRepository;

    public TimeSlotService(
            TimeSlotRepository timeSlotRepository,
            VaccinationSessionRepository sessionRepository,
            SessionCapacityPlanRepository capacityPlanRepository
    ) {
        this.timeSlotRepository = Objects.requireNonNull(timeSlotRepository, "TimeSlotRepository must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "VaccinationSessionRepository must not be null");
        this.capacityPlanRepository = Objects.requireNonNull(capacityPlanRepository, "SessionCapacityPlanRepository must not be null");
    }

    @Transactional
    public TimeSlot createSlot(Long sessionId, Instant startTime, Instant endTime, int capacity) {
        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));

        if (startTime.isBefore(session.getStartAt()) || endTime.isAfter(session.getEndAt())) {
            throw new IllegalArgumentException("Slot interval must fall within session operating window");
        }

        Integer currentSum = timeSlotRepository.sumCapacityBySessionId(sessionId);
        int newTotal = (currentSum != null ? currentSum : 0) + capacity;

        capacityPlanRepository.findTopBySessionIdOrderByRevisionDesc(sessionId).ifPresent(plan -> {
            if (newTotal > plan.getApprovedCapacity()) {
                throw new IllegalStateException("Total slot capacity (" + newTotal + ") cannot exceed approved session capacity (" + plan.getApprovedCapacity() + ")");
            }
        });

        TimeSlot slot = new TimeSlot(session, startTime, endTime, capacity);
        return timeSlotRepository.save(slot);
    }

    @Transactional
    public TimeSlot updateSlotCapacity(Long slotId, int newCapacity) {
        if (slotId == null) {
            throw new IllegalArgumentException("Slot ID must not be null");
        }
        TimeSlot initialSlot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("TimeSlot not found with ID: " + slotId));

        Long sessionId = initialSlot.getSession().getId();
        VaccinationSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));

        TimeSlot slot = timeSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("TimeSlot not found with ID: " + slotId));

        if (!slot.getSession().getId().equals(session.getId())) {
            throw new IllegalStateException("TimeSlot session mismatch");
        }

        if (newCapacity < slot.getReservedCount()) {
            throw new IllegalStateException("Cannot reduce slot capacity (" + newCapacity + ") below existing reserved bookings (" + slot.getReservedCount() + ")");
        }

        Integer currentSum = timeSlotRepository.sumCapacityBySessionId(sessionId);
        int diff = newCapacity - slot.getCapacity();
        int newTotal = (currentSum != null ? currentSum : 0) + diff;

        capacityPlanRepository.findTopBySessionIdOrderByRevisionDesc(sessionId).ifPresent(plan -> {
            if (newTotal > plan.getApprovedCapacity()) {
                throw new IllegalStateException("Total slot capacity (" + newTotal + ") cannot exceed approved session capacity (" + plan.getApprovedCapacity() + ")");
            }
        });

        slot.setCapacity(newCapacity);
        return timeSlotRepository.save(slot);
    }

    @Transactional(readOnly = true)
    public List<TimeSlot> getSlotsForSession(Long sessionId) {
        return timeSlotRepository.findBySessionIdOrderByStartTimeAsc(sessionId);
    }
}
