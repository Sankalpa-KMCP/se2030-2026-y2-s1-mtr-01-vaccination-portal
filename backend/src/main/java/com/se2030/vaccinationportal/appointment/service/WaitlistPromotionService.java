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

    private Optional<Appointment> processPromotionCandidates(List<WaitlistEntry> candidates, Long slotId, Instant effectiveNow) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        for (WaitlistEntry entry : candidates) {
            Long citizenId = entry.getCitizenId();
            Long campaignId = entry.getCampaignId();

            // Step 1a: Acquire pessimistic write lock for citizen+campaign (Canonical Section 12.4)
            guardRepository.insertGuardIfAbsent(citizenId, campaignId);
            guardRepository.findByCitizenIdAndCampaignIdForUpdate(citizenId, campaignId);

            // Step 1b: Re-validate screening eligibility via EligibilityLookupPort (MF03-BR-01 & MF03-BR-07)
            if (eligibilityLookupPort != null) {
                EligibilityStatus status = eligibilityLookupPort.getCurrentEligibility(citizenId, campaignId);
                if (status != EligibilityStatus.ELIGIBLE) {
                    entry.markSkippedIneligible();
                    waitlistRepository.save(entry);
                    continue; // Skip ineligible waiter and evaluate next candidate in FIFO order
                }
            }

            // Step 1c: Re-validate active appointment uniqueness (MF03-BR-02 & MF03-BR-07)
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

            // Step 1e: Create confirmed appointment and update waitlist entry status to PROMOTED (MF03-BR-08)
            Appointment appointment = new Appointment(citizenId, campaignId, slotId, AppointmentStatus.CONFIRMED, effectiveNow);
            Appointment savedAppointment = appointmentRepository.save(appointment);

            entry.markPromoted();
            waitlistRepository.save(entry);

            return Optional.of(savedAppointment);
        }

        return Optional.empty();
    }
}
