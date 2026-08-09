package com.se2030.vaccinationportal.campaign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing an effective-dated assignment of a published questionnaire version to a vaccination campaign.
 * Defined by canonical specification WBVP-SE2030 section 2, 5, 6, 12 (Table 12.1).
 *
 * <p>Canonical Rules & Boundaries:</p>
 * <ul>
 *   <li>MF-01 owns detailed questionnaire versions and eligibility rules.</li>
 *   <li>MF-02 owns the campaign and the effective-dated selection of a published questionnaire version.</li>
 *   <li>{@code questionnaire_version_id} is a scalar cross-module reference until MF-01 is implemented.</li>
 *   <li>Effective-history invariant (BR-028 / DEC-030): overlapping effective periods are prohibited, ensuring at most one active assignment at any instant during configuration.</li>
 *   <li>Publication prerequisite (MF02-BR-02): campaign publication requires exactly one effective assignment linked to a PUBLISHED questionnaire version.</li>
 *   <li>Questionnaire assignment changes apply only to future screening submissions (MF02-BR-17).</li>
 * </ul>
 */
@Entity
@Table(
        name = "campaign_questionnaire_assignments",
        indexes = {
                @Index(name = "idx_cqa_campaign_active", columnList = "campaign_id, active_from, active_to"),
                @Index(name = "idx_cqa_qversion", columnList = "questionnaire_version_id")
        }
)
public class CampaignQuestionnaireAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Campaign is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private VaccinationCampaign campaign;

    @NotNull(message = "Questionnaire version ID is required")
    @Positive(message = "Questionnaire version ID must be positive")
    @Column(name = "questionnaire_version_id", nullable = false)
    private Long questionnaireVersionId;

    @NotNull(message = "Active from date is required")
    @Column(name = "active_from", nullable = false)
    private LocalDate activeFrom;

    @Column(name = "active_to")
    private LocalDate activeTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CampaignQuestionnaireAssignment() {
    }

    public CampaignQuestionnaireAssignment(VaccinationCampaign campaign,
                                           Long questionnaireVersionId,
                                           LocalDate activeFrom,
                                           LocalDate activeTo) {
        this.campaign = campaign;
        this.questionnaireVersionId = questionnaireVersionId;
        this.activeFrom = activeFrom;
        this.activeTo = activeTo;
        validateConstraints();
    }

    /**
     * Validates domain invariants for CampaignQuestionnaireAssignment.
     */
    public void validateConstraints() {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign is required");
        }
        if (questionnaireVersionId == null) {
            throw new IllegalArgumentException("Questionnaire version ID is required");
        }
        if (questionnaireVersionId <= 0) {
            throw new IllegalArgumentException("Questionnaire version ID must be positive: " + questionnaireVersionId);
        }
        if (activeFrom == null) {
            throw new IllegalArgumentException("Active from date is required");
        }
        if (activeTo != null && activeTo.isBefore(activeFrom)) {
            throw new IllegalArgumentException("Active to date (" + activeTo + ") cannot be before active from date (" + activeFrom + ")");
        }
    }

    /**
     * Evaluates whether this assignment is effective on the given date (inclusive boundary).
     *
     * @param date Date to evaluate
     * @return true if date is within [activeFrom, activeTo] (or activeTo is null and date >= activeFrom)
     */
    public boolean isEffectiveOn(LocalDate date) {
        if (date == null) {
            return false;
        }
        boolean afterOrOnFrom = !date.isBefore(activeFrom);
        boolean beforeOrOnTo = (activeTo == null || !date.isAfter(activeTo));
        return afterOrOnFrom && beforeOrOnTo;
    }

    @PrePersist
    protected void onCreate() {
        validateConstraints();
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        validateConstraints();
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VaccinationCampaign getCampaign() {
        return campaign;
    }

    public void setCampaign(VaccinationCampaign campaign) {
        this.campaign = campaign;
    }

    public Long getQuestionnaireVersionId() {
        return questionnaireVersionId;
    }

    public void setQuestionnaireVersionId(Long questionnaireVersionId) {
        this.questionnaireVersionId = questionnaireVersionId;
    }

    public LocalDate getActiveFrom() {
        return activeFrom;
    }

    public void setActiveFrom(LocalDate activeFrom) {
        this.activeFrom = activeFrom;
    }

    public LocalDate getActiveTo() {
        return activeTo;
    }

    public void setActiveTo(LocalDate activeTo) {
        this.activeTo = activeTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CampaignQuestionnaireAssignment that = (CampaignQuestionnaireAssignment) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(campaign != null ? campaign.getId() : null, that.campaign != null ? that.campaign.getId() : null) &&
                Objects.equals(questionnaireVersionId, that.questionnaireVersionId) &&
                Objects.equals(activeFrom, that.activeFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, campaign != null ? campaign.getId() : null, questionnaireVersionId, activeFrom);
    }

    @Override
    public String toString() {
        return "CampaignQuestionnaireAssignment{" +
                "id=" + id +
                ", campaignId=" + (campaign != null ? campaign.getId() : null) +
                ", questionnaireVersionId=" + questionnaireVersionId +
                ", activeFrom=" + activeFrom +
                ", activeTo=" + activeTo +
                '}';
    }
}
