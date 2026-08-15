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
import java.time.Instant;

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
    private Instant bookingOpenAt;

    @NotNull(message = "Booking close timestamp is required")
    @Column(name = "booking_close_at", nullable = false)
    private Instant bookingCloseAt;

    @NotNull(message = "Campaign status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VaccinationCampaign() {
    }

    public VaccinationCampaign(String code,
                               String name,
                               String description,
                               VaccineType vaccineType,
                               TargetGroup targetGroup,
                               LocalDate serviceStartDate,
                               LocalDate serviceEndDate,
                               Instant bookingOpenAt,
                               Instant bookingCloseAt) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.vaccineType = vaccineType;
        this.targetGroup = targetGroup;
        this.serviceStartDate = serviceStartDate;
        this.serviceEndDate = serviceEndDate;
        this.bookingOpenAt = bookingOpenAt;
        this.bookingCloseAt = bookingCloseAt;
        this.status = CampaignStatus.DRAFT;
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
    public boolean isBookingWindowOpenAt(Instant at) {
        if (at == null || status == null || !status.isIntakePermitted()) {
            return false;
        }
        return !at.isBefore(bookingOpenAt) && !at.isAfter(bookingCloseAt);
    }

    @PrePersist
    protected void onCreate() {
        validateDateConsistency();
        Instant now = Instant.now();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VaccineType getVaccineType() {
        return vaccineType;
    }

    public TargetGroup getTargetGroup() {
        return targetGroup;
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

    public Instant getBookingOpenAt() {
        return bookingOpenAt;
    }

    public void setBookingOpenAt(Instant bookingOpenAt) {
        this.bookingOpenAt = bookingOpenAt;
    }

    public Instant getBookingCloseAt() {
        return bookingCloseAt;
    }

    public void setBookingCloseAt(Instant bookingCloseAt) {
        this.bookingCloseAt = bookingCloseAt;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    /**
     * Transitions campaign status to SCHEDULED.
     * Allowed only from DRAFT status.
     */
    public void schedule() {
        if (this.status != CampaignStatus.DRAFT) {
            throw new IllegalStateException(
                    "Cannot schedule campaign in " + this.status + " status. Only DRAFT campaigns can be scheduled."
            );
        }
        this.status = CampaignStatus.SCHEDULED;
    }

    /**
     * Transitions campaign status to PUBLISHED.
     * Allowed only from DRAFT or SCHEDULED status.
     */
    public void publish() {
        if (this.status != CampaignStatus.DRAFT && this.status != CampaignStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Cannot publish campaign in " + this.status + " status. Only DRAFT or SCHEDULED campaigns can be published."
            );
        }
        this.status = CampaignStatus.PUBLISHED;
    }

    /**
     * Transitions campaign status to ACTIVE.
     * Allowed only from PUBLISHED status.
     */
    public void activate() {
        if (this.status != CampaignStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Cannot activate campaign in " + this.status + " status. Only PUBLISHED campaigns can be activated."
            );
        }
        this.status = CampaignStatus.ACTIVE;
    }

    /**
     * Transitions campaign status to SUSPENDED.
     * Allowed only from PUBLISHED or ACTIVE status.
     */
    public void suspend() {
        if (this.status != CampaignStatus.PUBLISHED && this.status != CampaignStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot suspend campaign in " + this.status + " status. Only PUBLISHED or ACTIVE campaigns can be suspended."
            );
        }
        this.status = CampaignStatus.SUSPENDED;
    }

    /**
     * Transitions campaign status to PUBLISHED upon resumption before service start.
     * Allowed only from SUSPENDED status.
     */
    public void resumeToPublished() {
        if (this.status != CampaignStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Cannot resume campaign in " + this.status + " status. Only SUSPENDED campaigns can be resumed."
            );
        }
        this.status = CampaignStatus.PUBLISHED;
    }

    /**
     * Transitions campaign status to ACTIVE upon resumption during service period.
     * Allowed only from SUSPENDED status.
     */
    public void resumeToActive() {
        if (this.status != CampaignStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Cannot resume campaign in " + this.status + " status. Only SUSPENDED campaigns can be resumed."
            );
        }
        this.status = CampaignStatus.ACTIVE;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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
