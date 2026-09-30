package com.se2030.vaccinationportal.appointment.waitlist.service;

import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityLookupPort;
import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityStatus;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.exception.DuplicateActiveAppointmentException;
import com.se2030.vaccinationportal.appointment.exception.DuplicateWaitlistEntryException;
import com.se2030.vaccinationportal.appointment.exception.NotEligibleException;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating citizen waitlist entry creation (MF03-BR-06 / MF03-BR-14).
 * <p>
 * Enforces pessimistic guard-row locking, campaign eligibility checks (MF03-BR-01), active appointment incompatibility checks
 * (MF03-BR-02), active waitlist uniqueness checks (MF03-BR-14), and transactional entity persistence within ONE {@link Transactional} boundary.
 * </p>
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-05: Citizen joins a waitlist when a vaccination session is full.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced:</b>
 * <ul>
 *   <li>MF03-BR-01: Waitlist join requires a currently valid ELIGIBLE screening assessment for the campaign.</li>
 *   <li>MF03-BR-02 &amp; MF03-BR-14: Citizen cannot join a waitlist if they already hold an active appointment
 *       (CONFIRMED or CHECKED_IN) for the same campaign.</li>
 *   <li>MF03-BR-06: Allowed only when all time slots of the session have zero remaining capacity;
 *       at most one active WAITING entry per citizen per campaign.</li>
 *   <li>R01 campaign/session integrity: the requested session's authoritative campaign (resolved via
 *       the {@link com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort}) must match the
 *       requested campaign before any queue or capacity effects.</li>
 * </ul>
 */
@Service
@Transactional
public class WaitlistJoinService {

    private final WaitlistRepository waitlistRepository;
    private final AppointmentRepository appointmentRepository;
    private final CitizenCampaignBookingGuardRepository guardRepository;
    private final EligibilityLookupPort eligibilityLookupPort;
    private final SlotCapacityPort slotCapacityPort;

    @Autowired
    public WaitlistJoinService(
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
     * Joins a session waitlist for an eligible citizen (accepting Instant).
     * <p>
     * <b>Viva / Sequencing Rationale ("Why this specific order"):</b>
     * <ol>
     *   <li><b>Acquire Guard-Row Lock:</b> Pessimistic write lock ({@code SELECT ... FOR UPDATE}) on
     *       {@link com.se2030.vaccinationportal.appointment.persistence.model.CitizenCampaignBookingGuard}. Must be acquired
     *       first to serialize concurrent actions and prevent a citizen from booking an appointment while simultaneously
     *       joining a waitlist.</li>
     *   <li><b>Verify MF-01 Screening Eligibility (MF03-BR-01):</b> Verified up front to avoid queueing citizens
     *       who are not medically cleared.</li>
     *   <li><b>Verify Active Appointment Absence (MF03-BR-02, MF03-BR-14):</b> A citizen cannot hold a waitlist entry
     *       if they already have a CONFIRMED or CHECKED_IN appointment for the campaign.</li>
     *   <li><b>Verify Active Waitlist Absence (MF03-BR-06, MF03-BR-14):</b> Only one active WAITING entry per citizen
     *       per campaign.</li>
     *   <li><b>Confirm Session Is Truly Full (MF03-BR-06):</b> If bookable capacity still exists in any slot of the session,
     *       the citizen must book directly instead of joining a waitlist.</li>
     *   <li><b>Persist WaitlistEntry:</b> Saves the entry with {@link WaitlistStatus#WAITING} and the arrival timestamp.</li>
     * </ol>
     * </p>
     *
     * @param citizenId  Identifier of the citizen joining the waitlist
     * @param campaignId Identifier of the targeted campaign
     * @param sessionId  Identifier of the targeted session
     * @param now        Current request timestamp
     * @return Created {@link WaitlistEntry} in WAITING status
     */
    public WaitlistEntry joinWaitlist(Long citizenId, Long campaignId, Long sessionId, Instant now) {
        if (citizenId == null) {
            throw new IllegalArgumentException("Citizen ID is required");
        }
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }
        Instant effectiveNow = (now != null) ? now : Instant.now();

        // Step 2a-0: Authoritative campaign/session consistency (R01).
        // Campaign identity and session identity are never trusted merely because each exists: the
        // session's authoritative campaign (resolved from MF-02 backend data) must equal the requested
        // campaign. A mismatched pair is rejected BEFORE the guard-row insert, eligibility checks, or
        // any queue/capacity effects, so no waitlist entry can ever represent a non-authoritative
        // campaign/session pairing. Sessions that cannot be resolved to a campaign (nonexistent or not
        // scheduled) fall through unchanged to the authoritative session-state validation below, which
        // preserves the pre-existing error contract for those cases.
        if (slotCapacityPort == null) {
            throw new IllegalStateException("SlotCapacityPort implementation (MF-02) is unavailable");
        }
        Optional<Long> authoritativeCampaignId = slotCapacityPort.getCampaignIdForSession(sessionId);
        if (authoritativeCampaignId.isPresent() && !campaignId.equals(authoritativeCampaignId.get())) {
            throw new IllegalArgumentException("Session " + sessionId + " does not belong to campaign " + campaignId);
        }

        // Step 2a: Acquire pessimistic write lock on CitizenCampaignBookingGuard (Canonical Section 12.4).
        // Viva note: Must be acquired first to prevent race conditions against concurrent booking or promotion threads.
        guardRepository.insertGuardIfAbsent(citizenId, campaignId);
        guardRepository.findByCitizenIdAndCampaignIdForUpdate(citizenId, campaignId)
                .orElseThrow(() -> new IllegalStateException("Failed to acquire booking guard lock for citizen "
                        + citizenId + " and campaign " + campaignId));

        // Step 2b: MF03-BR-01 - Check eligibility via EligibilityLookupPort
        if (eligibilityLookupPort == null) {
            throw new IllegalStateException("EligibilityLookupPort implementation (MF-01) is unavailable");
        }
        EligibilityStatus eligibilityStatus = eligibilityLookupPort.getCurrentEligibility(citizenId, campaignId);
        if (eligibilityStatus != EligibilityStatus.ELIGIBLE) {
            throw new NotEligibleException("Citizen " + citizenId + " is not eligible for campaign "
                    + campaignId + ". Current status: " + eligibilityStatus);
        }

        // Step 2c: MF03-BR-02 / MF03-BR-14 - Reject if citizen already has an active appointment for this campaign.
        // Viva note: A citizen cannot hold an active booking and waitlist entry for the same campaign simultaneously.
        List<Appointment> activeAppointments = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                citizenId, campaignId, List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN)
        );
        if (!activeAppointments.isEmpty()) {
            throw new DuplicateActiveAppointmentException("Citizen " + citizenId
                    + " already has an active appointment for campaign " + campaignId);
        }

        // Step 2d: MF03-BR-06 / MF03-BR-14 - Reject if citizen already has an active WAITING waitlist entry for this campaign.
        // Viva note: Ensures strict single-waitlist policy per citizen per campaign.
        Optional<WaitlistEntry> existingWaiting = waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(
                citizenId, campaignId, WaitlistStatus.WAITING
        );
        if (existingWaiting.isPresent()) {
            throw new DuplicateWaitlistEntryException("Citizen " + citizenId
                    + " already has an active WAITING waitlist entry for campaign " + campaignId);
        }

        // Step 2e: Confirm via SlotCapacityPort whether session is full (MF03-BR-06).
        // Viva note: Waitlists are strictly overflow queues. If any slot has free capacity, citizen must book directly.
        if (slotCapacityPort != null && !slotCapacityPort.isSessionFull(sessionId)) {
            throw new IllegalStateException("Cannot join waitlist when session " + sessionId + " still has available booking capacity");
        }

        // Step 2f: Persist new WaitlistEntry in WAITING status with joined timestamp for FIFO ordering.
        WaitlistEntry entry = new WaitlistEntry(citizenId, campaignId, sessionId, WaitlistStatus.WAITING, effectiveNow);
        return waitlistRepository.save(entry);
    }

    /**
     * Overload accepting {@link LocalDateTime}.
     */
    public WaitlistEntry joinWaitlist(Long citizenId, Long campaignId, Long sessionId, LocalDateTime now) {
        Instant instantNow = (now != null) ? now.toInstant(ZoneOffset.UTC) : Instant.now();
        return joinWaitlist(citizenId, campaignId, sessionId, instantNow);
    }
}
