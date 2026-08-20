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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;

/**
 * Center-owned physical operational resource (room, vaccination station, or equipment).
 * Canonical Section 6.1, 6.4 and Table 12.1.
 */
@Entity
@Table(
        name = "operational_resources",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_center_resource_code",
                        columnNames = {"center_id", "code"}
                )
        },
        indexes = {
                @Index(name = "idx_operational_resource_center_type", columnList = "center_id, resource_type, status")
        }
)
public class OperationalResource {

    public static final int MAX_CODE_LENGTH = 64;
    public static final int MAX_NAME_LENGTH = 128;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Center is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private VaccinationCenter center;

    @NotBlank(message = "Resource code is required")
    @Size(max = MAX_CODE_LENGTH, message = "Resource code must not exceed 64 characters")
    @Column(name = "code", nullable = false, length = MAX_CODE_LENGTH)
    private String code;

    @NotNull(message = "Resource type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private ResourceType resourceType;

    @NotBlank(message = "Resource name is required")
    @Size(max = MAX_NAME_LENGTH, message = "Resource name must not exceed 128 characters")
    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @NotNull(message = "Resource status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OperationalResourceStatus status = OperationalResourceStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public OperationalResource() {
    }

    public OperationalResource(VaccinationCenter center,
                               String code,
                               ResourceType resourceType,
                               String name,
                               OperationalResourceStatus status) {
        if (center == null) {
            throw new IllegalArgumentException("Center is required");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource code is required");
        }
        if (resourceType == null) {
            throw new IllegalArgumentException("Resource type is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource name is required");
        }
        this.center = center;
        this.code = code.trim();
        this.resourceType = resourceType;
        this.name = name.trim();
        this.status = status != null ? status : OperationalResourceStatus.ACTIVE;
    }

    public OperationalResource(VaccinationCenter center,
                               String code,
                               ResourceType resourceType,
                               String name) {
        this(center, code, resourceType, name, OperationalResourceStatus.ACTIVE);
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
            this.status = OperationalResourceStatus.ACTIVE;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OperationalResourceStatus getStatus() {
        return status;
    }

    public void setStatus(OperationalResourceStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return this.status == OperationalResourceStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OperationalResource that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
