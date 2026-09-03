package com.se2030.vaccinationportal.screening.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "eligibility_assessment",
    indexes = {
        @Index(name = "idx_assess_citizen_campaign", columnList = "citizen_id, campaign_id, status")
    }
)
public class EligibilityAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_eligibility_assessment_sub"))
    private ScreeningSubmission submission;

    @Column(name = "citizen_id", nullable = false)
    private Long citizenId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EligibilityAssessmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "preliminary_result", nullable = false, length = 64)
    private PreliminaryScreeningResult preliminaryResult;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    @Column(name = "rescreening_authorized_at")
    private Instant rescreeningAuthorizedAt;

    @Column(name = "rescreening_authorized_by")
    private Long rescreeningAuthorizedBy;

    @Column(name = "rescreening_reason", columnDefinition = "TEXT")
    private String rescreeningReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.reviewedAt == null) {
            this.reviewedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public EligibilityAssessment() {}

    public EligibilityAssessment(ScreeningSubmission submission, Long citizenId, Long campaignId, Long reviewerId,
                                 EligibilityAssessmentStatus status, PreliminaryScreeningResult preliminaryResult,
                                 String reason, Instant reviewedAt, Instant validUntil) {
        this.submission = submission;
        this.citizenId = citizenId;
        this.campaignId = campaignId;
        this.reviewerId = reviewerId;
        this.status = status;
        this.preliminaryResult = preliminaryResult;
        this.reason = reason;
        this.reviewedAt = reviewedAt;
        this.validUntil = validUntil;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ScreeningSubmission getSubmission() { return submission; }
    public void setSubmission(ScreeningSubmission submission) { this.submission = submission; }

    public Long getCitizenId() { return citizenId; }
    public void setCitizenId(Long citizenId) { this.citizenId = citizenId; }

    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

    public EligibilityAssessmentStatus getStatus() { return status; }
    public void setStatus(EligibilityAssessmentStatus status) { this.status = status; }

    public PreliminaryScreeningResult getPreliminaryResult() { return preliminaryResult; }
    public void setPreliminaryResult(PreliminaryScreeningResult preliminaryResult) { this.preliminaryResult = preliminaryResult; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { this.revocationReason = revocationReason; }

    public Instant getRescreeningAuthorizedAt() { return rescreeningAuthorizedAt; }
    public void setRescreeningAuthorizedAt(Instant rescreeningAuthorizedAt) { this.rescreeningAuthorizedAt = rescreeningAuthorizedAt; }

    public Long getRescreeningAuthorizedBy() { return rescreeningAuthorizedBy; }
    public void setRescreeningAuthorizedBy(Long rescreeningAuthorizedBy) { this.rescreeningAuthorizedBy = rescreeningAuthorizedBy; }

    public String getRescreeningReason() { return rescreeningReason; }
    public void setRescreeningReason(String rescreeningReason) { this.rescreeningReason = rescreeningReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
