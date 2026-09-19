package com.se2030.vaccinationportal.vaccination.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Append-only audit record for an unfinalized vaccination draft hard-deletion.
 */
@Entity
@Table(
        name = "vaccination_draft_deletion_audits",
        indexes = {
                @Index(name = "idx_vaccination_draft_deletion_draft", columnList = "draft_id"),
                @Index(name = "idx_vaccination_draft_deletion_appointment", columnList = "appointment_id"),
                @Index(name = "idx_vaccination_draft_deletion_time", columnList = "deleted_at")
        }
)
public class VaccinationDraftDeletionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Draft ID is required")
    @Column(name = "draft_id", nullable = false, updatable = false)
    private Long draftId;

    @NotNull(message = "Appointment ID is required")
    @Column(name = "appointment_id", nullable = false, updatable = false)
    private Long appointmentId;

    @NotNull(message = "Vaccinator ID is required")
    @Column(name = "vaccinator_id", nullable = false, updatable = false)
    private Long vaccinatorId;

    @NotNull(message = "Deleted-by user ID is required")
    @Positive(message = "Deleted-by user ID must be positive")
    @Column(name = "deleted_by", nullable = false, updatable = false)
    private Long deletedBy;

    @NotBlank(message = "Deletion reason is required")
    @Size(max = 1000, message = "Deletion reason must not exceed 1000 characters")
    @Column(name = "reason", nullable = false, updatable = false, length = 1000)
    private String reason;

    @NotNull(message = "Deletion timestamp is required")
    @Column(name = "deleted_at", nullable = false, updatable = false)
    private Instant deletedAt;

    protected VaccinationDraftDeletionAudit() {
    }

    public VaccinationDraftDeletionAudit(
            Long draftId,
            Long appointmentId,
            Long vaccinatorId,
            Long deletedBy,
            String reason,
            Instant deletedAt
    ) {
        if (draftId == null || draftId <= 0) {
            throw new IllegalArgumentException("Draft ID must be positive");
        }
        if (appointmentId == null || appointmentId <= 0) {
            throw new IllegalArgumentException("Appointment ID must be positive");
        }
        if (vaccinatorId == null || vaccinatorId <= 0) {
            throw new IllegalArgumentException("Vaccinator ID must be positive");
        }
        if (deletedBy == null || deletedBy <= 0) {
            throw new IllegalArgumentException("Deleted-by ID must be positive");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Deletion reason is required");
        }
        if (reason.trim().length() > 1000) {
            throw new IllegalArgumentException("Deletion reason must not exceed 1000 characters");
        }
        if (deletedAt == null) {
            throw new IllegalArgumentException("Deletion timestamp is required");
        }
        this.draftId = draftId;
        this.appointmentId = appointmentId;
        this.vaccinatorId = vaccinatorId;
        this.deletedBy = deletedBy;
        this.reason = reason.trim();
        this.deletedAt = deletedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getDraftId() {
        return draftId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Long getVaccinatorId() {
        return vaccinatorId;
    }

    public Long getDeletedBy() {
        return deletedBy;
    }

    public String getReason() {
        return reason;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
