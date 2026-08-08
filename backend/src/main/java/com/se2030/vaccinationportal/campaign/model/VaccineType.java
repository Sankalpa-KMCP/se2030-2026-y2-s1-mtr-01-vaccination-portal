package com.se2030.vaccinationportal.campaign.model;

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
import java.time.LocalDateTime;
import java.util.Objects;

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
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaccineType that = (VaccineType) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
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
