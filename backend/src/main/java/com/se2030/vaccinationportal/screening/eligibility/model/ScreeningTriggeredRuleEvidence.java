package com.se2030.vaccinationportal.screening.eligibility.model;

import com.se2030.vaccinationportal.screening.questionnaire.model.EligibilityRule;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "screening_triggered_rule_evidence",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_triggered_rule_evidence_submission_rule",
                        columnNames = {"submission_id", "source_rule_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_triggered_rule_evidence_submission_order",
                        columnList = "submission_id, evaluation_order"
                )
        }
)
public class ScreeningTriggeredRuleEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "submission_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_triggered_rule_evidence_submission")
    )
    private ScreeningSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "source_rule_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_triggered_rule_evidence_rule")
    )
    private EligibilityRule sourceRule;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "preliminary_result", nullable = false, length = 64)
    private PreliminaryScreeningResult preliminaryResult;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "evaluation_order", nullable = false)
    private int evaluationOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ScreeningTriggeredRuleEvidence() {
    }

    public ScreeningTriggeredRuleEvidence(
            ScreeningSubmission submission,
            EligibilityRule sourceRule,
            String ruleCode,
            String message,
            PreliminaryScreeningResult preliminaryResult,
            int priority,
            int evaluationOrder
    ) {
        this.submission = submission;
        this.sourceRule = sourceRule;
        this.ruleCode = ruleCode;
        this.message = message;
        this.preliminaryResult = preliminaryResult;
        this.priority = priority;
        this.evaluationOrder = evaluationOrder;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ScreeningSubmission getSubmission() { return submission; }
    public void setSubmission(ScreeningSubmission submission) { this.submission = submission; }

    public EligibilityRule getSourceRule() { return sourceRule; }
    public void setSourceRule(EligibilityRule sourceRule) { this.sourceRule = sourceRule; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public PreliminaryScreeningResult getPreliminaryResult() { return preliminaryResult; }
    public void setPreliminaryResult(PreliminaryScreeningResult preliminaryResult) {
        this.preliminaryResult = preliminaryResult;
    }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getEvaluationOrder() { return evaluationOrder; }
    public void setEvaluationOrder(int evaluationOrder) { this.evaluationOrder = evaluationOrder; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
