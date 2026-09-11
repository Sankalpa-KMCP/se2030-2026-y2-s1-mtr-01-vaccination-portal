package com.se2030.vaccinationportal.campaign.model;

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
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
 * Assignment of a staff member to a vaccination session interval.
 * Uses half-open {@code [startAt, endAt)} interval semantics.
 * Canonical Section 6.1, 6.4 and Table 12.1.
 */
@Entity
@Table(
        name = "session_staff_assignments",
        indexes = {
                @Index(name = "idx_staff_assignment_roster_window", columnList = "staff_roster_id, start_at, end_at"),
                @Index(name = "idx_staff_assignment_user_window", columnList = "user_id, start_at, end_at"),
                @Index(name = "idx_staff_assignment_session", columnList = "session_id")
        }
)
public class StaffAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Session is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private VaccinationSession session;

    @NotNull(message = "Staff roster entry is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_roster_id", nullable = false)
    private OperationalStaffRoster staffRoster;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Staff role is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private StaffRole role;

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

    public StaffAssignment() {
    }

    public StaffAssignment(VaccinationSession session,
                           OperationalStaffRoster staffRoster,
                           StaffRole role,
                           Instant startAt,
                           Instant endAt) {
        if (session == null) {
            throw new IllegalArgumentException("Session is required");
        }
        if (staffRoster == null) {
            throw new IllegalArgumentException("Staff roster is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("Staff role is required");
        }
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("Start and end times are required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Assignment start time must be before end time");
        }
        this.session = session;
        this.staffRoster = staffRoster;
        this.userId = staffRoster.getUserId();
        this.role = role;
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
        if (this.userId == null && this.staffRoster != null) {
            this.userId = this.staffRoster.getUserId();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.userId == null && this.staffRoster != null) {
            this.userId = this.staffRoster.getUserId();
        }
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

    public OperationalStaffRoster getStaffRoster() {
        return staffRoster;
    }

    public void setStaffRoster(OperationalStaffRoster staffRoster) {
        this.staffRoster = staffRoster;
        if (staffRoster != null) {
            this.userId = staffRoster.getUserId();
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public StaffRole getRole() {
        return role;
    }

    public void setRole(StaffRole role) {
        this.role = role;
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
        if (!(o instanceof StaffAssignment that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
