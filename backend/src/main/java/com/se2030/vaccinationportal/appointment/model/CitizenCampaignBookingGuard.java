package com.se2030.vaccinationportal.appointment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Shared booking guard entity (one row per citizen and campaign).
 * Used with SELECT ... FOR UPDATE to serialize appointment and waitlist cross-table invariant checks.
 * Canonical section 12.2 and section 12.4.
 */
@Entity
@Table(
    name = "citizen_campaign_booking_guard",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_booking_guard_citizen_campaign",
        columnNames = {"citizen_id", "campaign_id"}
    )
)
public class CitizenCampaignBookingGuard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Citizen ID is required")
    @Column(name = "citizen_id", nullable = false)
    private Long citizenId;

    @NotNull(message = "Campaign ID is required")
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CitizenCampaignBookingGuard() {
    }

    public CitizenCampaignBookingGuard(Long citizenId, Long campaignId) {
        this.citizenId = citizenId;
        this.campaignId = campaignId;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
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

    public Long getLockVersion() {
        return lockVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "CitizenCampaignBookingGuard{" +
                "id=" + id +
                ", citizenId=" + citizenId +
                ", campaignId=" + campaignId +
                ", lockVersion=" + lockVersion +
                ", createdAt=" + createdAt +
                '}';
    }
}
