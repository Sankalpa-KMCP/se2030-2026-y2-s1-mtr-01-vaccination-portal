package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityLookupPort;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.dependency.SlotReservationResult;
import com.se2030.vaccinationportal.appointment.exception.DuplicateActiveAppointmentException;
import com.se2030.vaccinationportal.appointment.exception.NotEligibleException;
import com.se2030.vaccinationportal.appointment.exception.SlotFullException;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.event.AppointmentEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating atomic appointment booking workflows for citizens.
 * Enforces guard-row locking, cross-module eligibility lookup, active appointment duplicate checks,
 * and time-slot capacity reservations within one transactional boundary.
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
 * </ul>
 */
@Service
@Transactional
public class AppointmentBookingService {

    private final ApplicationEventPublisher eventPublisher;

    private final AppointmentRepository appointmentRepository;
    private final CitizenCampaignBookingGuardRepository guardRepository;
    private final EligibilityLookupPort eligibilityLookupPort;
    private final SlotCapacityPort slotCapacityPort;

    @Autowired
    public AppointmentBookingService(
            ApplicationEventPublisher eventPublisher,
            AppointmentRepository appointmentRepository,
            CitizenCampaignBookingGuardRepository guardRepository,
            @Autowired(required = false) EligibilityLookupPort eligibilityLookupPort,
            @Autowired(required = false) SlotCapacityPort slotCapacityPort
    ) {
        this.eventPublisher = eventPublisher;
        this.appointmentRepository = appointmentRepository;
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
     * @throws SlotFullException                   if time slot capacity reservation fails in MF-02
     * @throws IllegalArgumentException            if any required parameter is null
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

        // Step 1d: Reserve capacity on MF-02 time slot (MF03-BR-03).
        // Viva note: Atomic decrement performed inside MF-02's domain ensures no two users claim the last place.
        if (slotCapacityPort == null) {
            throw new IllegalStateException("SlotCapacityPort implementation (MF-02) is unavailable");
        }
        SlotReservationResult reservationResult = slotCapacityPort.reserveCapacity(slotId);
        if (reservationResult != SlotReservationResult.SUCCESS) {
            throw new SlotFullException("Time slot " + slotId + " capacity reservation failed with result: " + reservationResult);
        }

        // Step 1e: Persist new CONFIRMED appointment entity within the same transaction.
        Appointment appointment = new Appointment(citizenId, campaignId, slotId, AppointmentStatus.CONFIRMED, Instant.now());
        Appointment saved = appointmentRepository.save(appointment);
        eventPublisher.publishEvent(AppointmentEvent.from(AppointmentEvent.Type.BOOKED, saved));
        return saved;
    }
}
