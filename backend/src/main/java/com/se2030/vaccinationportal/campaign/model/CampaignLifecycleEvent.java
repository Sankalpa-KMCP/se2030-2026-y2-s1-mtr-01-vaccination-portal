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

/**
 * Append-only audit record for a vaccination campaign lifecycle change.
 */
@Entity
@Table(
        name = "campaign_lifecycle_events",
        indexes = {
                @Index(name = "idx_campaign_lifecycle_campaign_created",
                        columnList = "campaign_id, created_at, id")
        }
)
public class CampaignLifecycleEvent {

    public static final int MAX_REASON_LENGTH = 500;
    public static final int MAX_ACTOR_ID_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Vaccination campaign is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, updatable = false)
    private VaccinationCampaign campaign;

    @NotNull(message = "Lifecycle change type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 32, updatable = false)
    private CampaignLifecycleChangeType changeType;

    @NotNull(message = "Previous campaign status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 32, updatable = false)
    private CampaignStatus previousStatus;

    @NotNull(message = "New campaign status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 32, updatable = false)
    private CampaignStatus newStatus;

    @NotNull(message = "Reason is required")
    @Column(name = "reason", nullable = false, length = MAX_REASON_LENGTH, updatable = false)
    private String reason;

    @NotNull(message = "Actor ID is required")
    @Column(name = "actor_id", nullable = false, length = MAX_ACTOR_ID_LENGTH, updatable = false)
    private String actorId;

    @NotNull(message = "Created timestamp is required")
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CampaignLifecycleEvent() {
    }

    public CampaignLifecycleEvent(
            VaccinationCampaign campaign,
            CampaignLifecycleChangeType changeType,
            CampaignStatus previousStatus,
            CampaignStatus newStatus,
            String reason,
            String actorId,
            Instant createdAt
    ) {
        this.campaign = requireCampaign(campaign);
        this.changeType = requireChangeType(changeType);
        this.previousStatus = requireStatus(previousStatus, "Previous campaign status is required");
        this.newStatus = requireStatus(newStatus, "New campaign status is required");
        this.reason = validateReason(reason);
        this.actorId = validateActorId(actorId);
        this.createdAt = requireCreatedAt(createdAt);
        validateTransition();
    }

    public static String validateReason(String reason) {
        if (reason == null) {
            throw new IllegalArgumentException("Reason is required");
        }
        String trimmed = reason.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be blank");
        }
        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("Reason cannot exceed 500 characters");
        }
        return trimmed;
    }

    public static String validateActorId(String actorId) {
        if (actorId == null) {
            throw new IllegalArgumentException("Actor ID is required");
        }
        String trimmed = actorId.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Actor ID cannot be blank");
        }
        if (trimmed.length() > MAX_ACTOR_ID_LENGTH) {
            throw new IllegalArgumentException("Actor ID cannot exceed 64 characters");
        }
        return trimmed;
    }

    private static VaccinationCampaign requireCampaign(VaccinationCampaign campaign) {
        if (campaign == null) {
            throw new IllegalArgumentException("Vaccination campaign is required");
        }
        return campaign;
    }

    private static CampaignLifecycleChangeType requireChangeType(CampaignLifecycleChangeType changeType) {
        if (changeType == null) {
            throw new IllegalArgumentException("Lifecycle change type is required");
        }
        return changeType;
    }

    private static CampaignStatus requireStatus(CampaignStatus status, String message) {
        if (status == null) {
            throw new IllegalArgumentException(message);
        }
        return status;
    }

    private static Instant requireCreatedAt(Instant createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp is required");
        }
        return createdAt;
    }

    private void validateTransition() {
        if (changeType == CampaignLifecycleChangeType.SUSPENDED
                && (newStatus != CampaignStatus.SUSPENDED
                || (previousStatus != CampaignStatus.PUBLISHED && previousStatus != CampaignStatus.ACTIVE))) {
            throw new IllegalArgumentException(
                    "SUSPENDED lifecycle events require PUBLISHED or ACTIVE as the previous status and SUSPENDED as the new status"
            );
        }
        if (changeType == CampaignLifecycleChangeType.RESUMED
                && (previousStatus != CampaignStatus.SUSPENDED
                || (newStatus != CampaignStatus.PUBLISHED && newStatus != CampaignStatus.ACTIVE))) {
            throw new IllegalArgumentException(
                    "RESUMED lifecycle events require SUSPENDED as the previous status and PUBLISHED or ACTIVE as the new status"
            );
        }
        if (changeType == CampaignLifecycleChangeType.COMPLETED
                && (newStatus != CampaignStatus.COMPLETED
                || (previousStatus != CampaignStatus.ACTIVE
                && previousStatus != CampaignStatus.PUBLISHED
                && previousStatus != CampaignStatus.SUSPENDED))) {
            throw new IllegalArgumentException(
                    "COMPLETED lifecycle events require ACTIVE, PUBLISHED, or SUSPENDED as the previous status and COMPLETED as the new status"
            );
        }
        if (changeType == CampaignLifecycleChangeType.CANCELLED
                && (newStatus != CampaignStatus.CANCELLED
                || (previousStatus != CampaignStatus.DRAFT
                && previousStatus != CampaignStatus.SCHEDULED
                && previousStatus != CampaignStatus.PUBLISHED
                && previousStatus != CampaignStatus.ACTIVE
                && previousStatus != CampaignStatus.SUSPENDED))) {
            throw new IllegalArgumentException(
                    "CANCELLED lifecycle events require DRAFT, SCHEDULED, PUBLISHED, ACTIVE, or SUSPENDED as the previous status and CANCELLED as the new status"
            );
        }
        if (changeType == CampaignLifecycleChangeType.ARCHIVED
                && (newStatus != CampaignStatus.ARCHIVED
                || (previousStatus != CampaignStatus.COMPLETED && previousStatus != CampaignStatus.CANCELLED))) {
            throw new IllegalArgumentException(
                    "ARCHIVED lifecycle events require COMPLETED or CANCELLED as the previous status and ARCHIVED as the new status"
            );
        }
    }

    @PrePersist
    protected void onCreate() {
        requireCampaign(campaign);
        requireChangeType(changeType);
        requireStatus(previousStatus, "Previous campaign status is required");
        requireStatus(newStatus, "New campaign status is required");
        reason = validateReason(reason);
        actorId = validateActorId(actorId);
        requireCreatedAt(createdAt);
        validateTransition();
    }

    @PreUpdate
    protected void onUpdate() {
        throw new IllegalStateException("Campaign lifecycle events are immutable and cannot be updated");
    }

    public Long getId() {
        return id;
    }

    public VaccinationCampaign getCampaign() {
        return campaign;
    }

    public CampaignLifecycleChangeType getChangeType() {
        return changeType;
    }

    public CampaignStatus getPreviousStatus() {
        return previousStatus;
    }

    public CampaignStatus getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }

    public String getActorId() {
        return actorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
