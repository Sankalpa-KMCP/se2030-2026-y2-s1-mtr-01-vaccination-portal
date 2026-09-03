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
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;

/**
 * Assignment of an operational resource to a vaccination session interval.
 * Uses half-open {@code [startAt, endAt)} interval semantics.
 * Canonical Section 6.1, 6.4 and Table 12.1.
 */
@Entity
@Table(
        name = "session_resource_assignments",
        indexes = {
                @Index(name = "idx_resource_assignment_resource_window", columnList = "resource_id, start_at, end_at"),
                @Index(name = "idx_resource_assignment_session", columnList = "session_id")
        }
)
public class SessionResourceAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Session is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private VaccinationSession session;

    @NotNull(message = "Resource is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private OperationalResource resource;

    @NotNull(message = "Assignment start time is required")
    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @NotNull(message = "Assignment end time is required")
    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SessionResourceAssignment() {
    }

    public SessionResourceAssignment(VaccinationSession session,
                                     OperationalResource resource,
                                     Instant startAt,
                                     Instant endAt) {
        if (session == null) {
            throw new IllegalArgumentException("Session is required");
        }
        if (resource == null) {
            throw new IllegalArgumentException("Resource is required");
        }
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("Start and end times are required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Assignment start time must be before end time");
        }
        this.session = session;
        this.resource = resource;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
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

    public OperationalResource getResource() {
        return resource;
    }

    public void setResource(OperationalResource resource) {
        this.resource = resource;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
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
        if (!(o instanceof SessionResourceAssignment that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
