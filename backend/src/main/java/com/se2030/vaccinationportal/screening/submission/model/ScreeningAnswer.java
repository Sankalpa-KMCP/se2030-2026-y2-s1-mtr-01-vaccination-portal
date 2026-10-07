package com.se2030.vaccinationportal.screening.submission.model;

import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;


import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "screening_answer",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_screening_ans_sub_quest", columnNames = {"submission_id", "question_id"})
    }
)
public class ScreeningAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false, foreignKey = @ForeignKey(name = "fk_screening_ans_submission"))
    private ScreeningSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "fk_screening_ans_question"))
    private ScreeningQuestion question;

    @Column(name = "typed_answer", nullable = false, columnDefinition = "TEXT")
    private String typedAnswer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public ScreeningAnswer() {}

    public ScreeningAnswer(ScreeningSubmission submission, ScreeningQuestion question, String typedAnswer) {
        this.submission = submission;
        this.question = question;
        this.typedAnswer = typedAnswer;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ScreeningSubmission getSubmission() { return submission; }
    public void setSubmission(ScreeningSubmission submission) { this.submission = submission; }

    public ScreeningQuestion getQuestion() { return question; }
    public void setQuestion(ScreeningQuestion question) { this.question = question; }

    public String getTypedAnswer() { return typedAnswer; }
    public void setTypedAnswer(String typedAnswer) { this.typedAnswer = typedAnswer; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
