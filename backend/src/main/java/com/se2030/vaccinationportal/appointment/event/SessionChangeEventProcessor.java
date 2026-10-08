package com.se2030.vaccinationportal.appointment.event;

import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentChangeType;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.ModuleEventConsumption;
import com.se2030.vaccinationportal.appointment.persistence.model.ModuleEventConsumptionStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.ModuleEventConsumptionRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.campaign.api.session.SessionChangeNotice;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies each durable MF-02 session change to MF-03 at most once. */
@Service
public class SessionChangeEventProcessor {
    public static final String CONSUMER_CODE = "MF03_SESSION_CHANGE";
    private static final List<AppointmentStatus> ACTIVE_APPOINTMENTS =
            List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN);

    private final ModuleEventConsumptionRepository consumptions;
    private final AppointmentRepository appointments;
    private final WaitlistRepository waitlists;
    private final SlotCapacityPort capacity;
    private final Mf03HistoryRecorder history;

    public SessionChangeEventProcessor(ModuleEventConsumptionRepository consumptions,
            AppointmentRepository appointments, WaitlistRepository waitlists,
            @Qualifier("timeSlotCapacityService") SlotCapacityPort capacity,
            Mf03HistoryRecorder history) {
        this.consumptions = consumptions;
        this.appointments = appointments;
        this.waitlists = waitlists;
        this.capacity = capacity;
        this.history = history;
    }

    @Transactional
    public void process(SessionChangeNotice notice) {
        ModuleEventConsumption consumption = consumptions
                .findByConsumerCodeAndSourceEventId(CONSUMER_CODE, notice.eventId())
                .orElseGet(() -> consumptions.saveAndFlush(
                        new ModuleEventConsumption(CONSUMER_CODE, notice.eventId())));
        if (consumption.getStatus() == ModuleEventConsumptionStatus.COMPLETED) {
            return;
        }
        if (consumption.getStatus() == ModuleEventConsumptionStatus.FAILED) {
            consumption.retry();
        }

        switch (notice.changeType()) {
            case "CANCELLED", "SERIES_CANCELLED" -> cancelSessionBookings(notice);
            case "RESCHEDULED", "CENTER_TRANSFERRED", "SERIES_RESCHEDULED" ->
                    recordChangedSession(notice);
            case "BOOKING_SUSPENDED", "BOOKING_RESUMED" -> {
                // Booking policy changes affect future reservations only.
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported session change type: " + notice.changeType());
        }
        consumption.complete();
        consumptions.save(consumption);
    }

    private void cancelSessionBookings(SessionChangeNotice notice) {
        for (Appointment appointment : appointments.findBySlotIdInAndStatusIn(
                notice.slotIds(), ACTIVE_APPOINTMENTS)) {
            if (!capacity.releaseCapacity(appointment.getSlotId())) {
                throw new IllegalStateException(
                        "Failed to release capacity for slot ID: " + appointment.getSlotId());
            }
            appointment.cancel(notice.createdAt());
            Appointment saved = appointments.save(appointment);
            history.appointmentFromSessionChange(saved, AppointmentChangeType.SESSION_CANCELLED,
                    notice.eventId(), notice.reason(), notice.actorId(), notice.createdAt());
            history.notification(saved, AppointmentChangeType.SESSION_CANCELLED,
                    "Your vaccination session was cancelled. Please choose another available session.");
        }

        for (WaitlistEntry entry : waitlists.findBySessionIdAndStatusOrderByJoinedAtAscIdAsc(
                notice.sessionId(), WaitlistStatus.WAITING)) {
            WaitlistStatus oldStatus = entry.getStatus();
            entry.markSessionCancelled();
            WaitlistEntry saved = waitlists.save(entry);
            history.waitlistFromSessionChange(saved, oldStatus, notice.eventId(),
                    notice.reason(), notice.actorId(), notice.createdAt());
        }
    }

    private void recordChangedSession(SessionChangeNotice notice) {
        String message = "Your vaccination session details changed from " + notice.oldStartAt()
                + " to " + notice.newStartAt() + ". Please review your appointment.";
        for (Appointment appointment : appointments.findBySlotIdInAndStatusIn(
                notice.slotIds(), ACTIVE_APPOINTMENTS)) {
            history.appointmentFromSessionChange(appointment,
                    AppointmentChangeType.SESSION_CHANGE_RECORDED, notice.eventId(),
                    notice.reason(), notice.actorId(), notice.createdAt());
            history.notification(appointment, AppointmentChangeType.SESSION_CHANGE_RECORDED, message);
        }
        for (WaitlistEntry entry : waitlists.findBySessionIdAndStatusOrderByJoinedAtAscIdAsc(
                notice.sessionId(), WaitlistStatus.WAITING)) {
            history.waitlistFromSessionChange(entry, entry.getStatus(), notice.eventId(),
                    notice.reason(), notice.actorId(), notice.createdAt());
        }
    }
}
