package com.se2030.vaccinationportal.vaccination.administration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Editable, unfinalized vaccination administration data for one appointment.
 *
 * <p>Citizen, campaign, and center details remain derivable through MF-03's appointment and
 * slot references, so this draft stores only the identifiers that are specific to MF-05.</p>
 */
@Entity
@Table(
        name = "vaccination_drafts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vaccination_draft_appointment",
                columnNames = "appointment_id"
        ),
        indexes = {
                @Index(name = "idx_vaccination_draft_status", columnList = "status"),
                @Index(name = "idx_vaccination_draft_batch", columnList = "vaccine_batch_id")
        }
)
public class VaccinationDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Appointment ID is required")
    @Column(name = "appointment_id", nullable = false, updatable = false)
    private Long appointmentId;

    @Column(name = "vaccinator_id")
    private Long vaccinatorId;

    @Column(name = "vaccine_batch_id")
    private Long vaccineBatchId;

    @Positive(message = "Dose number must be positive")
    @Column(name = "dose_number")
    private Integer doseNumber;

    @Column(name = "administration_time")
    private Instant administrationTime;

    @Column(name = "next_dose_date")
    private LocalDate nextDoseDate;

    @Size(max = 2000, message = "Authorized notes must not exceed 2000 characters")
    @Column(name = "authorized_notes", length = 2000)
    private String authorizedNotes;

    @Column(name = "physical_administration_confirmed", nullable = false)
    private boolean physicalAdministrationConfirmed;

    @NotNull(message = "Draft status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private VaccinationDraftStatus status = VaccinationDraftStatus.IN_PROGRESS;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VaccinationDraft() {
    }

    public VaccinationDraft(Long appointmentId) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID is required");
        }
        this.appointmentId = appointmentId;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (status == null) {
            status = VaccinationDraftStatus.IN_PROGRESS;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Long getVaccinatorId() {
        return vaccinatorId;
    }

    public void setVaccinatorId(Long vaccinatorId) {
        this.vaccinatorId = vaccinatorId;
    }

    public Long getVaccineBatchId() {
        return vaccineBatchId;
    }

    public void setVaccineBatchId(Long vaccineBatchId) {
        this.vaccineBatchId = vaccineBatchId;
    }

    public Integer getDoseNumber() {
        return doseNumber;
    }

    public void setDoseNumber(Integer doseNumber) {
        if (doseNumber != null && doseNumber <= 0) {
            throw new IllegalArgumentException("Dose number must be positive");
        }
        this.doseNumber = doseNumber;
    }

    public Instant getAdministrationTime() {
        return administrationTime;
    }

    public void setAdministrationTime(Instant administrationTime) {
        this.administrationTime = administrationTime;
    }

    public LocalDate getNextDoseDate() {
        return nextDoseDate;
    }

    public void setNextDoseDate(LocalDate nextDoseDate) {
        this.nextDoseDate = nextDoseDate;
    }

    public String getAuthorizedNotes() {
        return authorizedNotes;
    }

    public void setAuthorizedNotes(String authorizedNotes) {
        if (authorizedNotes != null && authorizedNotes.length() > 2000) {
            throw new IllegalArgumentException("Authorized notes must not exceed 2000 characters");
        }
        this.authorizedNotes = authorizedNotes;
    }

    public boolean isPhysicalAdministrationConfirmed() {
        return physicalAdministrationConfirmed;
    }

    public void setPhysicalAdministrationConfirmed(boolean physicalAdministrationConfirmed) {
        this.physicalAdministrationConfirmed = physicalAdministrationConfirmed;
    }

    public VaccinationDraftStatus getStatus() {
        return status;
    }

    public void setStatus(VaccinationDraftStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Draft status is required");
        }
        this.status = status;
    }

    public Long getLockVersion() {
        return lockVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
