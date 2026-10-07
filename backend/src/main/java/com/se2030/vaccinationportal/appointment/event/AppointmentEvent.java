package com.se2030.vaccinationportal.appointment.event;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import java.time.Instant;

/** Immutable snapshot: observers never receive a managed JPA entity. */
public record AppointmentEvent(Type type, Long appointmentId, Long citizenId,
        Long campaignId, Long slotId, Instant occurredAt) {
    public enum Type { BOOKED, CANCELLED, WAITLIST_PROMOTED }

    public static AppointmentEvent from(Type type, Appointment appointment) {
        return new AppointmentEvent(type, appointment.getId(), appointment.getCitizenId(),
                appointment.getCampaignId(), appointment.getSlotId(), Instant.now());
    }
}
