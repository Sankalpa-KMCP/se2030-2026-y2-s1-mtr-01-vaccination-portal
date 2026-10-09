package com.se2030.vaccinationportal.vaccination.administration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Append-only audit entry describing one correction to a finalized vaccination record.
 *
 * <p>The original {@link VaccinationRecord} remains immutable. A correction records the
 * original value and the requested replacement without updating the medical record row.</p>
 */
@Entity
@Table(
        name = "vaccination_corrections",
        indexes = @Index(
                name = "idx_vaccination_correction_record_time",
                columnList = "vaccination_record_id, corrected_at, id"
        )
)
public class VaccinationCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Vaccination record ID is required")
    @Column(name = "vaccination_record_id", nullable = false, updatable = false)
    private Long vaccinationRecordId;

    @NotNull(message = "Correction field is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "field_name", nullable = false, length = 40)
    private VaccinationCorrectionField fieldName;

    @Size(max = 2000, message = "Old value must not exceed 2000 characters")
    @Column(name = "old_value", length = 2000)
    private String oldValue;

    @Size(max = 2000, message = "New value must not exceed 2000 characters")
    @Column(name = "new_value", length = 2000)
    private String newValue;

    @NotBlank(message = "Correction reason is required")
    @Size(max = 1000, message = "Correction reason must not exceed 1000 characters")
    @Column(name = "reason", nullable = false, updatable = false, length = 1000)
    private String reason;

    @NotNull(message = "Corrected-by ID is required")
    @Positive(message = "Corrected-by ID must be positive")
    @Column(name = "corrected_by", nullable = false, updatable = false)
    private Long correctedBy;

    @NotNull(message = "Correction timestamp is required")
    @Column(name = "corrected_at", nullable = false, updatable = false)
    private Instant correctedAt;

    @NotNull(message = "Correction status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private VaccinationCorrectionStatus status;

    @NotNull(message = "Request type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 32)
    private VaccinationCorrectionType requestType;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Size(max = 1000, message = "Review reason must not exceed 1000 characters")
    @Column(name = "review_reason", length = 1000)
    private String reviewReason;

    protected VaccinationCorrection() {
    }

    public VaccinationCorrection(
            Long vaccinationRecordId,
            VaccinationCorrectionField fieldName,
            String oldValue,
            String newValue,
            String reason,
            Long correctedBy,
            Instant correctedAt
    ) {
        this(
                vaccinationRecordId,
                fieldName,
                oldValue,
                newValue,
                reason,
                correctedBy,
                correctedAt,
                VaccinationCorrectionStatus.PENDING,
                VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE
        );
    }

    public VaccinationCorrection(
            Long vaccinationRecordId,
            VaccinationCorrectionField fieldName,
            String oldValue,
            String newValue,
            String reason,
            Long correctedBy,
            Instant correctedAt,
            VaccinationCorrectionStatus status
    ) {
        this(
                vaccinationRecordId,
                fieldName,
                oldValue,
                newValue,
                reason,
                correctedBy,
                correctedAt,
                status,
                VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE
        );
    }

    public VaccinationCorrection(
            Long vaccinationRecordId,
            VaccinationCorrectionField fieldName,
            String oldValue,
            String newValue,
            String reason,
            Long correctedBy,
            Instant correctedAt,
            VaccinationCorrectionStatus status,
            VaccinationCorrectionType requestType
    ) {
        if (vaccinationRecordId == null || vaccinationRecordId <= 0) {
            throw new IllegalArgumentException("Vaccination record ID must be positive");
        }
        if (fieldName == null) {
            throw new IllegalArgumentException("Correction field is required");
        }
        if (correctedBy == null || correctedBy <= 0) {
            throw new IllegalArgumentException("Corrected-by ID must be positive");
        }
        if (correctedAt == null) {
            throw new IllegalArgumentException("Correction timestamp is required");
        }
        this.vaccinationRecordId = vaccinationRecordId;
        this.fieldName = fieldName;
        this.oldValue = enforceLength(oldValue, "Old value", 2000);
        this.newValue = enforceLength(newValue, "New value", 2000);
        this.reason = requireText(reason, "Correction reason", 1000);
        this.correctedBy = correctedBy;
        this.correctedAt = correctedAt;
        this.status = status != null ? status : VaccinationCorrectionStatus.PENDING;
        this.requestType = requestType != null ? requestType : VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE;
    }

    public void approve(Long reviewerId, Instant reviewedAt, String reviewReason) {
        if (this.status != VaccinationCorrectionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING corrections can be approved; current status is: " + this.status);
        }
        if (reviewerId == null || reviewerId <= 0) {
            throw new IllegalArgumentException("Reviewer ID must be positive");
        }
        if (reviewedAt == null) {
            throw new IllegalArgumentException("Review timestamp is required");
        }
        this.status = VaccinationCorrectionStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
        this.reviewReason = reviewReason != null ? reviewReason.trim() : null;
    }

    public void reject(Long reviewerId, Instant reviewedAt, String rejectionReason) {
        if (this.status != VaccinationCorrectionStatus.PENDING && this.status != VaccinationCorrectionStatus.APPROVED) {
            throw new IllegalStateException("Only PENDING or APPROVED corrections can be rejected; current status is: " + this.status);
        }
        if (reviewerId == null || reviewerId <= 0) {
            throw new IllegalArgumentException("Reviewer ID must be positive");
        }
        if (reviewedAt == null) {
            throw new IllegalArgumentException("Review timestamp is required");
        }
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        this.status = VaccinationCorrectionStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
        this.reviewReason = rejectionReason.trim();
    }

    public void apply() {
        if (this.status != VaccinationCorrectionStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED corrections can be applied; current status is: " + this.status);
        }
        this.status = VaccinationCorrectionStatus.APPLIED;
    }

    public void updateCorrection(VaccinationCorrectionField fieldName, String oldValue, String newValue) {
        if (this.status != VaccinationCorrectionStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED corrections can be updated prior to application; current status is: " + this.status);
        }
        if (fieldName == null) {
            throw new IllegalArgumentException("Correction field is required");
        }
        this.fieldName = fieldName;
        this.oldValue = enforceLength(oldValue, "Old value", 2000);
        this.newValue = enforceLength(newValue, "New value", 2000);
    }

    private static String requireText(String value, String name, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return enforceLength(value.trim(), name, maxLength);
    }

    private static String enforceLength(String value, String name, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters");
        }
        return value;
    }

    public Long getId() {
        return id;
    }

    public Long getVaccinationRecordId() {
        return vaccinationRecordId;
    }

    public VaccinationCorrectionField getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getReason() {
        return reason;
    }

    public Long getCorrectedBy() {
        return correctedBy;
    }

    public Instant getCorrectedAt() {
        return correctedAt;
    }

    public VaccinationCorrectionStatus getStatus() {
        return status;
    }

    public VaccinationCorrectionType getRequestType() {
        return requestType;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewReason() {
        return reviewReason;
    }
}
