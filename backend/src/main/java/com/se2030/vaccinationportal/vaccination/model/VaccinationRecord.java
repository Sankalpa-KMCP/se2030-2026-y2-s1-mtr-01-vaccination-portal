package com.se2030.vaccinationportal.vaccination.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Immutable finalized vaccination history created from a vaccination draft.
 *
 * <p>Cross-module references are stored as identifiers and historical snapshots. This avoids
 * direct JPA coupling while preserving the provenance of a finalized administration event.</p>
 */
@Entity
@Table(
        name = "vaccination_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vaccination_record_appointment",
                columnNames = "appointment_id"
        ),
        indexes = {
                @Index(
                        name = "idx_vaccination_record_citizen_administered",
                        columnList = "citizen_id, administration_time"
                ),
                @Index(name = "idx_vaccination_record_batch", columnList = "vaccine_batch_id")
        }
)
public class VaccinationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Appointment ID is required")
    @Column(name = "appointment_id", nullable = false, updatable = false)
    private Long appointmentId;

    @NotNull(message = "Citizen ID is required")
    @Column(name = "citizen_id", nullable = false, updatable = false)
    private Long citizenId;

    @NotNull(message = "Campaign ID is required")
    @Column(name = "campaign_id", nullable = false, updatable = false)
    private Long campaignId;

    @NotNull(message = "Center ID is required")
    @Column(name = "center_id", nullable = false, updatable = false)
    private Long centerId;

    @NotNull(message = "Vaccinator ID is required")
    @Column(name = "vaccinator_id", nullable = false, updatable = false)
    private Long vaccinatorId;

    @NotNull(message = "Vaccine batch ID is required")
    @Column(name = "vaccine_batch_id", nullable = false, updatable = false)
    private Long vaccineBatchId;

    @NotNull(message = "Dose number is required")
    @Positive(message = "Dose number must be positive")
    @Column(name = "dose_number", nullable = false, updatable = false)
    private Integer doseNumber;

    @NotNull(message = "Administration time is required")
    @Column(name = "administration_time", nullable = false, updatable = false)
    private Instant administrationTime;

    @Column(name = "next_dose_date", updatable = false)
    private LocalDate nextDoseDate;

    @Size(max = 2000, message = "Authorized notes must not exceed 2000 characters")
    @Column(name = "authorized_notes", length = 2000, updatable = false)
    private String authorizedNotes;

    @Column(name = "finalized_at", nullable = false, updatable = false)
    private Instant finalizedAt;

    protected VaccinationRecord() {
    }

    public VaccinationRecord(
            Long appointmentId,
            Long citizenId,
            Long campaignId,
            Long centerId,
            Long vaccinatorId,
            Long vaccineBatchId,
            Integer doseNumber,
            Instant administrationTime,
            LocalDate nextDoseDate,
            String authorizedNotes,
            Instant finalizedAt
    ) {
        this.appointmentId = requireIdentifier(appointmentId, "Appointment ID");
        this.citizenId = requireIdentifier(citizenId, "Citizen ID");
        this.campaignId = requireIdentifier(campaignId, "Campaign ID");
        this.centerId = requireIdentifier(centerId, "Center ID");
        this.vaccinatorId = requireIdentifier(vaccinatorId, "Vaccinator ID");
        this.vaccineBatchId = requireIdentifier(vaccineBatchId, "Vaccine batch ID");
        if (doseNumber == null || doseNumber <= 0) {
            throw new IllegalArgumentException("Dose number must be positive");
        }
        if (authorizedNotes != null && authorizedNotes.length() > 2000) {
            throw new IllegalArgumentException("Authorized notes must not exceed 2000 characters");
        }
        this.doseNumber = doseNumber;
        this.administrationTime = requireTimestamp(administrationTime, "Administration time");
        this.nextDoseDate = nextDoseDate;
        this.authorizedNotes = authorizedNotes;
        this.finalizedAt = finalizedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (finalizedAt == null) {
            finalizedAt = Instant.now();
        }
    }

    private static Long requireIdentifier(Long value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static Instant requireTimestamp(Instant value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    public Long getId() {
        return id;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Long getCitizenId() {
        return citizenId;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public Long getCenterId() {
        return centerId;
    }

    public Long getVaccinatorId() {
        return vaccinatorId;
    }

    public Long getVaccineBatchId() {
        return vaccineBatchId;
    }

    public Integer getDoseNumber() {
        return doseNumber;
    }

    public Instant getAdministrationTime() {
        return administrationTime;
    }

    public LocalDate getNextDoseDate() {
        return nextDoseDate;
    }

    public String getAuthorizedNotes() {
        return authorizedNotes;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }
}
