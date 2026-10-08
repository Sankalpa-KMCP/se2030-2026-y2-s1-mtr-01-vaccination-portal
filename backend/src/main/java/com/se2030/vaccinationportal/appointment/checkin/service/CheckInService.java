package com.se2030.vaccinationportal.appointment.checkin.service;

import com.se2030.vaccinationportal.appointment.waitlist.service.WaitlistPromotionService;
import com.se2030.vaccinationportal.appointment.api.vaccination.VaccinationStateQuery;
import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityLookupPort;
import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityStatus;


import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.exception.AppointmentAlreadyCompletedException;
import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.exception.InvalidAppointmentStateException;
import com.se2030.vaccinationportal.appointment.exception.InvalidCheckInWindowException;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
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
 * Service orchestrating citizen arrival check-in, check-in reversal, no-show marking, and vaccinator queue inspection workflows.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-26: View and manage checked-in and eligible appointments at clinic arrival.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced:</b>
 * <ul>
 *   <li>MF03-BR-09: Standard check-in window is 30 minutes before to 30 minutes after slot start time;
 *       arrival outside this window requires authorized staff override with mandatory reason.</li>
 *   <li>MF03-BR-10: Check-in is permitted only for CONFIRMED appointments at the designated vaccination center.</li>
 *   <li>MF03-BR-11: A COMPLETED appointment cannot be checked in again.</li>
 *   <li>MF03-BR-15: Check-in reversal returns CHECKED_IN to CONFIRMED and is rejected after completion.</li>
 * </ul>
 */
@Service
@Transactional
public class CheckInService {

    private final AppointmentRepository appointmentRepository;
    private final SlotCapacityPort slotCapacityPort;
    private final WaitlistPromotionService promotionService;
    private final VaccinationStateQuery vaccinationStateQuery;
    private final EligibilityLookupPort eligibilityLookupPort;

    @Autowired
    public CheckInService(
            AppointmentRepository appointmentRepository,
            @Autowired(required = false) SlotCapacityPort slotCapacityPort,
            @Autowired(required = false) WaitlistPromotionService promotionService,
            VaccinationStateQuery vaccinationStateQuery,
            @Autowired(required = false) EligibilityLookupPort eligibilityLookupPort
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotCapacityPort = slotCapacityPort;
        this.promotionService = promotionService;
        this.vaccinationStateQuery = vaccinationStateQuery;
        this.eligibilityLookupPort = eligibilityLookupPort;
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
     * <p>
     * <b>Viva / Window Rationale ("Why enforce a 30-minute window"):</b><br>
     * Clinics operate with scheduled capacity per time slot. Arriving hours early causes lobby congestion and
     * risks vaccine dose preparation timing; arriving hours late disrupts subsequent slots. The 30-minute buffer
     * ({@code [-30m, +30m]} from slot start) provides practical grace period while maintaining crowd safety.
     * Exceptions (e.g. transport delay) require staff override with an audit reason (MF03-BR-09).
     * </p>
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
        // Viva note: Window is [-30 minutes before slot start, +30 minutes after slot start].
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
     * <p>
     * <b>Viva / Clinical Note:</b> Reverses an erroneous check-in (e.g. wrong citizen badge scanned,
     * or citizen left before nurse consultation). Permitted only while status is CHECKED_IN;
     * once vaccination is administered and COMPLETED, reversal is strictly prohibited.
     * </p>
     *
     * @param appointmentId Identifier of the appointment to revert
     * @return Updated {@link Appointment} in CONFIRMED status
     */
    public Appointment reverseCheckIn(Long appointmentId, String reason) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID is required");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reversal reason is required");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new InvalidAppointmentStateException("Only CHECKED_IN appointments can be reversed. Current status: "
                    + appointment.getStatus());
        }

        if (vaccinationStateQuery.hasFinalizedRecordForAppointment(appointmentId)) {
            throw new InvalidAppointmentStateException(
                    "Check-in cannot be reversed after vaccination finalization");
        }
        if (vaccinationStateQuery.hasDraftForAppointment(appointmentId)) {
            throw new InvalidAppointmentStateException(
                    "Delete or explicitly invalidate the vaccination draft before reversing check-in");
        }

        appointment.reverseCheckIn();
        return appointmentRepository.save(appointment);
    }

    /**
     * Marks a CONFIRMED appointment as NO_SHOW after its check-in window closes, releasing capacity and triggering waitlist promotion.
     * <p>
     * <b>Viva / Concurrency Note:</b> A no-show cannot be recorded until after the check-in window expires (+30 minutes
     * past slot start), ensuring the citizen has full opportunity to arrive. Once recorded, the unused slot capacity is
     * released and waitlist promotion fires immediately to offer the dose to waiting citizens.
     * </p>
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

        // Validate window close: Citizen must not be marked NO_SHOW while their 30-minute arrival grace window is still open
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

        // Release capacity on slot and trigger waitlist promotion (same pattern as cancellation)
        // Viva note: The unused place is immediately reclaimed and allocated to the next FIFO waitlist candidate.
        boolean ownsReservation = appointment.ownsSlotReservation();
        if (ownsReservation) {
            if (slotCapacityPort != null) {
                boolean released = slotCapacityPort.releaseCapacity(appointment.getSlotId());
                if (!released) {
                    throw new IllegalStateException("Failed to release slot capacity for slot ID: " + appointment.getSlotId());
                }
            }
        }

        appointment.markNoShow();
        Appointment saved = appointmentRepository.save(appointment);

        if (ownsReservation && promotionService != null) {
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
        return retainCurrentlyEligible(appointmentRepository.findBySlotIdAndStatusIn(
                slotId, List.of(AppointmentStatus.CHECKED_IN)));
    }

    /**
     * Returns the operational arrival queue used by center managers and administrators.
     * CONFIRMED appointments must be visible so staff can check citizens in or mark
     * them as no-shows, while CHECKED_IN appointments remain visible for reversal.
     */
    public List<Appointment> getArrivalQueueForSlot(Long slotId) {
        if (slotId == null) {
            return List.of();
        }
        return appointmentRepository.findBySlotIdAndStatusIn(
                slotId,
                List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN)
        );
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
        return retainCurrentlyEligible(appointmentRepository.findBySlotIdInAndStatusIn(
                slotIds, List.of(AppointmentStatus.CHECKED_IN)));
    }

    private List<Appointment> retainCurrentlyEligible(List<Appointment> appointments) {
        if (eligibilityLookupPort == null) {
            throw new IllegalStateException("EligibilityLookupPort implementation (MF-01) is unavailable");
        }
        return appointments.stream()
                .filter(appointment -> eligibilityLookupPort.getCurrentEligibility(
                        appointment.getCitizenId(), appointment.getCampaignId()) == EligibilityStatus.ELIGIBLE)
                .toList();
    }

    /**
     * Completes an appointment upon vaccination finalization (MF-05 handover).
     *
     * @param appointmentId Identifier of the appointment
     * @return Updated {@link Appointment} in COMPLETED status
     */
    public Appointment completeAppointment(Long appointmentId) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID is required");
        }
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppointmentAlreadyCompletedException("Completed appointment " + appointmentId + " cannot be completed again");
        }
        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new InvalidAppointmentStateException("Appointment " + appointmentId + " must be in CHECKED_IN status to complete, but was " + appointment.getStatus());
        }

        appointment.complete();
        return appointmentRepository.save(appointment);
    }
}
