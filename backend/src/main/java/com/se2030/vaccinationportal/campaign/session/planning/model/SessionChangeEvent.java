package com.se2030.vaccinationportal.campaign.session.planning.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Append-only audit record capturing material schedule and routing changes applied to a vaccination session.
 */
@Entity
@Table(
        name = "session_change_events",
        indexes = {
                @Index(name = "idx_session_change_session_created",
                        columnList = "session_id, created_at, id")
        }
)
public class SessionChangeEvent {

    public static final int MAX_REASON_LENGTH = 500;
    public static final int MAX_ACTOR_ID_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Vaccination session is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private VaccinationSession session;

    @NotNull(message = "Change type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 32, updatable = false)
    private SessionChangeType changeType;

    @NotNull(message = "Old snapshot is required")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "centerId", column = @Column(name = "old_center_id", nullable = false, updatable = false)),
            @AttributeOverride(name = "scheduleTimezone", column = @Column(name = "old_schedule_timezone", nullable = false, length = 64, updatable = false)),
            @AttributeOverride(name = "startAt", column = @Column(name = "old_start_at", nullable = false, updatable = false)),
            @AttributeOverride(name = "endAt", column = @Column(name = "old_end_at", nullable = false, updatable = false)),
            @AttributeOverride(name = "bookingCloseAt", column = @Column(name = "old_booking_close_at", nullable = false, updatable = false)),
            @AttributeOverride(name = "lifecycleStatus", column = @Column(name = "old_lifecycle_status", nullable = false, length = 16, updatable = false))
    })
    private SessionScheduleSnapshot oldSnapshot;

    @NotNull(message = "New snapshot is required")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "centerId", column = @Column(name = "new_center_id", nullable = false, updatable = false)),
            @AttributeOverride(name = "scheduleTimezone", column = @Column(name = "new_schedule_timezone", nullable = false, length = 64, updatable = false)),
            @AttributeOverride(name = "startAt", column = @Column(name = "new_start_at", nullable = false, updatable = false)),
            @AttributeOverride(name = "endAt", column = @Column(name = "new_end_at", nullable = false, updatable = false)),
            @AttributeOverride(name = "bookingCloseAt", column = @Column(name = "new_booking_close_at", nullable = false, updatable = false)),
            @AttributeOverride(name = "lifecycleStatus", column = @Column(name = "new_lifecycle_status", nullable = false, length = 16, updatable = false))
    })
    private SessionScheduleSnapshot newSnapshot;

    @NotNull(message = "Reason is required")
    @Column(name = "reason", nullable = false, length = MAX_REASON_LENGTH, updatable = false)
    private String reason;

    @Column(name = "affected_appointment_count", nullable = false, updatable = false)
    private int affectedAppointmentCount;

    @Column(name = "affected_waitlist_count", nullable = false, updatable = false)
    private int affectedWaitlistCount;

    @NotNull(message = "Actor ID is required")
    @Column(name = "actor_id", nullable = false, length = MAX_ACTOR_ID_LENGTH, updatable = false)
    private String actorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SessionChangeEvent() {
    }

    public SessionChangeEvent(
            VaccinationSession session,
            SessionChangeType changeType,
            SessionScheduleSnapshot oldSnapshot,
            SessionScheduleSnapshot newSnapshot,
            String reason,
            int affectedAppointmentCount,
            int affectedWaitlistCount,
            String actorId
    ) {
        if (session == null) {
            throw new IllegalArgumentException("Vaccination session is required");
        }
        if (changeType == null) {
            throw new IllegalArgumentException("Change type is required");
        }
        if (oldSnapshot == null) {
            throw new IllegalArgumentException("Old snapshot is required");
        }
        if (newSnapshot == null) {
            throw new IllegalArgumentException("New snapshot is required");
        }
        String validatedReason = validateReason(reason);
        if (affectedAppointmentCount < 0) {
            throw new IllegalArgumentException("Affected appointment count cannot be negative");
        }
        if (affectedWaitlistCount < 0) {
            throw new IllegalArgumentException("Affected waitlist count cannot be negative");
        }
        String validatedActorId = validateActorId(actorId);

        this.session = session;
        this.changeType = changeType;
        this.oldSnapshot = oldSnapshot;
        this.newSnapshot = newSnapshot;
        this.reason = validatedReason;
        this.affectedAppointmentCount = affectedAppointmentCount;
        this.affectedWaitlistCount = affectedWaitlistCount;
        this.actorId = validatedActorId;
    }

    public static String validateReason(String reason) {
        if (reason == null) {
            throw new IllegalArgumentException("Reason is required");
        }
        String trimmed = reason.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be blank");
        }
        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("Reason cannot exceed 500 characters");
        }
        return trimmed;
    }

    public static String validateActorId(String actorId) {
        if (actorId == null) {
            throw new IllegalArgumentException("Actor ID is required");
        }
        String trimmed = actorId.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Actor ID cannot be blank");
        }
        if (trimmed.length() > MAX_ACTOR_ID_LENGTH) {
            throw new IllegalArgumentException("Actor ID cannot exceed 64 characters");
        }
        return trimmed;
    }

    private void validateConstraints() {
        if (session == null) {
            throw new IllegalArgumentException("Vaccination session is required");
        }
        if (changeType == null) {
            throw new IllegalArgumentException("Change type is required");
        }
        if (oldSnapshot == null) {
            throw new IllegalArgumentException("Old snapshot is required");
        }
        if (newSnapshot == null) {
            throw new IllegalArgumentException("New snapshot is required");
        }
        validateReason(this.reason);
        if (affectedAppointmentCount < 0) {
            throw new IllegalArgumentException("Affected appointment count cannot be negative");
        }
        if (affectedWaitlistCount < 0) {
            throw new IllegalArgumentException("Affected waitlist count cannot be negative");
        }
        validateActorId(this.actorId);
    }

    @PrePersist
    protected void onCreate() {
        validateConstraints();
        this.reason = this.reason.trim();
        this.actorId = this.actorId.trim();
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        throw new IllegalStateException("Session change events are immutable and cannot be updated");
    }

    public Long getId() {
        return id;
    }

    public VaccinationSession getSession() {
        return session;
    }

    public SessionChangeType getChangeType() {
        return changeType;
    }

    public SessionScheduleSnapshot getOldSnapshot() {
        return oldSnapshot;
    }

    public SessionScheduleSnapshot getNewSnapshot() {
        return newSnapshot;
    }

    public String getReason() {
        return reason;
    }

    public int getAffectedAppointmentCount() {
        return affectedAppointmentCount;
    }

    public int getAffectedWaitlistCount() {
        return affectedWaitlistCount;
    }

    public String getActorId() {
        return actorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
