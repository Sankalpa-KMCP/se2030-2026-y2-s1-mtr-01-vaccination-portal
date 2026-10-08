package com.se2030.vaccinationportal.screening.questionnaire.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.Instant;

@Entity
@Table(
    name = "questionnaire_item",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_questionnaire_item_order", columnNames = {"version_id", "display_order"}),
        @UniqueConstraint(name = "uq_questionnaire_item_question", columnNames = {"version_id", "question_id"})
    }
)
public class QuestionnaireItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false, foreignKey = @ForeignKey(name = "fk_questionnaire_item_version"))
    private QuestionnaireVersion questionnaireVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "fk_questionnaire_item_question"))
    private ScreeningQuestion question;

    @Min(value = 1, message = "Display order must be at least 1")
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "required_override")
    private Boolean requiredOverride;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public QuestionnaireItem() {}

    public QuestionnaireItem(QuestionnaireVersion questionnaireVersion, ScreeningQuestion question, int displayOrder, Boolean requiredOverride) {
        this.questionnaireVersion = questionnaireVersion;
        this.question = question;
        this.displayOrder = displayOrder;
        this.requiredOverride = requiredOverride;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public QuestionnaireVersion getQuestionnaireVersion() { return questionnaireVersion; }
    public void setQuestionnaireVersion(QuestionnaireVersion questionnaireVersion) { this.questionnaireVersion = questionnaireVersion; }

    public ScreeningQuestion getQuestion() { return question; }
    public void setQuestion(ScreeningQuestion question) { this.question = question; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getRequiredOverride() { return requiredOverride; }
    public void setRequiredOverride(Boolean requiredOverride) { this.requiredOverride = requiredOverride; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
