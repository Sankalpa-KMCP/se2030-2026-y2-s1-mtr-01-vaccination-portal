package com.se2030.vaccinationportal.appointment.booking.service;

import com.se2030.vaccinationportal.appointment.waitlist.service.WaitlistPromotionService;


import com.se2030.vaccinationportal.campaign.api.capacity.SlotBookability;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotReservationResult;
import com.se2030.vaccinationportal.appointment.exception.AppointmentAlreadyCompletedException;
import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.exception.RequiresStaffOverrideException;
import com.se2030.vaccinationportal.appointment.exception.SlotFullException;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating atomic appointment rescheduling workflows (MF03-BR-05).
 * <p>
 * Enforces guard-row write locking, 24-hour notice window rules (MF03-BR-04), completion immutability (MF03-BR-11),
 * capacity reservation on the target slot BEFORE release of the old slot, and atomic entity mutation within one single
 * {@link Transactional} boundary.
 * </p>
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Reschedule an existing appointment slot.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced:</b>
 * <ul>
 *   <li>MF03-BR-04: Rescheduling within 24 hours of slot start requires staff override with reason.</li>
 *   <li>MF03-BR-05: Atomic release of old slot capacity and reservation of new slot capacity in one transaction.</li>
 *   <li>MF03-BR-11: COMPLETED appointments cannot be rescheduled.</li>
 * </ul>
 */
@Service
@Transactional
public class AppointmentRescheduleService {

    private final AppointmentRepository appointmentRepository;
    private final CitizenCampaignBookingGuardRepository guardRepository;
    private final SlotCapacityPort slotCapacityPort;
    private final WaitlistPromotionService promotionService;

    @Autowired
    public AppointmentRescheduleService(
            AppointmentRepository appointmentRepository,
            CitizenCampaignBookingGuardRepository guardRepository,
            @Autowired(required = false) SlotCapacityPort slotCapacityPort,
            @Autowired(required = false) WaitlistPromotionService promotionService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.guardRepository = guardRepository;
        this.slotCapacityPort = slotCapacityPort;
        this.promotionService = promotionService;
    }

    /**
     * Reschedules an existing appointment for a citizen to a new time slot (citizen self-service).
     *
     * @param appointmentId        Identifier of the appointment to reschedule
     * @param requestingCitizenId Identifier of the citizen making the request
     * @param newSlotId            Identifier of the target time slot
     * @param now                  Timestamp of the reschedule request
     * @return Updated {@link Appointment} entity with new slot ID
     */
    public Appointment rescheduleAppointment(Long appointmentId, Long requestingCitizenId, Long newSlotId, Instant now) {
        return rescheduleAppointment(appointmentId, requestingCitizenId, newSlotId, false, null, now);
    }

    /**
     * Overload for citizen self-service rescheduling accepting {@link LocalDateTime}.
     */
    public Appointment rescheduleAppointment(Long appointmentId, Long requestingCitizenId, Long newSlotId, LocalDateTime now) {
        Instant instantNow = (now != null) ? now.toInstant(ZoneOffset.UTC) : Instant.now();
        return rescheduleAppointment(appointmentId, requestingCitizenId, newSlotId, false, null, instantNow);
    }

    /**
     * Reschedules an existing appointment with staff override (inside 24h window if needed).
     *
     * @param appointmentId Identifier of the appointment to reschedule
     * @param newSlotId     Identifier of the target time slot
     * @param staffReason   Mandatory staff reason when rescheduling inside 24h window
     * @param now           Timestamp of the reschedule request
     * @return Updated {@link Appointment} entity with new slot ID
     */
    public Appointment rescheduleAppointmentByStaff(Long appointmentId, Long newSlotId, String staffReason, Instant now) {
        return rescheduleAppointment(appointmentId, null, newSlotId, true, staffReason, now);
    }

    /**
     * Core rescheduling method operating in ONE {@link Transactional} boundary (MF03-BR-06).
     * <p>
     * <b>Execution Sequence:</b>
     * <ol>
     *   <li>Load existing {@link Appointment}; throw {@link AppointmentNotFoundException} if not found.</li>
     *   <li>Validate status non-completion ({@link AppointmentAlreadyCompletedException}) and 24-hour notice window
     *       ({@link RequiresStaffOverrideException}) using shared {@link AppointmentModificationValidator}.</li>
     *   <li>Acquire pessimistic write lock on {@link com.se2030.vaccinationportal.appointment.persistence.model.CitizenCampaignBookingGuard}
     *       for (citizenId, campaignId) to serialize concurrent operations for the same citizen+campaign.</li>
     *   <li>Verify target {@code newSlotId} belongs to {@code appointment.getCampaignId()} to prevent cross-campaign reschedule attacks.</li>
     *   <li>Reserve capacity on {@code newSlotId} via {@link SlotCapacityPort#reserveCapacity(Long)} BEFORE touching the old slot.
     *       If capacity is exhausted, throw {@link SlotFullException} leaving the old slot and appointment completely untouched.</li>
     *   <li>Release capacity on old {@code slotId} via {@link SlotCapacityPort#releaseCapacity(Long)}.</li>
     *   <li>Mutate existing {@link Appointment}'s slotId to {@code newSlotId} (status stays CONFIRMED, lockVersion increments).</li>
     * </ol>
     * <i>Transactional Rollback Contract:</i> Under Spring's default transactional behavior, if step 6 or 7 fails or throws any
     * {@link RuntimeException} after step 5 (new slot reservation) succeeded, the entire transaction rolls back completely,
     * reverting the new-slot reservation and preserving system-wide capacity integrity.
     * </p>
     *
     * @param appointmentId        Identifier of the target appointment
     * @param requestingCitizenId Identifier of requesting citizen (optional if staff override)
     * @param newSlotId            Identifier of the proposed target time slot
     * @param isStaffOverride      {@code true} if performed by an authorized staff member
     * @param staffReason          Mandatory staff reason if overriding inside 24h window
     * @param now                  Timestamp of reschedule request
     * @return Updated {@link Appointment} entity
     */
    public Appointment rescheduleAppointment(
            Long appointmentId,
            Long requestingCitizenId,
            Long newSlotId,
            boolean isStaffOverride,
            String staffReason,
            Instant now
    ) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID is required");
        }
        if (newSlotId == null) {
            throw new IllegalArgumentException("New slot ID is required");
        }
        Instant effectiveNow = (now != null) ? now : Instant.now();

        // Step 1a: Load the existing Appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

        if (newSlotId.equals(appointment.getSlotId())) {
            throw new IllegalArgumentException("Proposed new slot ID is identical to current slot ID");
        }

        // Step 1b & 1c: Validate 24-hour notice window (MF03-BR-04) & completion status (MF03-BR-11)
        AppointmentModificationValidator.validateModificationWindow(
                appointment, requestingCitizenId, isStaffOverride, staffReason, effectiveNow, slotCapacityPort);

        // Step 1d: Acquire CitizenCampaignBookingGuard pessimistic write lock for (citizenId, campaignId)
        // Viva note: Locks the citizen mutex row to prevent concurrent actions (like waitlist promotion) during slot migration.
        guardRepository.insertGuardIfAbsent(appointment.getCitizenId(), appointment.getCampaignId());
        guardRepository.findByCitizenIdAndCampaignIdForUpdate(appointment.getCitizenId(), appointment.getCampaignId())
                .orElseThrow(() -> new IllegalStateException("Failed to acquire booking guard lock for citizen "
                        + appointment.getCitizenId() + " and campaign " + appointment.getCampaignId()));

        // Step 1e: Verify target newSlotId belongs to appointment's campaign
        // Viva note: Verified before reserving capacity to maintain campaign-slot integrity and prevent cross-campaign reschedule attacks.
        if (slotCapacityPort == null) {
            throw new IllegalStateException("SlotCapacityPort implementation (MF-02) is unavailable");
        }
        if (!slotCapacityPort.isSlotInCampaign(newSlotId, appointment.getCampaignId())) {
            throw new IllegalArgumentException("Target time slot " + newSlotId + " does not belong to campaign " + appointment.getCampaignId());
        }

        // Step 1e-2: Enforce the authoritative bookability policy on the destination slot (R03).
        // Viva note: A reschedule destination is a new reservation mutation — it must obey the same
        // restrictions as direct booking (session lifecycle, campaign booking window, suspension,
        // cutoff, future slot time, capacity). The 24h staff-override rule governs the SOURCE
        // appointment only; it never legitimizes an unbookable destination.
        SlotBookability bookability = slotCapacityPort.evaluateBookabilityForNewBooking(newSlotId, effectiveNow);
        if (bookability != SlotBookability.BOOKABLE) {
            throw new IllegalArgumentException("Target time slot " + newSlotId + " is not bookable: " + bookability);
        }

        // Step 1f: CRITICAL VIVA PROBE: "Reserve-Before-Release" Ordering (MF03-BR-05, F1)
        // Why reserve newSlotId BEFORE releasing oldSlotId?
        // If we released the old slot first and then attempted to reserve the new slot only to find it full
        // (or failing due to a concurrent booking race), the citizen would be left stranded with NEITHER slot!
        // By reserving newSlotId first, any failure (SlotFullException / IllegalArgumentException) leaves the
        // citizen's existing slot and appointment completely intact.
        SlotReservationResult reservationResult = slotCapacityPort.reserveCapacityForNewBooking(newSlotId, effectiveNow);
        if (reservationResult == SlotReservationResult.NOT_BOOKABLE) {
            throw new IllegalArgumentException("Target time slot " + newSlotId + " is not bookable");
        }
        if (reservationResult != SlotReservationResult.SUCCESS) {
            throw new SlotFullException("Target time slot " + newSlotId + " capacity reservation failed with result: " + reservationResult);
        }

        Long oldSlotId = appointment.getSlotId();

        // Step 1g: Release capacity on old slotId AFTER new slot reservation succeeds
        // Viva note: If releasing the old slot fails or throws any exception, Spring's @Transactional rollback
        // automatically reverts the Step 1f reservation on newSlotId, keeping total system capacity consistent.
        boolean released = slotCapacityPort.releaseCapacity(oldSlotId);
        if (!released) {
            throw new IllegalStateException("Failed to release capacity for old slot ID: " + oldSlotId + ". Transaction rolling back.");
        }

        // Update existing Appointment entity (status remains CONFIRMED, JPA @Version increments automatically)
        appointment.reschedule(newSlotId);
        Appointment saved = appointmentRepository.save(appointment);

        // Step 1h: Trigger automatic waitlist promotion for the freed old slot synchronously within this transaction.
        // Viva note: Running promotion synchronously within the same transaction ensures that the freshly released
        // old slot is immediately offered to waiting citizens in FIFO order before external searchers can grab it.
        if (promotionService != null) {
            promotionService.promoteNextForSlot(appointment.getCampaignId(), oldSlotId, effectiveNow);
        }

        return saved;
    }
}
