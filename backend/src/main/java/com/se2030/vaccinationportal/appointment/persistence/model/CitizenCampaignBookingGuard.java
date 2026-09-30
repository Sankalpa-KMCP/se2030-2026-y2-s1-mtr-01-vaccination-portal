package com.se2030.vaccinationportal.appointment.persistence.model;

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
 * Shared booking guard entity (one row per citizen and campaign) providing an explicit database mutex.
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Search, book, and reschedule appointments without race conditions.</li>
 *   <li>US-05: Join waitlists and execute automated promotions without concurrent double-booking.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-02: Exactly one active appointment per citizen per campaign.</li>
 *   <li>MF03-BR-14: Citizen cannot hold an active waitlist entry while holding an active appointment.</li>
 * </ul>
 *
 * <p><b>Viva / Concurrency Architecture ("Why this class exists"):</b><br>
 * Cross-table invariants exist between {@code appointment} and {@code waitlist_entry} for a given
 * {@code (citizenId, campaignId)} tuple (e.g. a citizen must not book an appointment while being concurrently
 * promoted from a waitlist, nor join a waitlist while holding an active booking). In relational databases,
 * locking rows that do not yet exist or locking across separate tables without a common row lock creates
 * race windows. By performing {@code SELECT ... FOR UPDATE} on this guard row at the very start of booking,
 * waitlist joining, rescheduling, and promotion transactions, all mutating operations for the same citizen
 * within the campaign are serialized deterministically, eliminating cross-table race conditions.
 * </p>
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
