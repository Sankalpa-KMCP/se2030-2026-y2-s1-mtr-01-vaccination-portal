package com.se2030.vaccinationportal.appointment.persistence.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "appointment_change_event")
public class AppointmentChangeEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "appointment_id", nullable = false, updatable = false) private Long appointmentId;
    @Column(name = "source_session_change_event_id", updatable = false) private Long sourceSessionChangeEventId;
    @Enumerated(EnumType.STRING) @Column(name = "change_type", nullable = false, length = 40, updatable = false)
    private AppointmentChangeType changeType;
    @Column(name = "old_slot_id", updatable = false) private Long oldSlotId;
    @Column(name = "new_slot_id", updatable = false) private Long newSlotId;
    @Column(name = "reason", length = 500, updatable = false) private String reason;
    @Column(name = "actor_id", length = 64, updatable = false) private String actorId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected AppointmentChangeEvent() {}

    public AppointmentChangeEvent(Long appointmentId, Long sourceSessionChangeEventId,
            AppointmentChangeType changeType, Long oldSlotId, Long newSlotId,
            String reason, String actorId, Instant createdAt) {
        this.appointmentId = requirePositive(appointmentId, "Appointment ID");
        this.sourceSessionChangeEventId = sourceSessionChangeEventId;
        this.changeType = java.util.Objects.requireNonNull(changeType, "Change type is required");
        this.oldSlotId = oldSlotId;
        this.newSlotId = newSlotId;
        this.reason = normalize(reason, 500);
        this.actorId = normalize(actorId, 64);
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    private static Long requirePositive(Long value, String label) {
        if (value == null || value <= 0) throw new IllegalArgumentException(label + " is required");
        return value;
    }
    private static String normalize(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new IllegalArgumentException("Value exceeds " + max + " characters");
        return trimmed;
    }

    public Long getId() { return id; }
    public Long getAppointmentId() { return appointmentId; }
    public Long getSourceSessionChangeEventId() { return sourceSessionChangeEventId; }
    public AppointmentChangeType getChangeType() { return changeType; }
    public Long getOldSlotId() { return oldSlotId; }
    public Long getNewSlotId() { return newSlotId; }
    public String getReason() { return reason; }
    public String getActorId() { return actorId; }
    public Instant getCreatedAt() { return createdAt; }
}
