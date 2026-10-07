package com.se2030.vaccinationportal.campaign.center.model;

import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;


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
import java.time.LocalDate;

/** Effective-dated operational authorization for a center to serve a campaign. */
@Entity
@Table(
        name = "campaign_center_assignments",
        indexes = {
                @Index(name = "idx_campaign_center_assignment_campaign_history",
                        columnList = "campaign_id, active_from, id"),
                @Index(name = "idx_campaign_center_assignment_center_history",
                        columnList = "center_id, active_from, id"),
                @Index(name = "idx_campaign_center_assignment_overlap",
                        columnList = "campaign_id, center_id, assignment_status, active_from, active_to")
        }
)
public class CampaignCenterAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Campaign is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private VaccinationCampaign campaign;

    @NotNull(message = "Center is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private VaccinationCenter center;

    @NotNull(message = "Assignment active-from date is required")
    @Column(name = "active_from", nullable = false)
    private LocalDate activeFrom;

    @Column(name = "active_to")
    private LocalDate activeTo;

    @NotNull(message = "Assignment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false, length = 16)
    private CampaignCenterAssignmentStatus status = CampaignCenterAssignmentStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CampaignCenterAssignment() {
    }

    public CampaignCenterAssignment(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            LocalDate activeFrom,
            LocalDate activeTo
    ) {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign is required");
        }
        if (center == null) {
            throw new IllegalArgumentException("Center is required");
        }
        validatePeriod(activeFrom, activeTo);
        this.campaign = campaign;
        this.center = center;
        this.activeFrom = activeFrom;
        this.activeTo = activeTo;
    }

    public static void validatePeriod(LocalDate activeFrom, LocalDate activeTo) {
        if (activeFrom == null) {
            throw new IllegalArgumentException("Assignment active-from date is required");
        }
        if (activeTo != null && !activeFrom.isBefore(activeTo)) {
            throw new IllegalArgumentException("Assignment active-from date must be before active-to date");
        }
    }

    public void updatePeriod(LocalDate activeFrom, LocalDate activeTo) {
        requireActive();
        validatePeriod(activeFrom, activeTo);
        this.activeFrom = activeFrom;
        this.activeTo = activeTo;
    }

    public void endAt(LocalDate newActiveTo) {
        requireActive();
        validatePeriod(activeFrom, newActiveTo);
        if (activeTo != null && !newActiveTo.isBefore(activeTo)) {
            throw new IllegalArgumentException("Assignment end date must shorten the current period");
        }
        activeTo = newActiveTo;
    }

    public void cancel() {
        requireActive();
        status = CampaignCenterAssignmentStatus.CANCELLED;
    }

    public boolean isEffectiveOn(LocalDate date) {
        return date != null
                && status == CampaignCenterAssignmentStatus.ACTIVE
                && !date.isBefore(activeFrom)
                && (activeTo == null || date.isBefore(activeTo));
    }

    private void requireActive() {
        if (status != CampaignCenterAssignmentStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active campaign-center assignment can be changed");
        }
    }

    private void validateConstraints() {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign is required");
        }
        if (center == null) {
            throw new IllegalArgumentException("Center is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Assignment status is required");
        }
        validatePeriod(activeFrom, activeTo);
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

    public VaccinationCampaign getCampaign() {
        return campaign;
    }

    public VaccinationCenter getCenter() {
        return center;
    }

    public LocalDate getActiveFrom() {
        return activeFrom;
    }

    public LocalDate getActiveTo() {
        return activeTo;
    }

    public CampaignCenterAssignmentStatus getStatus() {
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
        return "CampaignCenterAssignment{" +
                "id=" + id +
                ", campaignId=" + (campaign != null ? campaign.getId() : null) +
                ", centerId=" + (center != null ? center.getId() : null) +
                ", activeFrom=" + activeFrom +
                ", activeTo=" + activeTo +
                ", status=" + status +
                '}';
    }
}
