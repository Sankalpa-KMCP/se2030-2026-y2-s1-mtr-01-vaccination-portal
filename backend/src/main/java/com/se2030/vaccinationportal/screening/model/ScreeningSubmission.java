package com.se2030.vaccinationportal.screening.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "screening_submission",
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
