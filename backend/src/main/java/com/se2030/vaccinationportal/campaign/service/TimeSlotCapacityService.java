package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.dependency.SlotReservationResult;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Authoritative MF-02 production adapter implementing MF-03's {@link SlotCapacityPort}.
 * Executes pessimistic row locking on {@link TimeSlot} rows to provide concurrency-safe
 * capacity check, reservation, and release operations within the caller's transaction.
 */
@Service
public class TimeSlotCapacityService implements SlotCapacityPort {

    private final TimeSlotRepository timeSlotRepository;
    private final VaccinationSessionRepository sessionRepository;

    public TimeSlotCapacityService(
            TimeSlotRepository timeSlotRepository,
            VaccinationSessionRepository sessionRepository
    ) {
        this.timeSlotRepository = Objects.requireNonNull(timeSlotRepository, "TimeSlotRepository must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "VaccinationSessionRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAvailableCapacity(Long slotId) {
        if (slotId == null) {
            return false;
        }
        Optional<TimeSlot> slotOpt = timeSlotRepository.findById(slotId);
        if (slotOpt.isEmpty()) {
            return false;
        }
        TimeSlot slot = slotOpt.get();
        VaccinationSession session = slot.getSession();
        if (session == null || session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            return false;
        }
        return slot.hasAvailableCapacity();
    }

    @Override
    @Transactional
    public SlotReservationResult reserveCapacity(Long slotId) {
        if (slotId == null) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        Optional<TimeSlot> slotOpt = timeSlotRepository.findByIdForUpdate(slotId);
        if (slotOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        TimeSlot slot = slotOpt.get();
        VaccinationSession session = slot.getSession();
        if (session == null || session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            return SlotReservationResult.SLOT_FULL;
        }
        if (!slot.reserve()) {
            return SlotReservationResult.SLOT_FULL;
        }
        timeSlotRepository.save(slot);
        return SlotReservationResult.SUCCESS;
    }

    @Override
    @Transactional
    public boolean releaseCapacity(Long slotId) {
        if (slotId == null) {
            return false;
        }
        Optional<TimeSlot> slotOpt = timeSlotRepository.findByIdForUpdate(slotId);
        if (slotOpt.isEmpty()) {
            return false;
        }
        TimeSlot slot = slotOpt.get();
        if (!slot.release()) {
            return false;
        }
        timeSlotRepository.save(slot);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Instant getSlotStartTime(Long slotId) {
        if (slotId == null) {
            return null;
        }
        return timeSlotRepository.findById(slotId)
                .map(TimeSlot::getStartTime)
                .orElse(null);
    }

    @Override
    @Transactional
    public boolean isSessionFull(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }

        List<TimeSlot> slots = timeSlotRepository.findBySessionIdForUpdateOrderByIdAsc(sessionId);

        if (slots.isEmpty()) {
            VaccinationSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));
            if (session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
                throw new IllegalStateException("Cannot check capacity or join waitlist for session in "
                        + session.getLifecycleStatus() + " status: " + sessionId);
            }
            throw new IllegalStateException("Session " + sessionId + " has no configured time slots");
        }

        VaccinationSession session = slots.get(0).getSession();
        if (session == null || session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            SessionLifecycleStatus status = (session != null) ? session.getLifecycleStatus() : null;
            throw new IllegalStateException("Cannot check capacity or join waitlist for session in "
                    + status + " status: " + sessionId);
        }

        return slots.stream().noneMatch(TimeSlot::hasAvailableCapacity);
    }
}
