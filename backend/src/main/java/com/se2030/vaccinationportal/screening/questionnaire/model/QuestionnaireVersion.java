package com.se2030.vaccinationportal.screening.questionnaire.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "questionnaire_version",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_questionnaire_version", columnNames = {"questionnaire_id", "version_number"})
    }
)
public class QuestionnaireVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionnaire_id", nullable = false, foreignKey = @ForeignKey(name = "fk_questionnaire_version_questionnaire"))
    private Questionnaire questionnaire;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private QuestionnaireVersionStatus status = QuestionnaireVersionStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @OneToMany(mappedBy = "questionnaireVersion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<QuestionnaireItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "questionnaireVersion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("priority ASC")
    private List<EligibilityRule> rules = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public QuestionnaireVersion() {}

    public QuestionnaireVersion(Questionnaire questionnaire, Integer versionNumber, QuestionnaireVersionStatus status) {
        this.questionnaire = questionnaire;
        this.versionNumber = versionNumber;
        this.status = status;
    }

    public void addItem(QuestionnaireItem item) {
        items.add(item);
        item.setQuestionnaireVersion(this);
    }

    public void removeItem(QuestionnaireItem item) {
        items.remove(item);
        item.setQuestionnaireVersion(null);
    }

    public void addRule(EligibilityRule rule) {
        rules.add(rule);
        rule.setQuestionnaireVersion(this);
    }

    public void removeRule(EligibilityRule rule) {
        rules.remove(rule);
        rule.setQuestionnaireVersion(null);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Questionnaire getQuestionnaire() { return questionnaire; }
    public void setQuestionnaire(Questionnaire questionnaire) { this.questionnaire = questionnaire; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public QuestionnaireVersionStatus getStatus() { return status; }
    public void setStatus(QuestionnaireVersionStatus status) { this.status = status; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getRetiredAt() { return retiredAt; }
    public void setRetiredAt(Instant retiredAt) { this.retiredAt = retiredAt; }

    public List<QuestionnaireItem> getItems() { return items; }
    public void setItems(List<QuestionnaireItem> items) { this.items = items; }

    public List<EligibilityRule> getRules() { return rules; }
    public void setRules(List<EligibilityRule> rules) { this.rules = rules; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
