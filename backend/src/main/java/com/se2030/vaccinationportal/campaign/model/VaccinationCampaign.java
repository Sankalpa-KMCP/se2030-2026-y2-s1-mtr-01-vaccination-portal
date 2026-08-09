package com.se2030.vaccinationportal.campaign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Core domain entity representing a Vaccination Campaign.
 * Governs service dates, booking intake windows, and campaign lifecycle status.
 * Preserves business rule MF02-BR-01 (date sequencing and booking window consistency).
 */
@Entity
@Table(name = "vaccination_campaigns")
public class VaccinationCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Campaign code is required")
    @Size(max = 64, message = "Campaign code must not exceed 64 characters")
    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @NotBlank(message = "Campaign name is required")
    @Size(max = 128, message = "Campaign name must not exceed 128 characters")
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    @NotNull(message = "Vaccine type is required")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vaccine_type_id", nullable = false)
    private VaccineType vaccineType;

    @NotNull(message = "Target group is required")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "target_group_id", nullable = false)
    private TargetGroup targetGroup;

    @NotNull(message = "Service start date is required")
    @Column(name = "service_start_date", nullable = false)
    private LocalDate serviceStartDate;

    @NotNull(message = "Service end date is required")
    @Column(name = "service_end_date", nullable = false)
    private LocalDate serviceEndDate;

    @NotNull(message = "Booking open timestamp is required")
    @Column(name = "booking_open_at", nullable = false)
    private LocalDateTime bookingOpenAt;

    @NotNull(message = "Booking close timestamp is required")
    @Column(name = "booking_close_at", nullable = false)
    private LocalDateTime bookingCloseAt;

    @NotNull(message = "Campaign status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public VaccinationCampaign() {
    }

    public VaccinationCampaign(String code,
                               String name,
                               String description,
                               VaccineType vaccineType,
                               TargetGroup targetGroup,
                               LocalDate serviceStartDate,
                               LocalDate serviceEndDate,
                               LocalDateTime bookingOpenAt,
                               LocalDateTime bookingCloseAt,
                               CampaignStatus status) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.vaccineType = vaccineType;
        this.targetGroup = targetGroup;
        this.serviceStartDate = serviceStartDate;
        this.serviceEndDate = serviceEndDate;
        this.bookingOpenAt = bookingOpenAt;
        this.bookingCloseAt = bookingCloseAt;
        this.status = status != null ? status : CampaignStatus.DRAFT;
        validateDateConsistency();
    }

    /**
     * Validates business rule MF02-BR-01:
     * - Campaign end date must be strictly after campaign start date.
     * - Booking close timestamp must be strictly after booking open timestamp.
     */
    public void validateDateConsistency() {
        if (serviceStartDate != null && serviceEndDate != null) {
            if (!serviceEndDate.isAfter(serviceStartDate)) {
                throw new IllegalArgumentException(
                        "Campaign service end date (" + serviceEndDate + ") must be after service start date (" + serviceStartDate + ") [MF02-BR-01]"
                );
            }
        }
        if (bookingOpenAt != null && bookingCloseAt != null) {
            if (!bookingCloseAt.isAfter(bookingOpenAt)) {
                throw new IllegalArgumentException(
                        "Campaign booking close time (" + bookingCloseAt + ") must be after booking open time (" + bookingOpenAt + ") [MF02-BR-01]"
                );
            }
        }
    }

    /**
     * Checks if citizen booking / screening is currently permitted (MF02-BR-03):
     * Campaign must be in PUBLISHED or ACTIVE status and within the active booking window.
     *
     * @param at Current evaluation timestamp
     * @return true if status permits intake and current time is within [bookingOpenAt, bookingCloseAt]
     */
    public boolean isBookingWindowOpenAt(LocalDateTime at) {
        if (at == null || status == null || !status.isIntakePermitted()) {
            return false;
        }
        return !at.isBefore(bookingOpenAt) && !at.isAfter(bookingCloseAt);
    }

    @PrePersist
    protected void onCreate() {
        validateDateConsistency();
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = CampaignStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        validateDateConsistency();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VaccineType getVaccineType() {
        return vaccineType;
    }

    public void setVaccineType(VaccineType vaccineType) {
        this.vaccineType = vaccineType;
    }

    public TargetGroup getTargetGroup() {
        return targetGroup;
    }

    public void setTargetGroup(TargetGroup targetGroup) {
        this.targetGroup = targetGroup;
    }

    public LocalDate getServiceStartDate() {
        return serviceStartDate;
    }

    public void setServiceStartDate(LocalDate serviceStartDate) {
        this.serviceStartDate = serviceStartDate;
    }

    public LocalDate getServiceEndDate() {
        return serviceEndDate;
    }

    public void setServiceEndDate(LocalDate serviceEndDate) {
        this.serviceEndDate = serviceEndDate;
    }

    public LocalDateTime getBookingOpenAt() {
        return bookingOpenAt;
    }

    public void setBookingOpenAt(LocalDateTime bookingOpenAt) {
        this.bookingOpenAt = bookingOpenAt;
    }

    public LocalDateTime getBookingCloseAt() {
        return bookingCloseAt;
    }

    public void setBookingCloseAt(LocalDateTime bookingCloseAt) {
        this.bookingCloseAt = bookingCloseAt;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
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
        VaccinationCampaign that = (VaccinationCampaign) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "VaccinationCampaign{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", serviceStartDate=" + serviceStartDate +
                ", serviceEndDate=" + serviceEndDate +
                ", bookingOpenAt=" + bookingOpenAt +
                ", bookingCloseAt=" + bookingCloseAt +
                ", status=" + status +
                '}';
    }
}
