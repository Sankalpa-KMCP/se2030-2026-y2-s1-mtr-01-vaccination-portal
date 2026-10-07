package com.se2030.vaccinationportal.appointment.waitlist.service;

import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityLookupPort;
import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityStatus;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotBookability;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotReservationResult;
import com.se2030.vaccinationportal.appointment.event.AppointmentEvent;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.shared.auth.api.EmailVerificationStatusReader;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating automatic waitlist promotion workflows when session capacity opens (MF03-BR-07 / MF03-BR-08).
 * <p>
 * Evaluates waiting entries in deterministic FIFO order (joinedAt ascending, then id ascending). Rechecks screening eligibility
 * via {@link EligibilityLookupPort} (MF03-BR-01) and active appointment rules (MF03-BR-02). Ineligible or duplicate waiters are skipped
 * and marked with terminal status ({@link WaitlistStatus#SKIPPED_INELIGIBLE} or {@link WaitlistStatus#SKIPPED_DUPLICATE_APPOINTMENT}).
 * The first eligible waiter without an active appointment is atomically promoted into a new {@link Appointment} in {@link AppointmentStatus#CONFIRMED}
 * status while their waitlist entry status transitions to {@link WaitlistStatus#PROMOTED}.
 * </p>
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-05: Automatic promotion of queued citizens when appointment slots are freed.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced:</b>
 * <ul>
 *   <li>MF03-BR-01 &amp; MF03-BR-07: Promotion rechecks screening eligibility with MF-01; expired or non-eligible entries
 *       are skipped and marked {@code SKIPPED_INELIGIBLE}.</li>
 *   <li>MF03-BR-02 &amp; MF03-BR-07: Entries holding an active appointment elsewhere in the campaign are skipped
 *       and marked {@code SKIPPED_DUPLICATE_APPOINTMENT}.</li>
 *   <li>Verified-email rule: entries whose citizen has no verified email are temporarily passed over — the
 *       entry stays {@code WAITING} with its original arrival order, and the FIFO loop continues with the
 *       next candidate. No capacity is reserved for an unverified candidate.</li>
 *   <li>MF03-BR-06: Strict deterministic FIFO candidate ordering ({@code joined_at} ASC, then {@code id} ASC).</li>
 *   <li>MF03-BR-08: Successful promotion creates confirmed appointment and marks waitlist entry {@code PROMOTED}.</li>
 * </ul>
 */
@Service
@Transactional
public class WaitlistPromotionService {

    private final ApplicationEventPublisher eventPublisher;

    private final WaitlistRepository waitlistRepository;
    private final AppointmentRepository appointmentRepository;
    private final CitizenCampaignBookingGuardRepository guardRepository;
    private final EligibilityLookupPort eligibilityLookupPort;
    private final SlotCapacityPort slotCapacityPort;
    private final EmailVerificationStatusReader emailVerificationStatusReader;

    @Autowired
    public WaitlistPromotionService(
            ApplicationEventPublisher eventPublisher,
            WaitlistRepository waitlistRepository,
            AppointmentRepository appointmentRepository,
            CitizenCampaignBookingGuardRepository guardRepository,
            @Autowired(required = false) EligibilityLookupPort eligibilityLookupPort,
            @Autowired(required = false) SlotCapacityPort slotCapacityPort,
            EmailVerificationStatusReader emailVerificationStatusReader
    ) {
        this.eventPublisher = eventPublisher;
        this.waitlistRepository = waitlistRepository;
        this.appointmentRepository = appointmentRepository;
        this.guardRepository = guardRepository;
        this.eligibilityLookupPort = eligibilityLookupPort;
        this.slotCapacityPort = slotCapacityPort;
        this.emailVerificationStatusReader = emailVerificationStatusReader;
    }

    /**
     * Promotes the earliest valid WAITING entry for a specific session into an open time slot (accepting Instant).
     *
     * @param sessionId Identifier of the session with open capacity
     * @param slotId    Identifier of the target time slot
     * @param now       Timestamp of the promotion request
     * @return {@link Optional} containing newly created {@link Appointment} if promotion succeeded; empty otherwise
     */
    public Optional<Appointment> promoteNext(Long sessionId, Long slotId, Instant now) {
        if (sessionId == null || slotId == null) {
            return Optional.empty();
        }
        Instant effectiveNow = (now != null) ? now : Instant.now();

        // Request-level campaign/session/slot consistency (R01): the freed slot must authoritatively
        // belong to the requested session. Promotion is strictly session-scoped, so a slot that cannot
        // be resolved to this session must never receive a promoted appointment.
        if (slotCapacityPort != null) {
            Optional<Long> authoritativeSlotSession = slotCapacityPort.getSessionIdForSlot(slotId);
            if (authoritativeSlotSession.isEmpty() || !sessionId.equals(authoritativeSlotSession.get())) {
                return Optional.empty();
            }
        }

        List<WaitlistEntry> waitingEntries = waitlistRepository.findBySessionIdAndStatusOrderByJoinedAtAsc(
                sessionId, WaitlistStatus.WAITING);

        return processPromotionCandidates(waitingEntries, slotId, effectiveNow);
    }

    /**
     * Overload accepting {@link LocalDateTime}.
     */
    public Optional<Appointment> promoteNext(Long sessionId, Long slotId, LocalDateTime now) {
        Instant instantNow = (now != null) ? now.toInstant(ZoneOffset.UTC) : Instant.now();
        return promoteNext(sessionId, slotId, instantNow);
    }

    /**
     * Promotes the earliest valid WAITING entry for the session of a freed time slot (MF03-BR-07 / F04).
     * <p>
     * Resolves the authoritative session for {@code slotId} and verifies campaign-slot integrity via
     * {@link SlotCapacityPort}. Promotion is strictly scoped to the exact session of the freed slot,
     * maintaining deterministic FIFO arrival ordering (MF03-BR-06) within that session.
     * If the slot does not belong to the campaign, cannot be resolved, or has no eligible waiters,
     * returns {@link Optional#empty()} safely without promoting anyone.
     * </p>
     *
     * @param campaignId Identifier of the campaign
     * @param slotId     Identifier of the target time slot
     * @param now        Timestamp of promotion
     * @return {@link Optional} containing created {@link Appointment} if promotion succeeded; empty otherwise
     */
    public Optional<Appointment> promoteNextForSlot(Long campaignId, Long slotId, Instant now) {
        if (campaignId == null || slotId == null || slotCapacityPort == null) {
            return Optional.empty();
        }
        Instant effectiveNow = (now != null) ? now : Instant.now();

        // Step 1: Verify freed slot belongs to the supplied campaign and is in operational state (F03 integrity)
        if (!slotCapacityPort.isSlotInCampaign(slotId, campaignId)) {
            return Optional.empty();
        }

        // Step 2: Resolve the authoritative session for the freed slot (F04 session-scoping)
        Optional<Long> sessionIdOpt = slotCapacityPort.getSessionIdForSlot(slotId);
        if (sessionIdOpt.isEmpty()) {
            return Optional.empty();
        }

        // Step 3: Delegate to session-scoped promotion path to enforce exact session waitlist scoping
        return promoteNext(sessionIdOpt.get(), slotId, effectiveNow);
    }

    /**
     * Overload for {@link #promoteNextForSlot(Long, Long, Instant)} accepting {@link LocalDateTime}.
     */
    public Optional<Appointment> promoteNextForSlot(Long campaignId, Long slotId, LocalDateTime now) {
        Instant instantNow = (now != null) ? now.toInstant(ZoneOffset.UTC) : Instant.now();
        return promoteNextForSlot(campaignId, slotId, instantNow);
    }

    /**
     * Iterates over FIFO waitlist candidates and promotes the first candidate that passes all clinical
     * and concurrency checks (MF03-BR-06, MF03-BR-07, MF03-BR-08).
     * <p>
     * <b>Viva Probe ("Why re-check eligibility and duplicates during promotion?"):</b><br>
     * <ul>
     *   <li><b>Eligibility Re-Check:</b> Waitlist entries can sit in queue for days or weeks. A citizen's clinical
     *       eligibility is not permanent — their assessment may have expired or been revoked. Allocating a physical dose
     *       to an unverified citizen violates vaccination safety protocols.</li>
     *   <li><b>Duplicate Re-Check:</b> While waiting, the citizen might have booked another appointment in the same
     *       campaign. Promoting them would violate the single active booking rule (MF03-BR-02).</li>
     *   <li><b>Skipping &amp; Progressing:</b> When a candidate fails either check, their entry is marked with a terminal
     *       skip status ({@code SKIPPED_INELIGIBLE} or {@code SKIPPED_DUPLICATE_APPOINTMENT}) and the loop immediately
     *       proceeds to the next waiter in FIFO order without aborting the promotion process.</li>
     * </ul>
     * </p>
     */
    private Optional<Appointment> processPromotionCandidates(List<WaitlistEntry> candidates, Long slotId, Instant effectiveNow) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        // Authoritative bookability of the destination slot (R03). Promotion follows a deliberate,
        // narrower policy than direct booking (see SlotCapacityPort#evaluateBookabilityForWaitlistPromotion):
        // session lifecycle and campaign intake status are enforced, while booking suspension, booking
        // cutoff, and future-slot-time are not, because freed capacity from cancellations and no-shows
        // occurs at/after slot start and must stay reachable by the FIFO queue. A non-bookable slot
        // stops the promotion attempt without touching any queue entry or capacity counter.
        if (slotCapacityPort != null) {
            SlotBookability bookability = slotCapacityPort.evaluateBookabilityForWaitlistPromotion(slotId, effectiveNow);
            if (bookability != SlotBookability.BOOKABLE) {
                return Optional.empty();
            }
        }

        for (WaitlistEntry entry : candidates) {
            Long citizenId = entry.getCitizenId();
            Long campaignId = entry.getCampaignId();

            // Step 1a-0: Structural campaign/session integrity of the queue entry itself (R01, F3).
            // A malformed historical entry whose campaign does not authoritatively match its session
            // must never be promoted: it would create an appointment pairing the entry's campaign with
            // capacity from another campaign's session. The entry is marked SKIPPED_MALFORMED and saved,
            // which sets active_campaign_id to NULL, releasing the unique constraint and queue position
            // without blocking FIFO promotion of valid waiters.
            if (slotCapacityPort != null) {
                Optional<Long> authoritativeCampaign = slotCapacityPort.getCampaignIdForSession(entry.getSessionId());
                if (authoritativeCampaign.isEmpty() || !campaignId.equals(authoritativeCampaign.get())) {
                    entry.markSkippedMalformed();
                    waitlistRepository.save(entry);
                    continue;
                }
            }

            // Serialize booking, waitlist join, and promotion for the same citizen and campaign.
            // Lock before checking duplicates so concurrent requests cannot both pass the check.
            guardRepository.insertGuardIfAbsent(citizenId, campaignId);
            guardRepository.findByCitizenIdAndCampaignIdForUpdate(citizenId, campaignId);

            // Step 1a-1: Re-check email verification at promotion time (verified-email rule).
            // Viva note: Verification is account state that can change after joining (false -> true;
            // account email is immutable so it cannot regress). An unverified candidate is a TEMPORARY
            // promotion-ineligibility, not a disqualification: the entry stays WAITING with its original
            // joinedAt and queue position, no capacity is reserved, and the FIFO loop continues so a
            // later verified candidate can still receive the freed capacity. Unlike the clinical and
            // duplicate checks below, this check runs FIRST so an unverified candidate is never
            // terminally marked for what may resolve itself once they verify.
            if (!emailVerificationStatusReader.isEmailVerified(citizenId)) {
                continue;
            }

            // Step 1b: Re-validate screening eligibility via EligibilityLookupPort (MF03-BR-01 & MF03-BR-07).
            // Viva note: If medical clearance expired or was revoked in MF-01, mark SKIPPED_INELIGIBLE and continue FIFO loop.
            if (eligibilityLookupPort != null) {
                EligibilityStatus status = eligibilityLookupPort.getCurrentEligibility(citizenId, campaignId);
                if (status != EligibilityStatus.ELIGIBLE) {
                    entry.markSkippedIneligible();
                    waitlistRepository.save(entry);
                    continue; // Skip ineligible waiter and evaluate next candidate in FIFO order
                }
            }

            // Step 1c: Re-validate active appointment uniqueness (MF03-BR-02 & MF03-BR-07).
            // Viva note: If citizen booked another slot while waiting, mark SKIPPED_DUPLICATE_APPOINTMENT and continue.
            List<Appointment> activeAppts = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                    citizenId, campaignId, List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN));
            if (!activeAppts.isEmpty()) {
                entry.markSkippedDuplicate();
                waitlistRepository.save(entry);
                continue; // Skip waiter with active appointment and evaluate next candidate
            }

            // Step 1d: Reserve capacity place on slotId with promotion bookability check under lock (F1)
            if (slotCapacityPort != null) {
                SlotReservationResult res = slotCapacityPort.reserveCapacityForWaitlistPromotion(slotId, effectiveNow);
                if (res != SlotReservationResult.SUCCESS) {
                    // Capacity could not be reserved (e.g. slot full or error), stop promotion attempt
                    return Optional.empty();
                }
            }

            // Step 1e: Create confirmed appointment and update waitlist entry status to PROMOTED (MF03-BR-08).
            // Viva note: Both entities are persisted within the same atomic transaction.
            Appointment appointment = new Appointment(citizenId, campaignId, slotId, AppointmentStatus.CONFIRMED, effectiveNow);
            Appointment savedAppointment = appointmentRepository.save(appointment);

            entry.markPromoted();
            waitlistRepository.save(entry);

            eventPublisher.publishEvent(AppointmentEvent.from(
                    AppointmentEvent.Type.WAITLIST_PROMOTED, savedAppointment));
            return Optional.of(savedAppointment);
        }

        return Optional.empty();
    }
}
