package com.se2030.vaccinationportal.appointment.booking.service;

import com.se2030.vaccinationportal.appointment.checkin.service.CheckInService;


import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.exception.AppointmentAlreadyCompletedException;
import com.se2030.vaccinationportal.appointment.exception.InvalidAppointmentStateException;
import com.se2030.vaccinationportal.appointment.exception.RequiresStaffOverrideException;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import java.time.Duration;
import java.time.Instant;

/**
 * Shared validator enforcing common appointment modification rules across cancellation and rescheduling:
 * completion immutability (MF03-BR-11), NO_SHOW terminality (capacity ownership), and 24-hour window
 * staff-override requirements (MF03-BR-04).
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Cancellation and rescheduling authorization and time window validation.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced:</b>
 * <ul>
 *   <li>MF03-BR-04: Citizen self-service modifications are permitted only until 24 hours prior to slot start time.
 *       Inside the 24-hour window, changes require authorized staff override with a mandatory explanation reason.</li>
 *   <li>MF03-BR-11: COMPLETED appointments are legally/clinically final and cannot be cancelled or rescheduled.</li>
 *   <li>NO_SHOW capacity ownership: a NO_SHOW appointment already released its slot reservation when it was marked
 *       as a no-show, so it no longer owns capacity and must not be cancelled or rescheduled — both transitions would
 *       release (or move) capacity it does not hold, driving reserved counters below the number of real reservations.</li>
 * </ul>
 */
public class AppointmentModificationValidator {

    /**
     * Validates that an appointment can be modified (cancelled or rescheduled) by checking
     * ownership, status non-completion, and the 24-hour notice window rule.
     * <p>
     * <b>Viva Rationale:</b>
     * <ul>
     *   <li><b>Ownership:</b> Verifies the requesting citizen matches the appointment record to prevent horizontal privilege escalation.</li>
     *   <li><b>Clinical Finality:</b> Blocks changes to COMPLETED appointments (MF03-BR-11) because the vaccination has already occurred.</li>
     *   <li><b>24-Hour Notice Window:</b> Protects clinic capacity utilization (MF03-BR-04). Late cancellations/reschedules within 24h
     *       waste health worker slots unless vetted and recorded by authorized staff.</li>
     * </ul>
     * </p>
     *
     * @param appointment         Target {@link Appointment}
     * @param requestingCitizenId Identifier of requesting citizen (null if staff override)
     * @param isStaffOverride     {@code true} if action performed by authorized staff
     * @param staffReason         Reason string if staff override inside 24h window
     * @param effectiveNow        Timestamp of modification request
     * @param slotCapacityPort    Port to retrieve slot schedule start time
     */
    public static void validateModificationWindow(
            Appointment appointment,
            Long requestingCitizenId,
            boolean isStaffOverride,
            String staffReason,
            Instant effectiveNow,
            SlotCapacityPort slotCapacityPort
    ) {
        if (!isStaffOverride) {
            if (requestingCitizenId == null || !requestingCitizenId.equals(appointment.getCitizenId())) {
                throw new IllegalArgumentException("Citizen ID " + requestingCitizenId + " is not authorized to modify appointment " + appointment.getId());
            }
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Appointment " + appointment.getId() + " is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppointmentAlreadyCompletedException("A completed appointment cannot be modified (appointment ID: " + appointment.getId() + ")");
        }

        // NO_SHOW already released its capacity when the no-show was recorded (CheckInService.markNoShow).
        // It no longer owns a slot reservation, so cancelling or rescheduling it would release/move
        // capacity a second time and corrupt the slot's reserved counter.
        if (appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new InvalidAppointmentStateException("Appointment " + appointment.getId()
                    + " is marked NO_SHOW and can no longer be cancelled or rescheduled because its capacity was already released");
        }

        if (slotCapacityPort == null) {
            throw new IllegalStateException("SlotCapacityPort implementation (MF-02) is unavailable");
        }
        Instant slotStart = slotCapacityPort.getSlotStartTime(appointment.getSlotId());
        if (slotStart == null) {
            throw new IllegalStateException("Slot start time not available for slot ID: " + appointment.getSlotId());
        }

        Instant citizenCutoff = slotStart.minus(Duration.ofHours(24));
        boolean isWithin24Hours = effectiveNow.isAfter(citizenCutoff);

        if (isWithin24Hours) {
            if (!isStaffOverride) {
                throw new RequiresStaffOverrideException(
                        "Modifications within 24 hours of session start require staff override (slot start: "
                                + slotStart + ", request time: " + effectiveNow + ")"
                );
            }
            if (staffReason == null || staffReason.trim().isEmpty()) {
                throw new IllegalArgumentException("A staff reason is required when overriding modifications within the 24-hour window");
            }
        }
    }
}
