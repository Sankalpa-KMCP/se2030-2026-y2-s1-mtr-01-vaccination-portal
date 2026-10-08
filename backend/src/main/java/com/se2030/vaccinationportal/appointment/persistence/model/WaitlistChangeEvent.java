package com.se2030.vaccinationportal.appointment.persistence.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "waitlist_change_event")
public class WaitlistChangeEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "waitlist_entry_id", nullable = false, updatable = false) private Long waitlistEntryId;
    @Column(name = "source_session_change_event_id", updatable = false) private Long sourceSessionChangeEventId;
    @Enumerated(EnumType.STRING) @Column(name = "old_status", length = 40, updatable = false) private WaitlistStatus oldStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 40, updatable = false) private WaitlistStatus newStatus;
    @Column(name = "reason", length = 500, updatable = false) private String reason;
    @Column(name = "actor_id", length = 64, updatable = false) private String actorId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected WaitlistChangeEvent() {}

    public WaitlistChangeEvent(Long waitlistEntryId, Long sourceSessionChangeEventId,
            WaitlistStatus oldStatus, WaitlistStatus newStatus, String reason,
            String actorId, Instant createdAt) {
        if (waitlistEntryId == null || waitlistEntryId <= 0) throw new IllegalArgumentException("Waitlist entry ID is required");
        this.waitlistEntryId = waitlistEntryId;
        this.sourceSessionChangeEventId = sourceSessionChangeEventId;
        this.oldStatus = oldStatus;
        this.newStatus = java.util.Objects.requireNonNull(newStatus, "New status is required");
        this.reason = normalize(reason, 500);
        this.actorId = normalize(actorId, 64);
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    private static String normalize(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new IllegalArgumentException("Value exceeds " + max + " characters");
        return trimmed;
    }

    public Long getId() { return id; }
    public Long getWaitlistEntryId() { return waitlistEntryId; }
    public Long getSourceSessionChangeEventId() { return sourceSessionChangeEventId; }
    public WaitlistStatus getOldStatus() { return oldStatus; }
    public WaitlistStatus getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public String getActorId() { return actorId; }
    public Instant getCreatedAt() { return createdAt; }
}
