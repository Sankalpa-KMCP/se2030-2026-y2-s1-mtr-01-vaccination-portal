package com.se2030.vaccinationportal.appointment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Core JPA entity representing a citizen waitlist entry for a vaccination session.
 * Enforces field list per canonical section 12.x and database conditional uniqueness on active WAITING status per campaign.
 * Status mutation is restricted to package-private access; dedicated services handle state transitions.
 */
@Entity
@Table(
    name = "waitlist_entry",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_waitlist_active_citizen_campaign",
        columnNames = {"citizen_id", "active_campaign_id"}
    )
)
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Citizen ID is required")
    @Column(name = "citizen_id", nullable = false)
    private Long citizenId;

    @NotNull(message = "Campaign ID is required")
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @NotNull(message = "Session ID is required")
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @NotNull(message = "Waitlist status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @NotNull(message = "Joined timestamp is required")
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    /**
     * Database generated active campaign ID column.
     * Evaluates to campaign_id while status = 'WAITING', and NULL otherwise.
     * Canonical Section 12.x conditional-uniqueness detail. Read-only in JPA.
     */
    @Column(
        name = "active_campaign_id",
        insertable = false,
        updatable = false,
        columnDefinition = "BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'WAITING' THEN campaign_id ELSE NULL END)"
    )
    private Long activeCampaignId;

    public WaitlistEntry() {
    }

    public WaitlistEntry(Long citizenId, Long campaignId, Long sessionId, WaitlistStatus status, Instant joinedAt) {
        this.citizenId = citizenId;
        this.campaignId = campaignId;
        this.sessionId = sessionId;
        if (status != null) {
            this.status = status;
        }
        if (joinedAt != null) {
            this.joinedAt = joinedAt;
        }
    }

    public WaitlistEntry(Long citizenId, Long campaignId, Long sessionId) {
        this(citizenId, campaignId, sessionId, WaitlistStatus.WAITING, Instant.now());
    }

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
        if (status == null) {
            status = WaitlistStatus.WAITING;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCitizenId() {
        return citizenId;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Long getLockVersion() {
        return lockVersion;
    }

    public Long getActiveCampaignId() {
        return activeCampaignId;
    }

    /**
     * Package-private status mutation for internal domain/service use only.
     * Free-form public setStatus() is intentionally omitted per canonical guidelines.
     */
    void setStatus(WaitlistStatus status) {
        this.status = status;
    }

    /**
     * Package-private status transition method.
     *
     * @param targetStatus Target {@link WaitlistStatus}
     */
    void transitionStatus(WaitlistStatus targetStatus) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target waitlist status cannot be null");
        }
        this.status = targetStatus;
    }

    /**
     * Marks this waitlist entry as PROMOTED (MF03-BR-08).
     */
    public void markPromoted() {
        this.status = WaitlistStatus.PROMOTED;
    }

    /**
     * Marks this waitlist entry as SKIPPED_INELIGIBLE (MF03-BR-07).
     */
    public void markSkippedIneligible() {
        this.status = WaitlistStatus.SKIPPED_INELIGIBLE;
    }

    /**
     * Marks this waitlist entry as SKIPPED_DUPLICATE_APPOINTMENT (MF03-BR-07).
     */
    public void markSkippedDuplicate() {
        this.status = WaitlistStatus.SKIPPED_DUPLICATE_APPOINTMENT;
    }

    /**
     * Cancels this waitlist entry.
     */
    public void cancel() {
        this.status = WaitlistStatus.CANCELLED;
    }

    @Override
    public String toString() {
        return "WaitlistEntry{" +
                "id=" + id +
                ", citizenId=" + citizenId +
                ", campaignId=" + campaignId +
                ", sessionId=" + sessionId +
                ", status=" + status +
                ", joinedAt=" + joinedAt +
                ", lockVersion=" + lockVersion +
                ", activeCampaignId=" + activeCampaignId +
                '}';
    }
}
