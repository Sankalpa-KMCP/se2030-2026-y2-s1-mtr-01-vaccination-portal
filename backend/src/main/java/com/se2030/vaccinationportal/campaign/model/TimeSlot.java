package com.se2030.vaccinationportal.campaign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;

/**
 * Concrete operational time slot within a scheduled vaccination session.
 * Manages bookable capacity and reservation counters for appointment booking integration.
 * Canonical Section 29 and Business Rule MF02 / MF03 Capacity Integration.
 */
@Entity
@Table(
        name = "time_slots",
        indexes = {
                @Index(name = "idx_time_slot_session", columnList = "session_id"),
                @Index(name = "idx_time_slot_start_time", columnList = "start_time")
        }
)
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Session is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private VaccinationSession session;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Min(value = 0, message = "Reserved count cannot be negative")
    @Column(name = "reserved_count", nullable = false)
    private int reservedCount = 0;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TimeSlot() {
    }

    public TimeSlot(VaccinationSession session, Instant startTime, Instant endTime, int capacity) {
        this.session = Objects.requireNonNull(session, "Session must not be null");
        this.startTime = Objects.requireNonNull(startTime, "Start time must not be null");
        this.endTime = Objects.requireNonNull(endTime, "End time must not be null");
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            throw new IllegalArgumentException("Slot end time must be strictly after start time");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Slot capacity must be strictly positive");
        }
        this.capacity = capacity;
        this.reservedCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean hasAvailableCapacity() {
        return reservedCount < capacity;
    }

    public int getAvailableCapacity() {
        return Math.max(0, capacity - reservedCount);
    }

    public boolean reserve() {
        if (reservedCount >= capacity) {
            return false;
        }
        this.reservedCount++;
        return true;
    }

    public boolean release() {
        if (reservedCount <= 0) {
            return false;
        }
        this.reservedCount--;
        return true;
    }

    public Long getId() {
        return id;
    }

    public VaccinationSession getSession() {
        return session;
    }

    public void setSession(VaccinationSession session) {
        this.session = session;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Slot capacity must be strictly positive");
        }
        this.capacity = capacity;
    }

    public int getReservedCount() {
        return reservedCount;
    }

    public void setReservedCount(int reservedCount) {
        if (reservedCount < 0) {
            throw new IllegalArgumentException("Reserved count cannot be negative");
        }
        this.reservedCount = reservedCount;
    }

    public Long getLockVersion() {
        return lockVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot timeSlot)) return false;
        return Objects.equals(id, timeSlot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
