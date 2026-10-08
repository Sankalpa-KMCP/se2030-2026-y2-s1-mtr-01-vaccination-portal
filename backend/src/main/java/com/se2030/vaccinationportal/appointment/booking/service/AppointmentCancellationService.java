package com.se2030.vaccinationportal.appointment.booking.service;

import com.se2030.vaccinationportal.appointment.waitlist.service.WaitlistPromotionService;


import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.event.AppointmentEvent;
import com.se2030.vaccinationportal.appointment.event.Mf03HistoryRecorder;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentChangeType;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating appointment cancellation and time slot capacity release workflows.
 * Enforces 24-hour cancellation rules (MF03-BR-04), completion immutability (MF03-BR-11),
 * atomic capacity release (MF03-BR-05), and synchronous waitlist promotion within a single transactional boundary.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Citizen self-service appointment cancellation and staff override cancellation.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced:</b>
 * <ul>
 *   <li>MF03-BR-04: Citizen self-cancellation allowed until 24 hours before slot start;
 *       within 24 hours requires authorized staff override with mandatory reason.</li>
 *   <li>MF03-BR-05: Atomically releases time slot capacity back to MF-02.</li>
 *   <li>MF03-BR-11: A COMPLETED appointment cannot be cancelled.</li>
 * </ul>
 */
@Service
@Transactional
public class AppointmentCancellationService {
    private Mf03HistoryRecorder historyRecorder;

    @Autowired(required = false)
    void setHistoryRecorder(Mf03HistoryRecorder historyRecorder) { this.historyRecorder = historyRecorder; }

    private final ApplicationEventPublisher eventPublisher;

    private final AppointmentRepository appointmentRepository;
    private final SlotCapacityPort slotCapacityPort;
    private final WaitlistPromotionService promotionService;

    @Autowired
    public AppointmentCancellationService(
            ApplicationEventPublisher eventPublisher,
            AppointmentRepository appointmentRepository,
            @Autowired(required = false) SlotCapacityPort slotCapacityPort,
            @Autowired(required = false) WaitlistPromotionService promotionService
    ) {
        this.eventPublisher = eventPublisher;
        this.appointmentRepository = appointmentRepository;
        this.slotCapacityPort = slotCapacityPort;
        this.promotionService = promotionService;
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
     * <p>
     * <b>Viva / Concurrency Rationale:</b> Validates ownership and the 24-hour modification window before releasing
     * capacity. Upon cancellation, the slot capacity is released in MF-02 and any queued waitlist candidate is promoted
     * <i>synchronously</i> within this exact same transaction so that the freed place is not intercepted by an external booker.
     * </p>
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

        // Ownership, completion status (MF03-BR-11), NO_SHOW terminality, and 24-hour rule (MF03-BR-04) validation
        AppointmentModificationValidator.validateModificationWindow(
                appointment, requestingCitizenId, isStaffOverride, staffReason, effectiveNow, slotCapacityPort);

        // Step 2d: MF03-BR-05 - Atomic capacity release gated on current state ownership (R04):
        // capacity may only be released if the appointment state still holds its reservation
        // (CONFIRMED or CHECKED_IN). NO_SHOW already released its capacity and cannot reach here.
        boolean ownsReservation = appointment.ownsSlotReservation();
        if (ownsReservation) {
            boolean released = slotCapacityPort.releaseCapacity(appointment.getSlotId());
            if (!released) {
                throw new IllegalStateException("Failed to release slot capacity for slot ID: " + appointment.getSlotId());
            }
        }

        appointment.cancel(effectiveNow);
        Appointment saved = appointmentRepository.save(appointment);
        if (historyRecorder != null) {
            String reason = isStaffOverride ? staffReason : "Citizen cancelled appointment";
            historyRecorder.appointment(saved, AppointmentChangeType.CANCELLED,
                    appointment.getSlotId(), null, reason, effectiveNow);
            historyRecorder.notification(saved, AppointmentChangeType.CANCELLED,
                    "Your vaccination appointment has been cancelled.");
        }
        eventPublisher.publishEvent(AppointmentEvent.from(AppointmentEvent.Type.CANCELLED, saved));

        // Step 2e: Trigger automatic waitlist promotion if a waiter is queued for this freed capacity.
        // Viva / Concurrency Sequencing Probe: Why promote synchronously inside cancellation's own transaction?
        // If cancellation committed before promoting, an external concurrent citizen could swoop in and steal the newly
        // freed slot via a direct booking request, starving the waitlisted citizen who has been waiting in FIFO order!
        // By running promotion synchronously within the cancellation transaction, the released capacity is immediately
        // claimed by the earliest eligible FIFO waitlist entry before other transactions can observe the unreserved capacity.
        if (ownsReservation && promotionService != null) {
            promotionService.promoteNextForSlot(appointment.getCampaignId(), appointment.getSlotId(), effectiveNow);
        }

        return saved;
    }
}
