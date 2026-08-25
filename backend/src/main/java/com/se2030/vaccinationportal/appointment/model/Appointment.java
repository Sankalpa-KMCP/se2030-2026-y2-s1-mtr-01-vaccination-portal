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
 * Core JPA entity representing a citizen appointment for a vaccination session slot.
 * Enforces field list per canonical section 12.2 and baseline conditional uniqueness per 12.4.
 * Status mutation is restricted to package-private access; dedicated services handle state transitions.
 */
@Entity
@Table(
    name = "appointment",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_appointment_active_citizen_campaign",
        columnNames = {"citizen_id", "active_campaign_id"}
    )
)
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Citizen ID is required")
    @Column(name = "citizen_id", nullable = false)
    private Long citizenId;

    @NotNull(message = "Campaign ID is required")
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @NotNull(message = "Slot ID is required")
    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @NotNull(message = "Appointment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @NotNull(message = "Booked timestamp is required")
    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    /**
     * Database generated active campaign ID column.
     * Evaluates to campaign_id while status IN ('CONFIRMED', 'CHECKED_IN'), and NULL otherwise.
     * Canonical Section 12.4 conditional-uniqueness detail. Read-only in JPA.
     */
    @Column(
        name = "active_campaign_id",
        insertable = false,
        updatable = false,
        columnDefinition = "BIGINT GENERATED ALWAYS AS (CASE WHEN status IN ('CONFIRMED', 'CHECKED_IN') THEN campaign_id ELSE NULL END)"
    )
    private Long activeCampaignId;

    public Appointment() {
    }

    public Appointment(Long citizenId, Long campaignId, Long slotId, AppointmentStatus status, Instant bookedAt) {
        this.citizenId = citizenId;
        this.campaignId = campaignId;
        this.slotId = slotId;
        if (status != null) {
            this.status = status;
        }
        if (bookedAt != null) {
            this.bookedAt = bookedAt;
        }
    }

    public Appointment(Long citizenId, Long campaignId, Long slotId) {
        this(citizenId, campaignId, slotId, AppointmentStatus.CONFIRMED, Instant.now());
    }

    @PrePersist
    protected void onCreate() {
        if (bookedAt == null) {
            bookedAt = Instant.now();
        }
        if (status == null) {
            status = AppointmentStatus.CONFIRMED;
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

    public Long getSlotId() {
        return slotId;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public Instant getBookedAt() {
        return bookedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
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
    void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    /**
     * Package-private cancellation timestamp mutation.
     */
    void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    /**
     * Cancels this appointment and sets the cancellation timestamp.
     *
     * @param cancelledAt Timestamp of cancellation
     */
    public void cancel(Instant cancelledAt) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancelledAt = (cancelledAt != null) ? cancelledAt : Instant.now();
    }

    /**
     * Completes this appointment.
     */
    public void complete() {
        this.status = AppointmentStatus.COMPLETED;
    }

    /**
     * Reschedules this appointment to a new time slot (MF03-BR-06).
     *
     * @param newSlotId Identifier of the target time slot
     */
    public void reschedule(Long newSlotId) {
        if (newSlotId == null) {
            throw new IllegalArgumentException("New slot ID is required for rescheduling");
        }
        this.slotId = newSlotId;
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "id=" + id +
                ", citizenId=" + citizenId +
                ", campaignId=" + campaignId +
                ", slotId=" + slotId +
                ", status=" + status +
                ", bookedAt=" + bookedAt +
                ", cancelledAt=" + cancelledAt +
                ", lockVersion=" + lockVersion +
                ", activeCampaignId=" + activeCampaignId +
                '}';
    }
}
