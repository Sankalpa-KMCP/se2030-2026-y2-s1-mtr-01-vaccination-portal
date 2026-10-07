package com.se2030.vaccinationportal.campaign.reference.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Shared master entity representing a type of vaccine (e.g. mRNA, Inactivated, Viral Vector).
 * Owned by MF-02 and referenced read-only across other business modules (e.g. MF-04).
 */
@Entity
@Table(name = "vaccine_types")
public class VaccineType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Vaccine type code is required")
    @Size(max = 64, message = "Vaccine type code must not exceed 64 characters")
    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @NotBlank(message = "Vaccine type name is required")
    @Size(max = 128, message = "Vaccine type name must not exceed 128 characters")
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @NotBlank(message = "Dose unit is required")
    @Size(max = 32, message = "Dose unit must not exceed 32 characters")
    @Column(name = "dose_unit", nullable = false, length = 32)
    private String doseUnit = "dose";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public VaccineType() {
    }

    public VaccineType(String code, String name, String doseUnit, boolean active) {
        this.code = code;
        this.name = name;
        this.doseUnit = doseUnit;
        this.active = active;
    }

    public VaccineType(String code, String name) {
        this(code, name, "dose", true);
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

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDoseUnit() {
        return doseUnit;
    }

    public void setDoseUnit(String doseUnit) {
        this.doseUnit = doseUnit;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "VaccineType{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", doseUnit='" + doseUnit + '\'' +
                ", active=" + active +
                '}';
    }
}
