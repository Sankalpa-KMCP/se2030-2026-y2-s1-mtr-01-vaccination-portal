package com.se2030.vaccinationportal.campaign.core.model;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;

/**
 * Domain entity representing an administrative service-area location target for a vaccination campaign.
 * Defined by canonical specification WBVP-SE2030 section 6.1 / 12 (Table 12.1).
 * Controls citizen-facing service visibility and access in the portal (MF02-BR-04 / BR-023).
 * Does not represent clinical or medical eligibility rules.
 */
@Entity
@Table(
        name = "campaign_location_targets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_campaign_location_target",
                        columnNames = {"campaign_id", "target_type", "target_code"}
                )
        }
)
public class CampaignLocationTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Campaign is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private VaccinationCampaign campaign;

    @NotNull(message = "Target type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private LocationTargetType targetType;

    @NotBlank(message = "Target code is required")
    @Size(max = 64, message = "Target code must not exceed 64 characters")
    @Column(name = "target_code", nullable = false, length = 64)
    private String targetCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CampaignLocationTarget() {
    }

    public CampaignLocationTarget(VaccinationCampaign campaign, LocationTargetType targetType, String targetCode) {
        this.campaign = campaign;
        this.targetType = targetType;
        this.targetCode = normalizeTargetCode(targetCode);
        validateConstraints();
    }

    /**
     * Normalizes target code by trimming and converting to uppercase using Locale.ROOT.
     * Implementation choice to prevent whitespace and case-sensitivity duplicates.
     *
     * @param code Raw target code (e.g. "  wp ", "colombo")
     * @return Trimmed uppercase target code (e.g. "WP", "COLOMBO")
     */
    public static String normalizeTargetCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Target code must not be null or blank");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 64) {
            throw new IllegalArgumentException("Target code must not exceed 64 characters: '" + normalized + "'");
        }
        return normalized;
    }

    /**
     * Validates domain constraints for CampaignLocationTarget.
     */
    public void validateConstraints() {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign is required");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Target type is required");
        }
        if (targetCode == null || targetCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Target code is required");
        }
    }

    @PrePersist
    protected void onCreate() {
        validateConstraints();
        this.targetCode = normalizeTargetCode(this.targetCode);
        Instant now = Instant.now();
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
        this.targetCode = normalizeTargetCode(this.targetCode);
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public VaccinationCampaign getCampaign() {
        return campaign;
    }

    public LocationTargetType getTargetType() {
        return targetType;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "CampaignLocationTarget{" +
                "id=" + id +
                ", campaignId=" + (campaign != null ? campaign.getId() : null) +
                ", targetType=" + targetType +
                ", targetCode='" + targetCode + '\'' +
                '}';
    }
}
