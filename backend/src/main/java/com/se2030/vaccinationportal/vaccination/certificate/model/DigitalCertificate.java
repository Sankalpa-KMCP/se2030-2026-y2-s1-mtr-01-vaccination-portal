package com.se2030.vaccinationportal.vaccination.certificate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Digital certificate identity issued for one finalized vaccination record.
 */
@Entity
@Table(
        name = "digital_certificates",
        indexes = @Index(
                name = "idx_digital_certificate_record_issued",
                columnList = "vaccination_record_id, issued_at, id"
        ),
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_digital_certificate_reference",
                        columnNames = "certificate_reference"
                ),
                @UniqueConstraint(
                        name = "uk_digital_certificate_verification_token",
                        columnNames = "verification_token"
                )
        }
)
public class DigitalCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Vaccination record ID is required")
    @Column(name = "vaccination_record_id", nullable = false, updatable = false)
    private Long vaccinationRecordId;

    @NotBlank(message = "Certificate reference is required")
    @Size(max = 64, message = "Certificate reference must not exceed 64 characters")
    @Column(name = "certificate_reference", nullable = false, updatable = false, length = 64)
    private String certificateReference;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @NotNull(message = "Certificate status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CertificateStatus status = CertificateStatus.VALID;

    @NotBlank(message = "Verification token is required")
    @Size(max = 128, message = "Verification token must not exceed 128 characters")
    @Column(name = "verification_token", nullable = false, updatable = false, length = 128)
    private String verificationToken;

    /** Batch identity captured at issue time; vaccine-name resolution awaits MF-04. */
    @Positive(message = "Issued vaccine batch ID must be positive")
    @Column(name = "issued_vaccine_batch_id", updatable = false)
    private Long issuedVaccineBatchId;

    @Positive(message = "Issued dose number must be positive")
    @Column(name = "issued_dose_number", updatable = false)
    private Integer issuedDoseNumber;

    @Column(name = "issued_administration_time", updatable = false)
    private Instant issuedAdministrationTime;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Positive(message = "Revoked-by ID must be positive")
    @Column(name = "revoked_by")
    private Long revokedBy;

    @Size(max = 1000, message = "Revocation reason must not exceed 1000 characters")
    @Column(name = "revocation_reason", length = 1000)
    private String revocationReason;

    @Positive(message = "Reissued-from certificate ID must be positive")
    @Column(name = "reissued_from_certificate_id", updatable = false)
    private Long reissuedFromCertificateId;

    @Positive(message = "Superseded-by certificate ID must be positive")
    @Column(name = "superseded_by_certificate_id")
    private Long supersededByCertificateId;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Positive(message = "Superseded-by actor ID must be positive")
    @Column(name = "superseded_by")
    private Long supersededBy;

    @Size(max = 1000, message = "Supersession reason must not exceed 1000 characters")
    @Column(name = "supersession_reason", length = 1000)
    private String supersessionReason;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    protected DigitalCertificate() {
    }

    public DigitalCertificate(
            Long vaccinationRecordId,
            String certificateReference,
            String verificationToken,
            Instant issuedAt
    ) {
        this(vaccinationRecordId, certificateReference, verificationToken, issuedAt, null, null, null, null);
    }

    public DigitalCertificate(
            Long vaccinationRecordId,
            String certificateReference,
            String verificationToken,
            Instant issuedAt,
            Long reissuedFromCertificateId
    ) {
        this(
                vaccinationRecordId,
                certificateReference,
                verificationToken,
                issuedAt,
                reissuedFromCertificateId,
                null,
                null,
                null
        );
    }

    public DigitalCertificate(
            Long vaccinationRecordId,
            String certificateReference,
            String verificationToken,
            Instant issuedAt,
            Long reissuedFromCertificateId,
            Long issuedVaccineBatchId,
            Integer issuedDoseNumber,
            Instant issuedAdministrationTime
    ) {
        if (vaccinationRecordId == null) {
            throw new IllegalArgumentException("Vaccination record ID is required");
        }
        this.vaccinationRecordId = vaccinationRecordId;
        this.certificateReference = requireText(certificateReference, "Certificate reference", 64);
        this.verificationToken = requireText(verificationToken, "Verification token", 128);
        this.issuedAt = issuedAt;
        this.status = CertificateStatus.VALID;
        if (reissuedFromCertificateId != null && reissuedFromCertificateId <= 0) {
            throw new IllegalArgumentException("Reissued-from certificate ID must be positive");
        }
        this.reissuedFromCertificateId = reissuedFromCertificateId;
        if (issuedVaccineBatchId != null && issuedVaccineBatchId <= 0) {
            throw new IllegalArgumentException("Issued vaccine batch ID must be positive");
        }
        if (issuedDoseNumber != null && issuedDoseNumber <= 0) {
            throw new IllegalArgumentException("Issued dose number must be positive");
        }
        this.issuedVaccineBatchId = issuedVaccineBatchId;
        this.issuedDoseNumber = issuedDoseNumber;
        this.issuedAdministrationTime = issuedAdministrationTime;
    }

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
        if (status == null) {
            status = CertificateStatus.VALID;
        }
    }

    private static String requireText(String value, String name, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    public void revoke(Instant revokedAt, Long revokedBy, String revocationReason) {
        if (status != CertificateStatus.VALID) {
            throw new IllegalStateException("Only a valid certificate can be revoked");
        }
        if (revokedAt == null) {
            throw new IllegalArgumentException("Revocation timestamp is required");
        }
        if (revokedBy == null || revokedBy <= 0) {
            throw new IllegalArgumentException("Revoked-by ID must be positive");
        }
        this.status = CertificateStatus.REVOKED;
        this.revokedAt = revokedAt;
        this.revokedBy = revokedBy;
        this.revocationReason = requireText(revocationReason, "Revocation reason", 1000);
    }

    public void supersede(Instant supersededAt, Long supersededBy, String reason) {
        if (status != CertificateStatus.VALID) {
            throw new IllegalStateException("Only a valid certificate can be superseded");
        }
        if (supersededAt == null) {
            throw new IllegalArgumentException("Supersession timestamp is required");
        }
        if (supersededBy == null || supersededBy <= 0) {
            throw new IllegalArgumentException("Superseded-by actor ID must be positive");
        }
        this.status = CertificateStatus.SUPERSEDED;
        this.supersededAt = supersededAt;
        this.supersededBy = supersededBy;
        this.supersessionReason = requireText(reason, "Supersession reason", 1000);
    }

    public void markSupersededBy(Long certificateId) {
        if (status != CertificateStatus.SUPERSEDED) {
            throw new IllegalStateException("Only a superseded certificate can link its replacement");
        }
        if (certificateId == null || certificateId <= 0) {
            throw new IllegalArgumentException("Superseding certificate ID must be positive");
        }
        if (supersededByCertificateId != null) {
            throw new IllegalStateException("Certificate has already been superseded");
        }
        if (id != null && id.equals(certificateId)) {
            throw new IllegalArgumentException("Certificate cannot supersede itself");
        }
        supersededByCertificateId = certificateId;
    }

    public Long getId() {
        return id;
    }

    public Long getVaccinationRecordId() {
        return vaccinationRecordId;
    }

    public String getCertificateReference() {
        return certificateReference;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public CertificateStatus getStatus() {
        return status;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public Long getIssuedVaccineBatchId() {
        return issuedVaccineBatchId;
    }

    public Integer getIssuedDoseNumber() {
        return issuedDoseNumber;
    }

    public Instant getIssuedAdministrationTime() {
        return issuedAdministrationTime;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Long getRevokedBy() {
        return revokedBy;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public Long getReissuedFromCertificateId() {
        return reissuedFromCertificateId;
    }

    public Long getSupersededByCertificateId() {
        return supersededByCertificateId;
    }

    public Instant getSupersededAt() {
        return supersededAt;
    }

    public Long getSupersededBy() {
        return supersededBy;
    }

    public String getSupersessionReason() {
        return supersessionReason;
    }

    public Long getLockVersion() {
        return lockVersion;
    }
}
