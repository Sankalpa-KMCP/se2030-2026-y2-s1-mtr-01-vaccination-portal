package com.se2030.vaccinationportal.screening.questionnaire.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "answer_option",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_answer_option_question_code", columnNames = {"question_id", "code"})
    }
)
public class AnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "fk_answer_option_question"))
    private ScreeningQuestion question;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "typed_value", nullable = false, length = 255)
    private String typedValue;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

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

    public AnswerOption() {}

    public AnswerOption(ScreeningQuestion question, String code, String label, String typedValue, int displayOrder) {
        this.question = question;
        this.code = code;
        this.label = label;
        this.typedValue = typedValue;
        this.displayOrder = displayOrder;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ScreeningQuestion getQuestion() { return question; }
    public void setQuestion(ScreeningQuestion question) { this.question = question; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getTypedValue() { return typedValue; }
    public void setTypedValue(String typedValue) { this.typedValue = typedValue; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
