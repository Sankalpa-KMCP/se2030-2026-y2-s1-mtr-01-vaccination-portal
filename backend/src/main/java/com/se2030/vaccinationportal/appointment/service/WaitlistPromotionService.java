package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityLookupPort;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.dependency.SlotReservationResult;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
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
 *   <li>MF03-BR-06: Strict deterministic FIFO candidate ordering ({@code joined_at} ASC, then {@code id} ASC).</li>
 *   <li>MF03-BR-08: Successful promotion creates confirmed appointment and marks waitlist entry {@code PROMOTED}.</li>
 * </ul>
 */
@Service
@Transactional
public class WaitlistPromotionService {

    private final WaitlistRepository waitlistRepository;
    private final AppointmentRepository appointmentRepository;
    private final CitizenCampaignBookingGuardRepository guardRepository;
    private final EligibilityLookupPort eligibilityLookupPort;
    private final SlotCapacityPort slotCapacityPort;

    @Autowired
    public WaitlistPromotionService(
            WaitlistRepository waitlistRepository,
            AppointmentRepository appointmentRepository,
            CitizenCampaignBookingGuardRepository guardRepository,
            @Autowired(required = false) EligibilityLookupPort eligibilityLookupPort,
            @Autowired(required = false) SlotCapacityPort slotCapacityPort
    ) {
        this.waitlistRepository = waitlistRepository;
        this.appointmentRepository = appointmentRepository;
        this.guardRepository = guardRepository;
        this.eligibilityLookupPort = eligibilityLookupPort;
        this.slotCapacityPort = slotCapacityPort;
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
     * Promotes the earliest valid WAITING entry for a campaign into an open time slot when session ID is not directly specified.
     *
     * @param campaignId Identifier of the campaign
     * @param slotId     Identifier of the target time slot
     * @param now        Timestamp of promotion
     * @return {@link Optional} containing created {@link Appointment} if promotion succeeded; empty otherwise
     */
    public Optional<Appointment> promoteNextForSlot(Long campaignId, Long slotId, Instant now) {
        if (campaignId == null || slotId == null) {
            return Optional.empty();
        }
        Instant effectiveNow = (now != null) ? now : Instant.now();

        List<WaitlistEntry> waitingEntries = waitlistRepository.findByCampaignIdAndStatusOrderByJoinedAtAsc(
                campaignId, WaitlistStatus.WAITING);

        return processPromotionCandidates(waitingEntries, slotId, effectiveNow);
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

        for (WaitlistEntry entry : candidates) {
            Long citizenId = entry.getCitizenId();
            Long campaignId = entry.getCampaignId();

            // Serialize booking, waitlist join, and promotion for the same citizen and campaign.
            // Lock before checking duplicates so concurrent requests cannot both pass the check.
            guardRepository.insertGuardIfAbsent(citizenId, campaignId);
            guardRepository.findByCitizenIdAndCampaignIdForUpdate(citizenId, campaignId);

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

            // Step 1d: Reserve capacity place on slotId
            if (slotCapacityPort != null) {
                SlotReservationResult res = slotCapacityPort.reserveCapacity(slotId);
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

            return Optional.of(savedAppointment);
        }

        return Optional.empty();
    }
}
