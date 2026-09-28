package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.appointment.dependency.SlotBookability;
import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.dependency.SlotReservationResult;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
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
 * Also owns the authoritative bookability policy (R03) so reservation mutations enforce
 * the same restrictions the availability/search path advertises.
 */
@Service
public class TimeSlotCapacityService implements SlotCapacityPort {

    private final TimeSlotRepository timeSlotRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final VaccinationCampaignRepository campaignRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public TimeSlotCapacityService(
            TimeSlotRepository timeSlotRepository,
            VaccinationSessionRepository sessionRepository,
            VaccinationCampaignRepository campaignRepository
    ) {
        this.timeSlotRepository = Objects.requireNonNull(timeSlotRepository, "TimeSlotRepository must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "VaccinationSessionRepository must not be null");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "VaccinationCampaignRepository must not be null");
    }

    private void detachIfAttached(Class<?> entityClass, Object id) {
        if (entityManager != null && id != null) {
            try {
                Object entity = entityManager.find(entityClass, id);
                if (entity != null) {
                    entityManager.detach(entity);
                }
            } catch (Exception ignored) {
            }
        }
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
        Optional<Long> sessionIdOpt = timeSlotRepository.findSessionIdBySlotId(slotId);
        if (sessionIdOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        Long sessionId = sessionIdOpt.get();

        Optional<Long> campaignIdOpt = sessionRepository.findCampaignIdBySessionId(sessionId);
        if (campaignIdOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        Long campaignId = campaignIdOpt.get();

        // Hierarchical lock order: Campaign -> Session -> Slot (F1-A / F1-B)
        detachIfAttached(VaccinationCampaign.class, campaignId);
        Optional<VaccinationCampaign> campaignOpt = campaignRepository.findByIdForUpdate(campaignId);
        if (campaignOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }

        detachIfAttached(VaccinationSession.class, sessionId);
        Optional<VaccinationSession> sessionOpt = sessionRepository.findByIdForUpdate(sessionId);
        if (sessionOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        VaccinationSession session = sessionOpt.get();
        if (session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            return SlotReservationResult.SLOT_FULL;
        }

        detachIfAttached(TimeSlot.class, slotId);
        Optional<TimeSlot> slotOpt = timeSlotRepository.findByIdForUpdate(slotId);
        if (slotOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        TimeSlot slot = slotOpt.get();
        if (!slot.reserve()) {
            return SlotReservationResult.SLOT_FULL;
        }
        timeSlotRepository.save(slot);
        return SlotReservationResult.SUCCESS;
    }

    @Override
    @Transactional
    public SlotReservationResult reserveCapacityForNewBooking(Long slotId, Instant now) {
        if (slotId == null) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        Instant effectiveNow = (now != null) ? now : Instant.now();

        Optional<Long> sessionIdOpt = timeSlotRepository.findSessionIdBySlotId(slotId);
        if (sessionIdOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        Long sessionId = sessionIdOpt.get();

        Optional<Long> campaignIdOpt = sessionRepository.findCampaignIdBySessionId(sessionId);
        if (campaignIdOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        Long campaignId = campaignIdOpt.get();

        // Hierarchical lock order: Campaign -> Session -> Slot (F1-A / F1-B)
        detachIfAttached(VaccinationCampaign.class, campaignId);
        Optional<VaccinationCampaign> campaignOpt = campaignRepository.findByIdForUpdate(campaignId);
        if (campaignOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        VaccinationCampaign campaign = campaignOpt.get();

        detachIfAttached(VaccinationSession.class, sessionId);
        Optional<VaccinationSession> sessionOpt = sessionRepository.findByIdForUpdate(sessionId);
        if (sessionOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        VaccinationSession session = sessionOpt.get();

        detachIfAttached(TimeSlot.class, slotId);
        Optional<TimeSlot> slotOpt = timeSlotRepository.findByIdForUpdate(slotId);
        if (slotOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        TimeSlot slot = slotOpt.get();

        // Verify structural relationship under lock
        if (session.getCampaign() == null || !campaignId.equals(session.getCampaign().getId())
                || slot.getSession() == null || !sessionId.equals(slot.getSession().getId())) {
            return SlotReservationResult.NOT_BOOKABLE;
        }

        // Authoritative bookability evaluation under locks (R03/F1-A)
        if (!campaign.isBookingWindowOpenAt(effectiveNow)) {
            return SlotReservationResult.NOT_BOOKABLE;
        }
        if (session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            return SlotReservationResult.NOT_BOOKABLE;
        }
        if (session.isBookingSuspended()) {
            return SlotReservationResult.NOT_BOOKABLE;
        }
        if (!effectiveNow.isBefore(session.getBookingCloseAt())) {
            return SlotReservationResult.NOT_BOOKABLE;
        }
        if (!slot.getStartTime().isAfter(effectiveNow)) {
            return SlotReservationResult.NOT_BOOKABLE;
        }
        if (!slot.hasAvailableCapacity()) {
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
    public SlotReservationResult reserveCapacityForWaitlistPromotion(Long slotId, Instant now) {
        if (slotId == null) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        Instant effectiveNow = (now != null) ? now : Instant.now();

        Optional<Long> sessionIdOpt = timeSlotRepository.findSessionIdBySlotId(slotId);
        if (sessionIdOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        Long sessionId = sessionIdOpt.get();

        Optional<Long> campaignIdOpt = sessionRepository.findCampaignIdBySessionId(sessionId);
        if (campaignIdOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        Long campaignId = campaignIdOpt.get();

        // Hierarchical lock order: Campaign -> Session -> Slot (F1-A / F1-B)
        detachIfAttached(VaccinationCampaign.class, campaignId);
        Optional<VaccinationCampaign> campaignOpt = campaignRepository.findByIdForUpdate(campaignId);
        if (campaignOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        VaccinationCampaign campaign = campaignOpt.get();

        detachIfAttached(VaccinationSession.class, sessionId);
        Optional<VaccinationSession> sessionOpt = sessionRepository.findByIdForUpdate(sessionId);
        if (sessionOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        VaccinationSession session = sessionOpt.get();

        detachIfAttached(TimeSlot.class, slotId);
        Optional<TimeSlot> slotOpt = timeSlotRepository.findByIdForUpdate(slotId);
        if (slotOpt.isEmpty()) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }
        TimeSlot slot = slotOpt.get();

        // Verify structural relationship under lock
        if (session.getCampaign() == null || !campaignId.equals(session.getCampaign().getId())
                || slot.getSession() == null || !sessionId.equals(slot.getSession().getId())) {
            return SlotReservationResult.NOT_BOOKABLE;
        }

        // Waitlist promotion narrower policy under lock (F1-A)
        if (!campaign.getStatus().isIntakePermitted()) {
            return SlotReservationResult.NOT_BOOKABLE;
        }
        if (session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            return SlotReservationResult.NOT_BOOKABLE;
        }
        if (!slot.hasAvailableCapacity()) {
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
        Optional<Long> sessionIdOpt = timeSlotRepository.findSessionIdBySlotId(slotId);
        if (sessionIdOpt.isEmpty()) {
            return false;
        }
        Long sessionId = sessionIdOpt.get();

        Optional<Long> campaignIdOpt = sessionRepository.findCampaignIdBySessionId(sessionId);
        if (campaignIdOpt.isEmpty()) {
            return false;
        }
        Long campaignId = campaignIdOpt.get();

        // Hierarchical lock order: Campaign -> Session -> Slot (F1-B)
        detachIfAttached(VaccinationCampaign.class, campaignId);
        Optional<VaccinationCampaign> campaignOpt = campaignRepository.findByIdForUpdate(campaignId);
        if (campaignOpt.isEmpty()) {
            return false;
        }

        detachIfAttached(VaccinationSession.class, sessionId);
        Optional<VaccinationSession> sessionOpt = sessionRepository.findByIdForUpdate(sessionId);
        if (sessionOpt.isEmpty()) {
            return false;
        }

        detachIfAttached(TimeSlot.class, slotId);
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

        Optional<Long> campaignIdOpt = sessionRepository.findCampaignIdBySessionId(sessionId);
        if (campaignIdOpt.isPresent()) {
            campaignRepository.findByIdForUpdate(campaignIdOpt.get());
        }

        // Hierarchical lock order: Campaign -> Session -> Slots (F1-B)
        Optional<VaccinationSession> sessionOpt = sessionRepository.findByIdForUpdate(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Session not found with ID: " + sessionId);
        }
        VaccinationSession session = sessionOpt.get();
        if (session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot check capacity or join waitlist for session in "
                    + session.getLifecycleStatus() + " status: " + sessionId);
        }

        List<TimeSlot> slots = timeSlotRepository.findBySessionIdForUpdateOrderByIdAsc(sessionId);
        if (slots.isEmpty()) {
            throw new IllegalStateException("Session " + sessionId + " has no configured time slots");
        }

        return slots.stream().noneMatch(TimeSlot::hasAvailableCapacity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotInCampaign(Long slotId, Long campaignId) {
        if (slotId == null || campaignId == null) {
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
        VaccinationCampaign campaign = session.getCampaign();
        if (campaign == null || campaign.getId() == null) {
            return false;
        }
        return campaignId.equals(campaign.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> getSessionIdForSlot(Long slotId) {
        if (slotId == null) {
            return Optional.empty();
        }
        Optional<TimeSlot> slotOpt = timeSlotRepository.findById(slotId);
        if (slotOpt.isEmpty()) {
            return Optional.empty();
        }
        TimeSlot slot = slotOpt.get();
        VaccinationSession session = slot.getSession();
        if (session == null || session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            return Optional.empty();
        }
        return Optional.ofNullable(session.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> getCampaignIdForSession(Long sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }
        return sessionRepository.findById(sessionId)
                .filter(session -> session.getLifecycleStatus() == SessionLifecycleStatus.SCHEDULED)
                .filter(session -> session.getCampaign() != null && session.getCampaign().getId() != null)
                .map(session -> session.getCampaign().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public SlotBookability evaluateBookabilityForNewBooking(Long slotId, Instant now) {
        SlotEvaluationContext context = resolveSlotContext(slotId);
        if (context.failure() != null) {
            return context.failure();
        }
        VaccinationSession session = context.slot().getSession();
        VaccinationCampaign campaign = session.getCampaign();

        // Full booking policy — identical to the search/availability path (R03)
        if (!campaign.isBookingWindowOpenAt(now)) {
            return SlotBookability.CAMPAIGN_BOOKING_NOT_OPEN;
        }
        if (session.isBookingSuspended()) {
            return SlotBookability.SESSION_BOOKING_SUSPENDED;
        }
        if (!now.isBefore(session.getBookingCloseAt())) {
            return SlotBookability.SESSION_BOOKING_CLOSED;
        }
        if (!context.slot().getStartTime().isAfter(now)) {
            return SlotBookability.SLOT_IN_PAST;
        }
        if (!context.slot().hasAvailableCapacity()) {
            return SlotBookability.NO_CAPACITY;
        }
        return SlotBookability.BOOKABLE;
    }

    @Override
    @Transactional(readOnly = true)
    public SlotBookability evaluateBookabilityForWaitlistPromotion(Long slotId, Instant now) {
        SlotEvaluationContext context = resolveSlotContext(slotId);
        if (context.failure() != null) {
            return context.failure();
        }
        VaccinationSession session = context.slot().getSession();
        VaccinationCampaign campaign = session.getCampaign();

        // Promotion fulfils an already-permitted queue join. Deliberately NOT enforced here
        // (documented domain distinction): session booking suspension, session booking cutoff,
        // and future-slot-time — cancellations and no-shows free capacity at/after slot start,
        // past the cutoff, and that capacity must stay reachable by the FIFO queue (F04/F05).
        // Campaign operational status IS enforced: an operationally closed campaign must not
        // create new confirmed appointments via promotion.
        if (!campaign.getStatus().isIntakePermitted()) {
            return SlotBookability.CAMPAIGN_BOOKING_NOT_OPEN;
        }
        return SlotBookability.BOOKABLE;
    }

    private SlotEvaluationContext resolveSlotContext(Long slotId) {
        if (slotId == null) {
            return new SlotEvaluationContext(null, SlotBookability.SLOT_NOT_FOUND);
        }
        Optional<TimeSlot> slotOpt = timeSlotRepository.findById(slotId);
        if (slotOpt.isEmpty()) {
            return new SlotEvaluationContext(null, SlotBookability.SLOT_NOT_FOUND);
        }
        TimeSlot slot = slotOpt.get();
        VaccinationSession session = slot.getSession();
        if (session == null) {
            return new SlotEvaluationContext(null, SlotBookability.SLOT_NOT_FOUND);
        }
        if (session.getLifecycleStatus() != SessionLifecycleStatus.SCHEDULED) {
            return new SlotEvaluationContext(null, SlotBookability.SESSION_NOT_SCHEDULED);
        }
        VaccinationCampaign campaign = session.getCampaign();
        if (campaign == null || campaign.getId() == null) {
            return new SlotEvaluationContext(null, SlotBookability.SLOT_NOT_FOUND);
        }
        return new SlotEvaluationContext(slot, null);
    }

    /**
     * Internal slot resolution result shared by the bookability evaluation methods.
     *
     * @param slot    resolved slot when resolvable
     * @param failure terminal failure reason when the slot chain cannot be evaluated
     */
    private record SlotEvaluationContext(TimeSlot slot, SlotBookability failure) {
    }
}
