package com.se2030.vaccinationportal.campaign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Domain entity representing a Campaign Dose Programme.
 * Defines programme constraints (dose category, permitted dose numbers, minimum intervals,
 * evidence requirements, and expected service minutes) for a campaign.
 * Preserves effective-dated versioning (BR-028 / DEC-030) ensuring at most one active version during configuration,
 * with exactly one effective version required at campaign publication (MF02-BR-02).
 */
@Entity
@Table(
        name = "campaign_dose_programmes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_campaign_dose_programme_version",
                        columnNames = {"campaign_id", "version_number"}
                )
        }
)
public class CampaignDoseProgramme {

    public static final int DEFAULT_EXPECTED_SERVICE_MINUTES = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Campaign is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private VaccinationCampaign campaign;

    @Min(value = 1, message = "Version number must be at least 1")
    @Column(name = "version_number", nullable = false)
    private int versionNumber = 1;

    @NotNull(message = "Dose category is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "dose_category", nullable = false, length = 32)
    private DoseCategory doseCategory;

    @NotBlank(message = "Permitted dose numbers are required")
    @Size(max = 64, message = "Permitted dose numbers must not exceed 64 characters")
    @Column(name = "permitted_dose_numbers", nullable = false, length = 64)
    private String permittedDoseNumbers;

    @Min(value = 0, message = "Minimum interval days must be non-negative")
    @Column(name = "minimum_interval_days", nullable = false)
    private int minimumIntervalDays = 0;

    @Column(name = "previous_evidence_required", nullable = false)
    private boolean previousEvidenceRequired = false;

    @Min(value = 1, message = "Expected service minutes must be a positive integer")
    @Column(name = "expected_service_minutes", nullable = false)
    private int expectedServiceMinutes = DEFAULT_EXPECTED_SERVICE_MINUTES;

    @NotNull(message = "Active from date is required")
    @Column(name = "active_from", nullable = false)
    private LocalDate activeFrom;

    @Column(name = "active_to")
    private LocalDate activeTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CampaignDoseProgramme() {
    }

    public CampaignDoseProgramme(VaccinationCampaign campaign,
                                 int versionNumber,
                                 DoseCategory doseCategory,
                                 String permittedDoseNumbers,
                                 int minimumIntervalDays,
                                 boolean previousEvidenceRequired,
                                 Integer expectedServiceMinutes,
                                 LocalDate activeFrom,
                                 LocalDate activeTo) {
        this.campaign = campaign;
        this.versionNumber = versionNumber;
        this.doseCategory = doseCategory;
        this.permittedDoseNumbers = permittedDoseNumbers;
        this.minimumIntervalDays = minimumIntervalDays;
        this.previousEvidenceRequired = previousEvidenceRequired;
        this.expectedServiceMinutes = (expectedServiceMinutes != null && expectedServiceMinutes > 0)
                ? expectedServiceMinutes
                : (expectedServiceMinutes != null ? expectedServiceMinutes : DEFAULT_EXPECTED_SERVICE_MINUTES);
        this.activeFrom = activeFrom;
        this.activeTo = activeTo;
        validateConstraints();
    }

    public CampaignDoseProgramme(VaccinationCampaign campaign,
                                 int versionNumber,
                                 DoseCategory doseCategory,
                                 String permittedDoseNumbers,
                                 int minimumIntervalDays,
                                 boolean previousEvidenceRequired,
                                 LocalDate activeFrom) {
        this(campaign, versionNumber, doseCategory, permittedDoseNumbers, minimumIntervalDays,
                previousEvidenceRequired, DEFAULT_EXPECTED_SERVICE_MINUTES, activeFrom, null);
    }

    /**
     * Validates domain constraints and business rules for the dose programme.
     */
    public void validateConstraints() {
        if (versionNumber < 1) {
            throw new IllegalArgumentException("Version number must be at least 1");
        }
        if (doseCategory == null) {
            throw new IllegalArgumentException("Dose category is required");
        }
        if (activeFrom == null) {
            throw new IllegalArgumentException("Active from date is required");
        }
        if (activeTo != null && activeTo.isBefore(activeFrom)) {
            throw new IllegalArgumentException(
                    "Active to date (" + activeTo + ") cannot be before active from date (" + activeFrom + ")"
            );
        }
        if (minimumIntervalDays < 0) {
            throw new IllegalArgumentException("Minimum interval days must be non-negative");
        }
        if (expectedServiceMinutes <= 0) {
            throw new IllegalArgumentException("Expected service minutes must be a positive integer");
        }
        validatePermittedDoseNumbers(this.permittedDoseNumbers);
    }

    /**
     * Validates permitted dose numbers string format.
     * Must contain positive, comma-separated integers without duplicates.
     * Note: String storage is an implementation choice consistent with the database schema.
     *
     * @param input Raw permitted dose numbers string (e.g. "1,2", "1", "3")
     * @return List of parsed distinct positive dose numbers
     */
    public static List<Integer> validatePermittedDoseNumbers(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Permitted dose numbers must not be blank");
        }
        String[] tokens = input.split(",");
        List<Integer> parsedList = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();

        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Permitted dose numbers contains empty element: '" + input + "'");
            }
            int doseNum;
            try {
                doseNum = Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Permitted dose number must be a valid integer: '" + trimmed + "'");
            }
            if (doseNum <= 0) {
                throw new IllegalArgumentException("Permitted dose number must be a positive integer: " + doseNum);
            }
            if (!seen.add(doseNum)) {
                throw new IllegalArgumentException("Duplicate permitted dose number: " + doseNum);
            }
            parsedList.add(doseNum);
        }
        return Collections.unmodifiableList(parsedList);
    }

    /**
     * Checks if this programme version is effective on the given date.
     *
     * @param date Date to evaluate
     * @return true if date is within [activeFrom, activeTo] (activeTo is open-ended if null)
     */
    public boolean isEffectiveOn(LocalDate date) {
        if (date == null || activeFrom == null) {
            return false;
        }
        if (date.isBefore(activeFrom)) {
            return false;
        }
        return activeTo == null || !date.isAfter(activeTo);
    }

    /**
     * Returns parsed list of permitted dose numbers.
     *
     * @return List of positive integer dose numbers
     */
    public List<Integer> getParsedPermittedDoseNumbers() {
        return validatePermittedDoseNumbers(this.permittedDoseNumbers);
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
        if (expectedServiceMinutes <= 0) {
            expectedServiceMinutes = DEFAULT_EXPECTED_SERVICE_MINUTES;
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

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public DoseCategory getDoseCategory() {
        return doseCategory;
    }

    public void setDoseCategory(DoseCategory doseCategory) {
        this.doseCategory = doseCategory;
    }

    public String getPermittedDoseNumbers() {
        return permittedDoseNumbers;
    }

    public void setPermittedDoseNumbers(String permittedDoseNumbers) {
        this.permittedDoseNumbers = permittedDoseNumbers;
    }

    public int getMinimumIntervalDays() {
        return minimumIntervalDays;
    }

    public void setMinimumIntervalDays(int minimumIntervalDays) {
        this.minimumIntervalDays = minimumIntervalDays;
    }

    public boolean isPreviousEvidenceRequired() {
        return previousEvidenceRequired;
    }

    public void setPreviousEvidenceRequired(boolean previousEvidenceRequired) {
        this.previousEvidenceRequired = previousEvidenceRequired;
    }

    public int getExpectedServiceMinutes() {
        return expectedServiceMinutes;
    }

    public void setExpectedServiceMinutes(int expectedServiceMinutes) {
        this.expectedServiceMinutes = expectedServiceMinutes;
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
        CampaignDoseProgramme that = (CampaignDoseProgramme) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return versionNumber == that.versionNumber &&
                Objects.equals(campaign != null ? campaign.getId() : null, that.campaign != null ? that.campaign.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, campaign != null ? campaign.getId() : null, versionNumber);
    }

    @Override
    public String toString() {
        return "CampaignDoseProgramme{" +
                "id=" + id +
                ", campaignId=" + (campaign != null ? campaign.getId() : null) +
                ", versionNumber=" + versionNumber +
                ", doseCategory=" + doseCategory +
                ", permittedDoseNumbers='" + permittedDoseNumbers + '\'' +
                ", minimumIntervalDays=" + minimumIntervalDays +
                ", previousEvidenceRequired=" + previousEvidenceRequired +
                ", expectedServiceMinutes=" + expectedServiceMinutes +
                ", activeFrom=" + activeFrom +
                ", activeTo=" + activeTo +
                '}';
    }
}
