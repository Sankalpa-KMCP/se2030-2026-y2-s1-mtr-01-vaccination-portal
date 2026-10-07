package com.se2030.vaccinationportal.campaign.center.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;

/**
 * Stable per-user operational identity anchor for vaccination center staffing in MF-02.
 * Serves as the authoritative, durable database concurrency anchor across all centers,
 * ensuring deterministic serialization even when a staff member has zero initial roster rows.
 */
@Entity
@Table(
        name = "operational_staff_identities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_operational_staff_identity_user",
                        columnNames = {"user_id"}
                )
        }
)
public class OperationalStaffIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OperationalStaffIdentity() {
    }

    public OperationalStaffIdentity(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        this.userId = userId;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OperationalStaffIdentity that)) return false;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
