package com.se2030.vaccinationportal.screening.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "eligibility_rule",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_eligibility_rule_code", columnNames = {"version_id", "rule_code"})
    }
)
public class EligibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false, foreignKey = @ForeignKey(name = "fk_eligibility_rule_version"))
    private QuestionnaireVersion questionnaireVersion;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Column(name = "condition_expression", nullable = false, columnDefinition = "TEXT")
    private String conditionExpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "preliminary_result", nullable = false, length = 64)
    private PreliminaryScreeningResult preliminaryResult = PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED;

    @Column(name = "priority", nullable = false)
    private int priority = 100;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public EligibilityRule() {}

    public EligibilityRule(String ruleCode, QuestionnaireVersion questionnaireVersion, String ruleName, String conditionExpression, PreliminaryScreeningResult preliminaryResult, int priority, String message) {
        this.ruleCode = ruleCode;
        this.questionnaireVersion = questionnaireVersion;
        this.ruleName = ruleName;
        this.conditionExpression = conditionExpression;
        this.preliminaryResult = preliminaryResult;
        this.priority = priority;
        this.message = message;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public QuestionnaireVersion getQuestionnaireVersion() { return questionnaireVersion; }
    public void setQuestionnaireVersion(QuestionnaireVersion questionnaireVersion) { this.questionnaireVersion = questionnaireVersion; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getConditionExpression() { return conditionExpression; }
    public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }

    public PreliminaryScreeningResult getPreliminaryResult() { return preliminaryResult; }
    public void setPreliminaryResult(PreliminaryScreeningResult preliminaryResult) { this.preliminaryResult = preliminaryResult; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
