package com.se2030.vaccinationportal.vaccination.model;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Digital certificate identity issued for one finalized vaccination record.
 */
@Entity
@Table(
        name = "digital_certificates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_digital_certificate_record",
                        columnNames = "vaccination_record_id"
                ),
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
        if (vaccinationRecordId == null) {
            throw new IllegalArgumentException("Vaccination record ID is required");
        }
        this.vaccinationRecordId = vaccinationRecordId;
        this.certificateReference = requireText(certificateReference, "Certificate reference", 64);
        this.verificationToken = requireText(verificationToken, "Verification token", 128);
        this.issuedAt = issuedAt;
        this.status = CertificateStatus.VALID;
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

    public Long getLockVersion() {
        return lockVersion;
    }
}
