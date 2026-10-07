package com.se2030.vaccinationportal.screening.submission.model;

import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.questionnaire.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;


import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "screening_submission",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_screening_submission_active_citizen_campaign",
            columnNames = {"citizen_id", "active_campaign_id"}
        )
    },
    indexes = {
        @Index(name = "idx_sub_citizen_campaign", columnList = "citizen_id, campaign_id"),
        @Index(name = "idx_sub_status", columnList = "status")
    }
)
public class ScreeningSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "citizen_id", nullable = false)
    private Long citizenId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false, foreignKey = @ForeignKey(name = "fk_screening_sub_version"))
    private QuestionnaireVersion questionnaireVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consent_notice_version_id", nullable = false, foreignKey = @ForeignKey(name = "fk_screening_sub_consent"))
    private ConsentNoticeVersion consentNoticeVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ScreeningSubmissionStatus status = ScreeningSubmissionStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "preliminary_result", length = 64)
    private PreliminaryScreeningResult preliminaryResult;

    @Column(name = "consent_at", nullable = false)
    private Instant consentAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "correction_instructions", columnDefinition = "TEXT")
    private String correctionInstructions;

    @Column(name = "correction_requested_at")
    private Instant correctionRequestedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ScreeningAnswer> answers = new ArrayList<>();

    @OneToOne(mappedBy = "submission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EligibilityAssessment assessment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.consentAt == null) {
            this.consentAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public ScreeningSubmission() {}

    public ScreeningSubmission(Long citizenId, Long campaignId, QuestionnaireVersion questionnaireVersion, ConsentNoticeVersion consentNoticeVersion, Instant consentAt) {
        this.citizenId = citizenId;
        this.campaignId = campaignId;
        this.questionnaireVersion = questionnaireVersion;
        this.consentNoticeVersion = consentNoticeVersion;
        this.consentAt = consentAt;
    }

    public void addAnswer(ScreeningAnswer answer) {
        answers.add(answer);
        answer.setSubmission(this);
    }

    public void removeAnswer(ScreeningAnswer answer) {
        answers.remove(answer);
        answer.setSubmission(null);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCitizenId() { return citizenId; }
    public void setCitizenId(Long citizenId) { this.citizenId = citizenId; }

    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }

    public QuestionnaireVersion getQuestionnaireVersion() { return questionnaireVersion; }
    public void setQuestionnaireVersion(QuestionnaireVersion questionnaireVersion) { this.questionnaireVersion = questionnaireVersion; }

    public ConsentNoticeVersion getConsentNoticeVersion() { return consentNoticeVersion; }
    public void setConsentNoticeVersion(ConsentNoticeVersion consentNoticeVersion) { this.consentNoticeVersion = consentNoticeVersion; }

    public ScreeningSubmissionStatus getStatus() { return status; }
    public void setStatus(ScreeningSubmissionStatus status) { this.status = status; }

    public PreliminaryScreeningResult getPreliminaryResult() { return preliminaryResult; }
    public void setPreliminaryResult(PreliminaryScreeningResult preliminaryResult) { this.preliminaryResult = preliminaryResult; }

    public Instant getConsentAt() { return consentAt; }
    public void setConsentAt(Instant consentAt) { this.consentAt = consentAt; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    /**
     * Database generated active campaign ID column (R05).
     * Evaluates to campaign_id while status IN ('DRAFT','PENDING_REVIEW','CORRECTIONS_REQUIRED'),
     * and NULL otherwise, so the {@code uk_screening_submission_active_citizen_campaign} unique
     * constraint only applies to active submissions. Read-only in JPA.
     */
    @Column(
        name = "active_campaign_id",
        insertable = false,
        updatable = false,
        columnDefinition = "BIGINT GENERATED ALWAYS AS (CASE WHEN status IN ('DRAFT','PENDING_REVIEW','CORRECTIONS_REQUIRED') THEN campaign_id ELSE NULL END)"
    )
    private Long activeCampaignId;

    public Long getActiveCampaignId() { return activeCampaignId; }

    public String getCorrectionInstructions() { return correctionInstructions; }
    public void setCorrectionInstructions(String correctionInstructions) { this.correctionInstructions = correctionInstructions; }

    public Instant getCorrectionRequestedAt() { return correctionRequestedAt; }
    public void setCorrectionRequestedAt(Instant correctionRequestedAt) { this.correctionRequestedAt = correctionRequestedAt; }

    public List<ScreeningAnswer> getAnswers() { return answers; }
    public void setAnswers(List<ScreeningAnswer> answers) { this.answers = answers; }

    public EligibilityAssessment getAssessment() { return assessment; }
    public void setAssessment(EligibilityAssessment assessment) {
        this.assessment = assessment;
        if (assessment != null) {
            assessment.setSubmission(this);
        }
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
