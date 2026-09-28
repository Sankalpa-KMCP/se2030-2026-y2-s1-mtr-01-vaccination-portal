package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityLookupPort;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.dependency.SlotBookability;
import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.dependency.SlotReservationResult;
import com.se2030.vaccinationportal.appointment.exception.DuplicateActiveAppointmentException;
import com.se2030.vaccinationportal.appointment.exception.DuplicateWaitlistEntryException;
import com.se2030.vaccinationportal.appointment.exception.NotEligibleException;
import com.se2030.vaccinationportal.appointment.exception.SlotFullException;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating atomic appointment booking workflows for citizens.
 * Enforces guard-row locking, cross-module eligibility lookup, active appointment duplicate checks,
 * active waitlist duplicate checks (Policy A), and time-slot capacity reservations within one transactional boundary.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Citizen search and atomic booking of an available appointment slot.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced:</b>
 * <ul>
 *   <li>MF03-BR-01: Booking requires a currently valid ELIGIBLE assessment for the same campaign.</li>
 *   <li>MF03-BR-02: Exactly one active appointment per citizen per campaign is enforced.</li>
 *   <li>MF03-BR-03: Capacity allocation is atomic and prevents two concurrent users taking the last place.</li>
 *   <li>MF03-BR-14 / Policy A: Dual holdings prohibited; active waitlist entry precludes direct booking.</li>
 * </ul>
 */
@Service
@Transactional
public class AppointmentBookingService {

    private final AppointmentRepository appointmentRepository;
    private final WaitlistRepository waitlistRepository;
    private final CitizenCampaignBookingGuardRepository guardRepository;
    private final EligibilityLookupPort eligibilityLookupPort;
    private final SlotCapacityPort slotCapacityPort;

    @Autowired
    public AppointmentBookingService(
            AppointmentRepository appointmentRepository,
            WaitlistRepository waitlistRepository,
            CitizenCampaignBookingGuardRepository guardRepository,
            @Autowired(required = false) EligibilityLookupPort eligibilityLookupPort,
            @Autowired(required = false) SlotCapacityPort slotCapacityPort
    ) {
        this.appointmentRepository = appointmentRepository;
        this.waitlistRepository = waitlistRepository;
        this.guardRepository = guardRepository;
        this.eligibilityLookupPort = eligibilityLookupPort;
        this.slotCapacityPort = slotCapacityPort;
    }

    /**
     * Atomically books an appointment for a citizen in a target vaccination campaign time slot.
     * <p>
     * <b>Viva / Sequencing Rationale ("Why this specific order"):</b>
     * <ol>
     *   <li><b>Acquire Guard-Row Lock:</b> Pessimistic write lock ({@code SELECT ... FOR UPDATE}) on
     *       {@link com.se2030.vaccinationportal.appointment.model.CitizenCampaignBookingGuard} for
     *       {@code (citizenId, campaignId)}. This mutex must be acquired <i>before</i> any checks so that
     *       two concurrent booking or waitlist-promotion threads for the same citizen cannot both pass the
     *       duplicate check simultaneously.</li>
     *   <li><b>Verify MF-01 Eligibility (MF03-BR-01):</b> Verified before calling MF-02 slot reservation so
     *       we do not mutate/decrement physical slot capacity for an ineligible citizen.</li>
     *   <li><b>Check Active Appointment Duplicates (MF03-BR-02):</b> Verified before slot reservation to prevent
     *       wasting capacity if the citizen already has a confirmed or checked-in booking.</li>
     *   <li><b>Check Active Waitlist Duplicates (MF03-BR-14 / Policy A):</b> Verified before slot reservation to prevent
     *       dual active holdings. Citizen must explicitly leave the waitlist before direct booking.</li>
     *   <li><b>Verify Slot Belongs to Campaign:</b> Verifies that the requested time slot belongs to the target campaign
     *       before allocating capacity, preventing cross-campaign slot booking integrity violations.</li>
     *   <li><b>Reserve MF-02 Slot Capacity (MF03-BR-03):</b> Atomically decrements slot capacity. If the slot is
     *       exhausted, throws {@link SlotFullException} and aborts before creating the appointment.</li>
     *   <li><b>Persist Appointment:</b> Saves the new {@link Appointment} in {@link AppointmentStatus#CONFIRMED}
     *       status. All steps succeed or roll back together within the {@code @Transactional} boundary.</li>
     * </ol>
     * </p>
     *
     * @param citizenId  Identifier of the booking citizen
     * @param campaignId Identifier of the target campaign
     * @param slotId     Identifier of the selected time slot
     * @return Saved {@link Appointment} entity
     * @throws NotEligibleException                if citizen is not ELIGIBLE in MF-01
     * @throws DuplicateActiveAppointmentException if citizen already has a CONFIRMED or CHECKED_IN appointment for the campaign
     * @throws DuplicateWaitlistEntryException     if citizen already has an active WAITING waitlist entry for the campaign (Policy A)
     * @throws SlotFullException                   if time slot capacity reservation fails in MF-02
     * @throws IllegalArgumentException            if any required parameter is null or slot does not belong to campaign
     */
    public Appointment bookAppointment(Long citizenId, Long campaignId, Long slotId) {
        if (citizenId == null) {
            throw new IllegalArgumentException("Citizen ID is required");
        }
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        if (slotId == null) {
            throw new IllegalArgumentException("Slot ID is required");
        }

        // Step 1a: Acquire pessimistic write lock on citizen-campaign booking guard row.
        // Viva note: Must be acquired first to serialize concurrent booking/promotion attempts for this citizen.
        guardRepository.insertGuardIfAbsent(citizenId, campaignId);
        guardRepository.findByCitizenIdAndCampaignIdForUpdate(citizenId, campaignId)
                .orElseThrow(() -> new IllegalStateException("Failed to acquire booking guard lock for citizen " + citizenId + " and campaign " + campaignId));

        // Step 1b: Check MF-01 eligibility (MF03-BR-01).
        // Viva note: Checked before reserving capacity so slot counts are not decremented for ineligible citizens.
        if (eligibilityLookupPort == null) {
            throw new IllegalStateException("EligibilityLookupPort implementation (MF-01) is unavailable");
        }
        EligibilityStatus eligibilityStatus = eligibilityLookupPort.getCurrentEligibility(citizenId, campaignId);
        if (eligibilityStatus != EligibilityStatus.ELIGIBLE) {
            throw new NotEligibleException("Citizen " + citizenId + " is not eligible for campaign " + campaignId + ". Eligibility status: " + eligibilityStatus);
        }

        // Step 1c: Check active appointment duplicates (CONFIRMED or CHECKED_IN) (MF03-BR-02).
        // Viva note: Checked under the guard-row lock to prevent concurrent double bookings across threads.
        List<Appointment> existingActiveAppointments = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                citizenId,
                campaignId,
                List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN)
        );
        if (!existingActiveAppointments.isEmpty()) {
            throw new DuplicateActiveAppointmentException("Citizen " + citizenId + " already has an active appointment for campaign " + campaignId);
        }

        // Step 1c-2: Check active waitlist duplicates (WAITING) under guard lock (Policy A - F05 invariant).
        // Viva note: A citizen cannot hold an active waitlist entry and an active appointment for the same campaign.
        // Citizen must explicitly leave the waitlist before direct booking. Checked before capacity reservation and persistence.
        if (waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(citizenId, campaignId, WaitlistStatus.WAITING).isPresent()) {
            throw new DuplicateWaitlistEntryException("Citizen already has an active waitlist entry for this campaign. Must leave waitlist before booking directly.");
        }

        // Step 1d: Verify slot belongs to the requested campaign.
        // Viva note: Verified before reserving capacity to maintain campaign-slot integrity and prevent cross-campaign allocation.
        if (slotCapacityPort == null) {
            throw new IllegalStateException("SlotCapacityPort implementation (MF-02) is unavailable");
        }
        if (!slotCapacityPort.isSlotInCampaign(slotId, campaignId)) {
            throw new IllegalArgumentException("Time slot " + slotId + " does not belong to campaign " + campaignId);
        }

        // Step 1d-2: Enforce the authoritative bookability policy at the mutation boundary (R03).
        // Viva note: The availability/search endpoint cannot guarantee mutation safety. Direct booking must obey
        // the same authoritative restrictions the search path advertises: session lifecycle, campaign booking
        // window, session booking suspension, session booking cutoff, future slot time, and capacity.
        SlotBookability bookability = slotCapacityPort.evaluateBookabilityForNewBooking(slotId, Instant.now());
        if (bookability != SlotBookability.BOOKABLE) {
            throw new IllegalArgumentException("Time slot " + slotId + " is not bookable: " + bookability);
        }

        // Step 1e: Reserve capacity on MF-02 time slot (MF03-BR-03, F1).
        // Viva note: Atomic decrement performed inside MF-02's domain ensures no two users claim the last place,
        // and re-evaluates bookability under session+slot locks to prevent TOCTOU races.
        SlotReservationResult reservationResult = slotCapacityPort.reserveCapacityForNewBooking(slotId, Instant.now());
        if (reservationResult == SlotReservationResult.NOT_BOOKABLE) {
            throw new IllegalArgumentException("Time slot " + slotId + " is not bookable");
        }
        if (reservationResult != SlotReservationResult.SUCCESS) {
            throw new SlotFullException("Time slot " + slotId + " capacity reservation failed with result: " + reservationResult);
        }

        // Step 1f: Persist new CONFIRMED appointment entity within the same transaction.
        Appointment appointment = new Appointment(citizenId, campaignId, slotId, AppointmentStatus.CONFIRMED, Instant.now());
        return appointmentRepository.save(appointment);
    }
}
