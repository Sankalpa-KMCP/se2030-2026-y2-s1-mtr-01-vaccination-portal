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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;

/**
 * Stable operational staff roster registry entry for a vaccination center.
 * Serves as the authoritative locking anchor for staff scheduling concurrency in MF-02.
 */
@Entity
@Table(
        name = "operational_staff_rosters",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_center_staff_user",
                        columnNames = {"center_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_operational_staff_user", columnList = "user_id"),
                @Index(name = "idx_operational_staff_center_role", columnList = "center_id, role, status")
        }
)
public class OperationalStaffRoster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Center is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private VaccinationCenter center;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Staff role is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private StaffRole role;

    @NotNull(message = "Staff status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OperationalStaffStatus status = OperationalStaffStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public OperationalStaffRoster() {
    }

    public OperationalStaffRoster(VaccinationCenter center,
                                  Long userId,
                                  StaffRole role,
                                  OperationalStaffStatus status) {
        if (center == null) {
            throw new IllegalArgumentException("Center is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("Staff role is required");
        }
        this.center = center;
        this.userId = userId;
        this.role = role;
        this.status = status != null ? status : OperationalStaffStatus.ACTIVE;
    }

    public OperationalStaffRoster(VaccinationCenter center,
                                  Long userId,
                                  StaffRole role) {
        this(center, userId, role, OperationalStaffStatus.ACTIVE);
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
        if (this.status == null) {
            this.status = OperationalStaffStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public VaccinationCenter getCenter() {
        return center;
    }

    public void setCenter(VaccinationCenter center) {
        this.center = center;
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

    public OperationalStaffStatus getStatus() {
        return status;
    }

    public void setStatus(OperationalStaffStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return this.status == OperationalStaffStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OperationalStaffRoster that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
