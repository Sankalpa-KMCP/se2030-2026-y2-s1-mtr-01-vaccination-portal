package com.se2030.vaccinationportal.appointment.persistence.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "check_in_event")
public class CheckInEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "appointment_id", nullable = false, updatable = false) private Long appointmentId;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 24, updatable = false)
    private CheckInEventType eventType;
    @Column(name = "actor_id", length = 64, updatable = false) private String actorId;
    @Column(name = "event_at", nullable = false, updatable = false) private Instant eventAt;
    @Column(name = "reason", length = 500, updatable = false) private String reason;

    protected CheckInEvent() {}
    public CheckInEvent(Long appointmentId, CheckInEventType eventType, String actorId, Instant eventAt, String reason) {
        if (appointmentId == null || appointmentId <= 0) throw new IllegalArgumentException("Appointment ID is required");
        this.appointmentId = appointmentId;
        this.eventType = java.util.Objects.requireNonNull(eventType, "Event type is required");
        this.actorId = normalize(actorId, 64);
        this.eventAt = eventAt == null ? Instant.now() : eventAt;
        this.reason = normalize(reason, 500);
    }
    private static String normalize(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new IllegalArgumentException("Value exceeds " + max + " characters");
        return trimmed;
    }
    public Long getId() { return id; }
    public Long getAppointmentId() { return appointmentId; }
    public CheckInEventType getEventType() { return eventType; }
    public String getActorId() { return actorId; }
    public Instant getEventAt() { return eventAt; }
    public String getReason() { return reason; }
}
