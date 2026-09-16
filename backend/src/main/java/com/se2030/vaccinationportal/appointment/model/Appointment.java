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
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Search, book, cancel, and reschedule appointment slots.</li>
 *   <li>US-26: Checked-in appointment queue for clinic arrival and vaccinator intake.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced:</b>
 * <ul>
 *   <li>MF03-BR-02: Exactly one active appointment (CONFIRMED or CHECKED_IN) per citizen per campaign,
 *       enforced at the database layer via generated column {@code active_campaign_id} and the unique
 *       constraint {@code uk_appointment_active_citizen_campaign}.</li>
 *   <li>MF03-BR-05: In-place slot reallocation during atomic rescheduling, preserving appointment ID and history.</li>
 *   <li>MF03-BR-10: Check-in is permitted only for CONFIRMED appointments.</li>
 *   <li>MF03-BR-11: COMPLETED appointments are immutable and cannot be cancelled, rescheduled, or checked in again.</li>
 *   <li>MF03-BR-15: Check-in reversal transitions CHECKED_IN back to CONFIRMED prior to finalization.</li>
 * </ul>
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
     * <p>
     * <b>Viva / Concurrency Note:</b> Setting status to {@link AppointmentStatus#CANCELLED} causes
     * the generated column {@code active_campaign_id} to evaluate to {@code NULL}. This releases the
     * database unique constraint ({@code uk_appointment_active_citizen_campaign}) so the citizen can
     * book another appointment in the same campaign if desired (MF03-BR-02).
     * </p>
     *
     * @param cancelledAt Timestamp of cancellation
     */
    public void cancel(Instant cancelledAt) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancelledAt = (cancelledAt != null) ? cancelledAt : Instant.now();
    }

    /**
     * Completes this appointment once the vaccination draft has been finalized (MF-05 handover).
     * <p>
     * <b>Viva / Invariant Note:</b> Transitioning to {@link AppointmentStatus#COMPLETED} makes the
     * record immutable per MF03-BR-11. It can never be cancelled, rescheduled, or checked in again,
     * preserving legal audit trails and clinical records.
     * </p>
     */
    public void complete() {
        this.status = AppointmentStatus.COMPLETED;
    }

    /**
     * Reschedules this appointment to a new time slot (MF03-BR-05).
     * <p>
     * <b>Viva / Concurrency Note:</b> Updates the slot pointer in place rather than deleting and re-creating
     * the entity. This preserves the original appointment ID, booking history, and citizen audit trail.
     * The service guarantees that new slot capacity has been reserved before this mutation occurs.
     * </p>
     *
     * @param newSlotId Identifier of the target time slot
     */
    public void reschedule(Long newSlotId) {
        if (newSlotId == null) {
            throw new IllegalArgumentException("New slot ID is required for rescheduling");
        }
        this.slotId = newSlotId;
    }

    /**
     * Checks in this appointment upon citizen arrival at the clinic (MF03-BR-09, MF03-BR-10).
     * <p>
     * <b>Viva / Queue Note:</b> Transitions status from CONFIRMED to CHECKED_IN. This places the
     * appointment into the vaccinator work queue (US-26), making it visible to clinical staff
     * for vaccination draft creation in MF-05.
     * </p>
     */
    public void checkIn() {
        this.status = AppointmentStatus.CHECKED_IN;
    }

    /**
     * Reverts check-in status back to CONFIRMED (MF03-BR-15).
     * <p>
     * <b>Viva / Clinical Note:</b> Allows reception staff to undo an accidental check-in (e.g. citizen
     * walked out before vaccination). Permitted only if no active vaccination draft exists in MF-05.
     * </p>
     */
    public void reverseCheckIn() {
        this.status = AppointmentStatus.CONFIRMED;
    }

    /**
     * Marks this appointment as NO_SHOW when the citizen failed to arrive within the allowable window.
     * <p>
     * <b>Viva Note:</b> Terminal state indicating unattended appointment. Releases the citizen's hold
     * so waitlist promotion or capacity reallocation can occur.
     * </p>
     */
    public void markNoShow() {
        this.status = AppointmentStatus.NO_SHOW;
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
