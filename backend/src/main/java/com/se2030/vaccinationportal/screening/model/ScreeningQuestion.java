package com.se2030.vaccinationportal.screening.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "screening_question",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_screening_question_code", columnNames = {"code"})
    }
)
public class ScreeningQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "text", nullable = false, length = 1000)
    private String text;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 32)
    private QuestionType questionType;

    @Column(name = "default_required", nullable = false)
    private boolean defaultRequired = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<AnswerOption> answerOptions = new ArrayList<>();

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

    public ScreeningQuestion() {}

    public ScreeningQuestion(String code, String text, QuestionType questionType, boolean defaultRequired, boolean active) {
        this.code = code;
        this.text = text;
        this.questionType = questionType;
        this.defaultRequired = defaultRequired;
        this.active = active;
    }

    public void addAnswerOption(AnswerOption option) {
        answerOptions.add(option);
        option.setQuestion(this);
    }

    public void removeAnswerOption(AnswerOption option) {
        answerOptions.remove(option);
        option.setQuestion(null);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }

    public boolean isDefaultRequired() { return defaultRequired; }
    public void setDefaultRequired(boolean defaultRequired) { this.defaultRequired = defaultRequired; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<AnswerOption> getAnswerOptions() { return answerOptions; }
    public void setAnswerOptions(List<AnswerOption> answerOptions) { this.answerOptions = answerOptions; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
