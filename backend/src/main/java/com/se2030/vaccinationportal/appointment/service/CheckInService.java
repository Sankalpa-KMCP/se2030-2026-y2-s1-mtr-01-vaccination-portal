package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.exception.AppointmentAlreadyCompletedException;
import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.exception.InvalidAppointmentStateException;
import com.se2030.vaccinationportal.appointment.exception.InvalidCheckInWindowException;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating citizen check-in, check-in reversal, no-show marking, and vaccinator queue inspection workflows (US-26, MF03-BR-09, MF03-BR-10, MF03-BR-15).
 */
@Service
@Transactional
public class CheckInService {

    private final AppointmentRepository appointmentRepository;
    private final SlotCapacityPort slotCapacityPort;
    private final WaitlistPromotionService promotionService;

    @Autowired
    public CheckInService(
            AppointmentRepository appointmentRepository,
            @Autowired(required = false) SlotCapacityPort slotCapacityPort,
            @Autowired(required = false) WaitlistPromotionService promotionService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotCapacityPort = slotCapacityPort;
        this.promotionService = promotionService;
    }

    /**
     * Checks in a citizen for a CONFIRMED appointment inside the allowable 30-minute window (MF03-BR-09, MF03-BR-10).
     *
     * @param appointmentId Identifier of the appointment
     * @param now           Current request timestamp
     * @return Updated {@link Appointment} in CHECKED_IN status
     */
    public Appointment checkIn(Long appointmentId, Instant now) {
        return checkIn(appointmentId, false, null, now);
    }

    /**
     * Overload for checkIn accepting {@link LocalDateTime}.
     */
    public Appointment checkIn(Long appointmentId, LocalDateTime now) {
        Instant instantNow = (now != null) ? now.toInstant(ZoneOffset.UTC) : Instant.now();
        return checkIn(appointmentId, false, null, instantNow);
    }

    /**
     * Staff check-in with optional override for check-ins outside the 30-minute window (MF03-BR-09).
     *
     * @param appointmentId Identifier of the appointment
     * @param staffReason   Reason required if checking in outside the 30-minute window
     * @param now           Current request timestamp
     * @return Updated {@link Appointment} in CHECKED_IN status
     */
    public Appointment checkInByStaff(Long appointmentId, String staffReason, Instant now) {
        return checkIn(appointmentId, true, staffReason, now);
    }

    /**
     * Core check-in method enforcing status immutability, 30-minute window validation, and state transition.
     *
     * @param appointmentId   Identifier of the target appointment
     * @param isStaffOverride {@code true} if staff override is active
     * @param staffReason     Reason required when overriding outside 30-minute window
     * @param now             Current request timestamp
     * @return Updated {@link Appointment} in CHECKED_IN status
     */
    public Appointment checkIn(Long appointmentId, boolean isStaffOverride, String staffReason, Instant now) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID is required");
        }
        Instant effectiveNow = (now != null) ? now : Instant.now();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppointmentAlreadyCompletedException("Completed appointment " + appointmentId + " cannot be checked in again");
        }
        if (appointment.getStatus() == AppointmentStatus.CHECKED_IN) {
            throw new InvalidAppointmentStateException("Appointment " + appointmentId + " is already checked in");
        }
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new InvalidAppointmentStateException("Appointment " + appointmentId + " is in status "
                    + appointment.getStatus() + " and cannot be checked in");
        }

        // Validate 30-minute check-in window (MF03-BR-09)
        if (slotCapacityPort != null) {
            Instant slotStart = slotCapacityPort.getSlotStartTime(appointment.getSlotId());
            if (slotStart != null) {
                long minutesFromStart = Duration.between(slotStart, effectiveNow).toMinutes();
                if (minutesFromStart < -30 || minutesFromStart > 30) {
                    if (!isStaffOverride) {
                        throw new InvalidCheckInWindowException("Check-in at " + effectiveNow
                                + " is outside the allowable 30-minute window for slot start " + slotStart);
                    }
                    if (staffReason == null || staffReason.trim().isEmpty()) {
                        throw new IllegalArgumentException("Staff reason is required when checking in outside the 30-minute window");
                    }
                }
            }
        }

        appointment.checkIn();
        return appointmentRepository.save(appointment);
    }

    /**
     * Reverts a CHECKED_IN appointment back to CONFIRMED (MF03-BR-15).
     *
     * @param appointmentId Identifier of the appointment to revert
     * @return Updated {@link Appointment} in CONFIRMED status
     */
    public Appointment reverseCheckIn(Long appointmentId) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID is required");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new InvalidAppointmentStateException("Only CHECKED_IN appointments can be reversed. Current status: "
                    + appointment.getStatus());
        }

        appointment.reverseCheckIn();
        return appointmentRepository.save(appointment);
    }

    /**
     * Marks a CONFIRMED appointment as NO_SHOW after its check-in window closes, releasing capacity and triggering waitlist promotion.
     *
     * @param appointmentId Identifier of the appointment
     * @param now           Current request timestamp
     * @return Updated {@link Appointment} in NO_SHOW status
     */
    public Appointment markNoShow(Long appointmentId, Instant now) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID is required");
        }
        Instant effectiveNow = (now != null) ? now : Instant.now();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new InvalidAppointmentStateException("Only CONFIRMED appointments can be marked as NO_SHOW. Current status: "
                    + appointment.getStatus());
        }

        // Validate window close
        if (slotCapacityPort != null) {
            Instant slotStart = slotCapacityPort.getSlotStartTime(appointment.getSlotId());
            if (slotStart != null) {
                long minutesFromStart = Duration.between(slotStart, effectiveNow).toMinutes();
                if (minutesFromStart <= 30) {
                    throw new InvalidCheckInWindowException("Cannot mark NO_SHOW before check-in window closes (slot start: "
                            + slotStart + ", current time: " + effectiveNow + ")");
                }
            }
        }

        appointment.markNoShow();
        Appointment saved = appointmentRepository.save(appointment);

        // Release capacity on slot and trigger waitlist promotion (same pattern as cancellation)
        if (slotCapacityPort != null) {
            slotCapacityPort.releaseCapacity(appointment.getSlotId());
        }
        if (promotionService != null) {
            promotionService.promoteNextForSlot(appointment.getCampaignId(), appointment.getSlotId(), effectiveNow);
        }

        return saved;
    }

    /**
     * Overload for markNoShow accepting {@link LocalDateTime}.
     */
    public Appointment markNoShow(Long appointmentId, LocalDateTime now) {
        Instant instantNow = (now != null) ? now.toInstant(ZoneOffset.UTC) : Instant.now();
        return markNoShow(appointmentId, instantNow);
    }

    /**
     * Queries checked-in appointments for a time slot (vaccinator queue, US-26).
     *
     * @param slotId Identifier of the time slot
     * @return List of {@link Appointment} entities in CHECKED_IN status
     */
    public List<Appointment> getVaccinatorQueueForSlot(Long slotId) {
        if (slotId == null) {
            return List.of();
        }
        return appointmentRepository.findBySlotIdAndStatusIn(slotId, List.of(AppointmentStatus.CHECKED_IN));
    }

    /**
     * Queries checked-in appointments across a collection of time slots for a session (vaccinator queue, US-26).
     *
     * @param slotIds Collection of slot IDs in the session
     * @return List of {@link Appointment} entities in CHECKED_IN status
     */
    public List<Appointment> getVaccinatorQueueForSlots(Collection<Long> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) {
            return List.of();
        }
        return appointmentRepository.findBySlotIdInAndStatusIn(slotIds, List.of(AppointmentStatus.CHECKED_IN));
    }
}
