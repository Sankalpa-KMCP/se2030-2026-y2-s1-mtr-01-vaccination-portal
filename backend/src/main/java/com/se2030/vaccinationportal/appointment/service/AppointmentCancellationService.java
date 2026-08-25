package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.exception.AppointmentAlreadyCompletedException;
import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.exception.RequiresStaffOverrideException;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating appointment cancellation and time slot capacity release workflows.
 * Enforces 24-hour cancellation rules (MF03-BR-04), completion immutability (MF03-BR-11),
 * and atomic capacity release (MF03-BR-05) within a single transactional boundary.
 */
@Service
@Transactional
public class AppointmentCancellationService {

    private final AppointmentRepository appointmentRepository;
    private final SlotCapacityPort slotCapacityPort;

    @Autowired
    public AppointmentCancellationService(
            AppointmentRepository appointmentRepository,
            @Autowired(required = false) SlotCapacityPort slotCapacityPort
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotCapacityPort = slotCapacityPort;
    }

    /**
     * Cancels an appointment as a citizen (self-service) with 24-hour window enforcement (MF03-BR-04).
     *
     * @param appointmentId        Identifier of the target appointment
     * @param requestingCitizenId Identifier of the citizen making the request
     * @param now                  Current timestamp of the request
     * @return Updated {@link Appointment} in CANCELLED status
     */
    public Appointment cancelAppointment(Long appointmentId, Long requestingCitizenId, Instant now) {
        return cancelAppointment(appointmentId, requestingCitizenId, false, null, now);
    }

    /**
     * Overload for citizen self-cancellation accepting {@link LocalDateTime}.
     */
    public Appointment cancelAppointment(Long appointmentId, Long requestingCitizenId, LocalDateTime now) {
        Instant instantNow = (now != null) ? now.toInstant(ZoneOffset.UTC) : Instant.now();
        return cancelAppointment(appointmentId, requestingCitizenId, false, null, instantNow);
    }

    /**
     * Cancels an appointment as a staff member with optional staff override inside the 24-hour window (MF03-BR-04).
     *
     * @param appointmentId Identifier of the target appointment
     * @param staffReason   Mandatory staff override reason if cancelling within 24 hours
     * @param now           Current timestamp of the request
     * @return Updated {@link Appointment} in CANCELLED status
     */
    public Appointment cancelAppointmentByStaff(Long appointmentId, String staffReason, Instant now) {
        return cancelAppointment(appointmentId, null, true, staffReason, now);
    }

    /**
     * Core cancellation method handling citizen and staff cancellation workflows atomically (MF03-BR-04, MF03-BR-05, MF03-BR-11).
     *
     * @param appointmentId        Identifier of the target appointment
     * @param requestingCitizenId Identifier of the citizen (optional if staff override)
     * @param isStaffOverride      {@code true} if performed by an authorized staff member
     * @param staffReason          Mandatory staff reason when {@code isStaffOverride} is true and inside 24h window
     * @param now                  Current timestamp of the cancellation request
     * @return Updated {@link Appointment} in CANCELLED status
     */
    public Appointment cancelAppointment(
            Long appointmentId,
            Long requestingCitizenId,
            boolean isStaffOverride,
            String staffReason,
            Instant now
    ) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID is required");
        }
        Instant effectiveNow = (now != null) ? now : Instant.now();

        // Step 2a: Load the Appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

        // Ownership, completion status (MF03-BR-11), and 24-hour rule (MF03-BR-04) validation
        AppointmentModificationValidator.validateModificationWindow(
                appointment, requestingCitizenId, isStaffOverride, staffReason, effectiveNow, slotCapacityPort);

        // Step 2d: MF03-BR-05 - Atomic capacity release and status transition to CANCELLED
        boolean released = slotCapacityPort.releaseCapacity(appointment.getSlotId());
        if (!released) {
            throw new IllegalStateException("Failed to release slot capacity for slot ID: " + appointment.getSlotId());
        }

        appointment.cancel(effectiveNow);
        return appointmentRepository.save(appointment);
    }
}
