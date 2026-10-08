package com.se2030.vaccinationportal.appointment.event;

import com.se2030.vaccinationportal.appointment.persistence.model.*;
import com.se2030.vaccinationportal.appointment.persistence.repository.*;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Writes append-only MF-03 history and durable notification requests inside the business transaction. */
@Service
public class Mf03HistoryRecorder {
    private final AppointmentChangeEventRepository appointmentChanges;
    private final WaitlistChangeEventRepository waitlistChanges;
    private final CheckInEventRepository checkIns;
    private final AppointmentNotificationOutboxRepository notifications;

    public Mf03HistoryRecorder(AppointmentChangeEventRepository appointmentChanges,
            WaitlistChangeEventRepository waitlistChanges, CheckInEventRepository checkIns,
            AppointmentNotificationOutboxRepository notifications) {
        this.appointmentChanges = appointmentChanges;
        this.waitlistChanges = waitlistChanges;
        this.checkIns = checkIns;
        this.notifications = notifications;
    }

    public void appointment(Appointment appointment, AppointmentChangeType type,
            Long oldSlotId, Long sourceEventId, String reason, Instant at) {
        appointmentChanges.save(new AppointmentChangeEvent(appointment.getId(), sourceEventId, type,
                oldSlotId, appointment.getSlotId(), reason, actorId(), at));
    }

    public void waitlist(WaitlistEntry entry, WaitlistStatus oldStatus, Long sourceEventId,
            String reason, Instant at) {
        waitlistChanges.save(new WaitlistChangeEvent(entry.getId(), sourceEventId, oldStatus,
                entry.getStatus(), reason, actorId(), at));
    }

    public void checkIn(Appointment appointment, CheckInEventType type, String reason, Instant at) {
        checkIns.save(new CheckInEvent(appointment.getId(), type, actorId(), at, reason));
    }

    public void notification(Appointment appointment, AppointmentChangeType type, String message) {
        notifications.save(new AppointmentNotificationOutbox(appointment.getId(), appointment.getCitizenId(),
                type.name(), message));
    }

    public void appointmentFromSessionChange(Appointment appointment, AppointmentChangeType type,
            Long sourceEventId, String reason, String actorId, Instant at) {
        appointmentChanges.save(new AppointmentChangeEvent(appointment.getId(), sourceEventId, type,
                appointment.getSlotId(), appointment.getSlotId(), reason, actorId, at));
    }

    public void waitlistFromSessionChange(WaitlistEntry entry, WaitlistStatus oldStatus,
            Long sourceEventId, String reason, String actorId, Instant at) {
        waitlistChanges.save(new WaitlistChangeEvent(entry.getId(), sourceEventId, oldStatus,
                entry.getStatus(), reason, actorId, at));
    }

    private static String actorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return String.valueOf(principal.getUserId());
        }
        return "SYSTEM";
    }
}
