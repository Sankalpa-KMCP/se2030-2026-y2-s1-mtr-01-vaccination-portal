package com.se2030.vaccinationportal.campaign.center.model;

import jakarta.persistence.Column;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Temporary unavailability interval for a vaccination center.
 * Active intervals use half-open {@code [startAt, endAt)} semantics.
 */
@Entity
@Table(
        name = "center_closures",
        indexes = {
                @Index(name = "idx_center_closure_center_start", columnList = "center_id, start_at, id"),
                @Index(name = "idx_center_closure_active_overlap", columnList = "center_id, status, start_at, end_at")
        }
)
public class CenterClosure {

    public static final int MAX_REASON_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Center is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private VaccinationCenter center;

    @NotNull(message = "Closure start is required")
    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @NotNull(message = "Closure end is required")
    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @NotBlank(message = "Closure reason is required")
    @Size(max = MAX_REASON_LENGTH, message = "Closure reason must not exceed 255 characters")
    @Column(name = "reason", nullable = false, length = MAX_REASON_LENGTH)
    private String reason;

    @NotNull(message = "Closure status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CenterClosureStatus status = CenterClosureStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CenterClosure() {
    }

    public CenterClosure(VaccinationCenter center, Instant startAt, Instant endAt, String reason) {
        if (center == null) {
            throw new IllegalArgumentException("Center is required");
        }
        validateCandidate(startAt, endAt, reason);
        this.center = center;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason.trim();
    }

    public static void validateCandidate(Instant startAt, Instant endAt, String reason) {
        if (startAt == null) {
            throw new IllegalArgumentException("Closure start is required");
        }
        if (endAt == null) {
            throw new IllegalArgumentException("Closure end is required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Closure start must be before closure end");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Closure reason is required");
        }
        if (reason.trim().length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("Closure reason must not exceed 255 characters");
        }
    }

    public void validateConstraints() {
        if (center == null) {
            throw new IllegalArgumentException("Center is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Closure status is required");
        }
        validateCandidate(startAt, endAt, reason);
    }

    public void updateDetails(Instant startAt, Instant endAt, String reason) {
        if (status != CenterClosureStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active closure can be updated");
        }
        validateCandidate(startAt, endAt, reason);
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason.trim();
    }

    public void cancel() {
        if (status != CenterClosureStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active closure can be cancelled");
        }
        status = CenterClosureStatus.CANCELLED;
    }

    @PrePersist
    protected void onCreate() {
        validateConstraints();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        validateConstraints();
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public VaccinationCenter getCenter() {
        return center;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public String getReason() {
        return reason;
    }

    public CenterClosureStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "CenterClosure{" +
                "id=" + id +
                ", centerId=" + (center != null ? center.getId() : null) +
                ", startAt=" + startAt +
                ", endAt=" + endAt +
                ", status=" + status +
                '}';
    }
}
