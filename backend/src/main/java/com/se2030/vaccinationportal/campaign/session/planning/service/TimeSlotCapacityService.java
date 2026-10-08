package com.se2030.vaccinationportal.campaign.session.planning.service;

import com.se2030.vaccinationportal.campaign.api.capacity.SlotBookability;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotReservationResult;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.session.planning.policy.SlotBookabilityStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Selects read-only booking strategies and reserves or releases capacity under database locks. */
@Service
public class TimeSlotCapacityService implements SlotCapacityPort {

    private final TimeSlotRepository timeSlotRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final VaccinationCampaignRepository campaignRepository;
    private final SlotBookabilityStrategy directBookingStrategy;
    private final SlotBookabilityStrategy waitlistPromotionStrategy;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public TimeSlotCapacityService(
            TimeSlotRepository timeSlotRepository,
            VaccinationSessionRepository sessionRepository,
            VaccinationCampaignRepository campaignRepository,
            @Qualifier("directBookingBookabilityStrategy") SlotBookabilityStrategy directBookingStrategy,
            @Qualifier("waitlistPromotionBookabilityStrategy") SlotBookabilityStrategy waitlistPromotionStrategy
    ) {
        this.timeSlotRepository = Objects.requireNonNull(timeSlotRepository, "TimeSlotRepository must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "VaccinationSessionRepository must not be null");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "VaccinationCampaignRepository must not be null");
        this.directBookingStrategy = Objects.requireNonNull(directBookingStrategy, "Direct booking strategy must not be null");
        this.waitlistPromotionStrategy = Objects.requireNonNull(waitlistPromotionStrategy, "Waitlist promotion strategy must not be null");
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

        // Lock campaign, session and slot in the same order for every reservation.
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

        // Lock campaign, session and slot in the same order for every reservation.
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

        // Recheck parent relationships after locking; the earlier reads may be stale.
        if (session.getCampaign() == null || !campaignId.equals(session.getCampaign().getId())
                || slot.getSession() == null || !sessionId.equals(slot.getSession().getId())) {
            return SlotReservationResult.NOT_BOOKABLE;
        }

        // Recheck booking rules under locks before changing capacity.
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

        // Lock campaign, session and slot in the same order for every reservation.
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

        // Recheck parent relationships after locking; the earlier reads may be stale.
        if (session.getCampaign() == null || !campaignId.equals(session.getCampaign().getId())
                || slot.getSession() == null || !sessionId.equals(slot.getSession().getId())) {
            return SlotReservationResult.NOT_BOOKABLE;
        }

        // Promotion skips direct-booking time checks, but still requires capacity.
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

        // Use the reservation lock order when releasing capacity too.
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

        // Lock the parent rows before checking all slots for remaining capacity.
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
    public Optional<Long> getCenterIdForSlot(Long slotId) {
        if (slotId == null) {
            return Optional.empty();
        }
        return timeSlotRepository.findById(slotId)
                .map(TimeSlot::getSession)
                .filter(Objects::nonNull)
                .map(VaccinationSession::getCenter)
                .filter(Objects::nonNull)
                .map(center -> center.getId());
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
        return evaluateBookability(slotId, now, directBookingStrategy);
    }

    @Override
    @Transactional(readOnly = true)
    public SlotBookability evaluateBookabilityForWaitlistPromotion(Long slotId, Instant now) {
        return evaluateBookability(slotId, now, waitlistPromotionStrategy);
    }

    private SlotBookability evaluateBookability(Long slotId, Instant now, SlotBookabilityStrategy strategy) {
        // Shared lookup failures take precedence over either booking policy.
        SlotEvaluationContext context = resolveSlotContext(slotId);
        if (context.failure() != null) {
            return context.failure();
        }
        return strategy.evaluate(context.slot(), now);
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
