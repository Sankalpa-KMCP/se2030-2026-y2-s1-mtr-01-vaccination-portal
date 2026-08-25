package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.dependency.SlotReservationResult;
import com.se2030.vaccinationportal.appointment.exception.AppointmentAlreadyCompletedException;
import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.exception.RequiresStaffOverrideException;
import com.se2030.vaccinationportal.appointment.exception.SlotFullException;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating atomic appointment rescheduling workflows (MF03-BR-06).
 * <p>
 * Enforces guard-row write locking, 24-hour notice window rules (MF03-BR-04), completion immutability (MF03-BR-11),
 * capacity reservation on the target slot BEFORE release of the old slot, and atomic entity mutation within one single
 * {@link Transactional} boundary.
 * </p>
 */
@Service
@Transactional
public class AppointmentRescheduleService {

    private final AppointmentRepository appointmentRepository;
    private final CitizenCampaignBookingGuardRepository guardRepository;
    private final SlotCapacityPort slotCapacityPort;

    @Autowired
    public AppointmentRescheduleService(
            AppointmentRepository appointmentRepository,
            CitizenCampaignBookingGuardRepository guardRepository,
            @Autowired(required = false) SlotCapacityPort slotCapacityPort
    ) {
        this.appointmentRepository = appointmentRepository;
        this.guardRepository = guardRepository;
        this.slotCapacityPort = slotCapacityPort;
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
     *   <li>Acquire pessimistic write lock on {@link com.se2030.vaccinationportal.appointment.model.CitizenCampaignBookingGuard}
     *       for (citizenId, campaignId) to serialize concurrent operations for the same citizen+campaign.</li>
     *   <li>Reserve capacity on {@code newSlotId} via {@link SlotCapacityPort#reserveCapacity(Long)} BEFORE touching the old slot.
     *       If capacity is exhausted, throw {@link SlotFullException} leaving the old slot and appointment completely untouched.</li>
     *   <li>Release capacity on old {@code slotId} via {@link SlotCapacityPort#releaseCapacity(Long)}.</li>
     *   <li>Mutate existing {@link Appointment}'s slotId to {@code newSlotId} (status stays CONFIRMED, lockVersion increments).</li>
     * </ol>
     * <i>Transactional Rollback Contract:</i> Under Spring's default transactional behavior, if step 5 or 6 fails or throws any
     * {@link RuntimeException} after step 4 (new slot reservation) succeeded, the entire transaction rolls back completely,
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
        guardRepository.insertGuardIfAbsent(appointment.getCitizenId(), appointment.getCampaignId());
        guardRepository.findByCitizenIdAndCampaignIdForUpdate(appointment.getCitizenId(), appointment.getCampaignId())
                .orElseThrow(() -> new IllegalStateException("Failed to acquire booking guard lock for citizen "
                        + appointment.getCitizenId() + " and campaign " + appointment.getCampaignId()));

        // Step 1e: Reserve capacity on newSlotId BEFORE releasing old slot
        SlotReservationResult reservationResult = slotCapacityPort.reserveCapacity(newSlotId);
        if (reservationResult != SlotReservationResult.SUCCESS) {
            throw new SlotFullException("Target time slot " + newSlotId + " capacity reservation failed with result: " + reservationResult);
        }

        // Step 1f: Release capacity on old slotId AFTER new slot reservation succeeds
        boolean released = slotCapacityPort.releaseCapacity(appointment.getSlotId());
        if (!released) {
            // Note: If releasing old slot fails, throwing RuntimeException triggers Spring's default @Transactional rollback,
            // which automatically reverts the newSlot capacity reservation made in Step 1e within the same atomic boundary.
            throw new IllegalStateException("Failed to release capacity for old slot ID: " + appointment.getSlotId() + ". Transaction rolling back.");
        }

        // Update existing Appointment entity (status remains CONFIRMED, JPA @Version increments automatically)
        appointment.reschedule(newSlotId);
        return appointmentRepository.save(appointment);
    }
}
