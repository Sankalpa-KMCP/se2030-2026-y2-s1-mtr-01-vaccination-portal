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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;

/**
 * Operational capacity plan and throughput snapshot for a vaccination session.
 * Preserves immutable historical revisions per session.
 * Canonical Section 6.4 and Table 12.1.
 */
@Entity
@Table(
        name = "session_capacity_plans",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_capacity_plan_revision",
                        columnNames = {"session_id", "revision"}
                )
        },
        indexes = {
                @Index(name = "idx_capacity_plan_session_rev", columnList = "session_id, revision")
        }
)
public class SessionCapacityPlan {

    public static final int DEFAULT_EXPECTED_DWELL_MINUTES = 15;
    public static final double DEFAULT_ACCOMPANYING_RATIO = 0.0;
    public static final int MAX_OVERRIDE_REASON_LENGTH = 255;
    public static final int MAX_APPROVED_BY_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Session is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private VaccinationSession session;

    @Min(value = 1, message = "Revision must be at least 1")
    @Column(name = "revision", nullable = false)
    private int revision = 1;

    @Min(value = 1, message = "Duration minutes must be positive")
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Min(value = 1, message = "Expected service minutes must be positive")
    @Column(name = "expected_service_minutes", nullable = false)
    private int expectedServiceMinutes = 10;

    @Min(value = 0, message = "Assigned vaccinator count must be non-negative")
    @Column(name = "assigned_vaccinator_count", nullable = false)
    private int assignedVaccinatorCount;

    @Min(value = 0, message = "Available station count must be non-negative")
    @Column(name = "available_station_count", nullable = false)
    private int availableStationCount;

    @Min(value = 0, message = "Fully equipped station count must be non-negative")
    @Column(name = "fully_equipped_station_count", nullable = false)
    private int fullyEquippedStationCount;

    @Min(value = 0, message = "Effective station count must be non-negative")
    @Column(name = "effective_station_count", nullable = false)
    private int effectiveStationCount;

    @Min(value = 0, message = "Recommended throughput must be non-negative")
    @Column(name = "recommended_throughput", nullable = false)
    private int recommendedThroughput;

    @Min(value = 0, message = "Approved capacity must be non-negative")
    @Column(name = "approved_capacity", nullable = false)
    private int approvedCapacity;

    @Min(value = 1, message = "Safe concurrent occupancy must be positive")
    @Column(name = "safe_concurrent_occupancy", nullable = false)
    private int safeConcurrentOccupancy;

    @Min(value = 1, message = "Expected dwell minutes must be positive")
    @Column(name = "expected_dwell_minutes", nullable = false)
    private int expectedDwellMinutes = DEFAULT_EXPECTED_DWELL_MINUTES;

    @DecimalMin(value = "0.0", message = "Expected accompanying ratio must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Expected accompanying ratio must not exceed 1.0")
    @Column(name = "expected_accompanying_persons_per_booking", nullable = false)
    private double expectedAccompanyingPersonsPerBooking = DEFAULT_ACCOMPANYING_RATIO;

    @Min(value = 1, message = "Session service ceiling must be positive if specified")
    @Column(name = "session_service_ceiling")
    private Integer sessionServiceCeiling;

    @Size(max = MAX_OVERRIDE_REASON_LENGTH, message = "Override reason must not exceed 255 characters")
    @Column(name = "override_reason", length = MAX_OVERRIDE_REASON_LENGTH)
    private String overrideReason;

    @Size(max = MAX_APPROVED_BY_LENGTH, message = "Approved by actor ID must not exceed 64 characters")
    @Column(name = "approved_by", length = MAX_APPROVED_BY_LENGTH)
    private String approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SessionCapacityPlan() {
    }

    public SessionCapacityPlan(VaccinationSession session,
                               int revision,
                               int durationMinutes,
                               int expectedServiceMinutes,
                               int assignedVaccinatorCount,
                               int availableStationCount,
                               int fullyEquippedStationCount,
                               int effectiveStationCount,
                               int recommendedThroughput,
                               int approvedCapacity,
                               int safeConcurrentOccupancy,
                               int expectedDwellMinutes,
                               double expectedAccompanyingPersonsPerBooking,
                               Integer sessionServiceCeiling,
                               String overrideReason,
                               String approvedBy) {
        validateInvariants(
                session,
                revision,
                durationMinutes,
                expectedServiceMinutes,
                assignedVaccinatorCount,
                availableStationCount,
                fullyEquippedStationCount,
                effectiveStationCount,
                recommendedThroughput,
                approvedCapacity,
                safeConcurrentOccupancy,
                expectedDwellMinutes,
                expectedAccompanyingPersonsPerBooking,
                sessionServiceCeiling,
                overrideReason
        );

        this.session = session;
        this.revision = revision;
        this.durationMinutes = durationMinutes;
        this.expectedServiceMinutes = expectedServiceMinutes;
        this.assignedVaccinatorCount = assignedVaccinatorCount;
        this.availableStationCount = availableStationCount;
        this.fullyEquippedStationCount = fullyEquippedStationCount;
        this.effectiveStationCount = effectiveStationCount;
        this.recommendedThroughput = recommendedThroughput;
        this.approvedCapacity = approvedCapacity;
        this.safeConcurrentOccupancy = safeConcurrentOccupancy;
        this.expectedDwellMinutes = expectedDwellMinutes;
        this.expectedAccompanyingPersonsPerBooking = expectedAccompanyingPersonsPerBooking;
        this.sessionServiceCeiling = sessionServiceCeiling;
        this.overrideReason = overrideReason != null && !overrideReason.trim().isEmpty() ? overrideReason.trim() : null;
        this.approvedBy = approvedBy != null && !approvedBy.trim().isEmpty() ? approvedBy.trim() : null;
    }

    public static void validateInvariants(VaccinationSession session,
                                          int revision,
                                          int durationMinutes,
                                          int expectedServiceMinutes,
                                          int assignedVaccinatorCount,
                                          int availableStationCount,
                                          int fullyEquippedStationCount,
                                          int effectiveStationCount,
                                          int recommendedThroughput,
                                          int approvedCapacity,
                                          int safeConcurrentOccupancy,
                                          int expectedDwellMinutes,
                                          double expectedAccompanyingPersonsPerBooking,
                                          Integer sessionServiceCeiling,
                                          String overrideReason) {
        if (session == null) {
            throw new IllegalArgumentException("Session is required");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("Revision must be at least 1");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration minutes must be positive");
        }
        if (expectedServiceMinutes <= 0) {
            throw new IllegalArgumentException("Expected service minutes must be positive");
        }
        if (assignedVaccinatorCount < 0) {
            throw new IllegalArgumentException("Assigned vaccinator count must be non-negative");
        }
        if (availableStationCount < 0) {
            throw new IllegalArgumentException("Available station count must be non-negative");
        }
        if (fullyEquippedStationCount < 0) {
            throw new IllegalArgumentException("Fully equipped station count must be non-negative");
        }
        if (fullyEquippedStationCount > availableStationCount) {
            throw new IllegalArgumentException("Fully equipped station count (" + fullyEquippedStationCount
                    + ") cannot exceed available station count (" + availableStationCount + ")");
        }
        int expectedEffective = Math.min(assignedVaccinatorCount, Math.min(availableStationCount, fullyEquippedStationCount));
        if (effectiveStationCount != expectedEffective) {
            throw new IllegalArgumentException("Effective station count (" + effectiveStationCount
                    + ") must equal min(vaccinators, stations, equipped) = " + expectedEffective);
        }
        int expectedThroughput = effectiveStationCount * (durationMinutes / expectedServiceMinutes);
        if (recommendedThroughput != expectedThroughput) {
            throw new IllegalArgumentException("Recommended throughput (" + recommendedThroughput
                    + ") must equal effectiveStations * floor(duration / serviceMinutes) = " + expectedThroughput);
        }
        if (approvedCapacity < 0) {
            throw new IllegalArgumentException("Approved capacity must be non-negative");
        }
        if (safeConcurrentOccupancy <= 0) {
            throw new IllegalArgumentException("Safe concurrent occupancy must be positive");
        }
        if (expectedDwellMinutes <= 0) {
            throw new IllegalArgumentException("Expected dwell minutes must be positive");
        }
        if (expectedAccompanyingPersonsPerBooking < 0.0 || expectedAccompanyingPersonsPerBooking > 1.0) {
            throw new IllegalArgumentException("Expected accompanying persons per booking must be between 0.0 and 1.0");
        }
        if (sessionServiceCeiling != null && sessionServiceCeiling <= 0) {
            throw new IllegalArgumentException("Session service ceiling must be positive if specified");
        }
        if (sessionServiceCeiling != null && approvedCapacity > sessionServiceCeiling) {
            throw new IllegalArgumentException("Approved capacity (" + approvedCapacity
                    + ") cannot exceed session service ceiling (" + sessionServiceCeiling + ")");
        }
        if (approvedCapacity > recommendedThroughput) {
            if (overrideReason == null || overrideReason.trim().isEmpty()) {
                throw new IllegalArgumentException("An override reason is required when approved capacity exceeds recommended throughput");
            }
        }
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public VaccinationSession getSession() {
        return session;
    }

    public void setSession(VaccinationSession session) {
        this.session = session;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getExpectedServiceMinutes() {
        return expectedServiceMinutes;
    }

    public void setExpectedServiceMinutes(int expectedServiceMinutes) {
        this.expectedServiceMinutes = expectedServiceMinutes;
    }

    public int getAssignedVaccinatorCount() {
        return assignedVaccinatorCount;
    }

    public void setAssignedVaccinatorCount(int assignedVaccinatorCount) {
        this.assignedVaccinatorCount = assignedVaccinatorCount;
    }

    public int getAvailableStationCount() {
        return availableStationCount;
    }

    public void setAvailableStationCount(int availableStationCount) {
        this.availableStationCount = availableStationCount;
    }

    public int getFullyEquippedStationCount() {
        return fullyEquippedStationCount;
    }

    public void setFullyEquippedStationCount(int fullyEquippedStationCount) {
        this.fullyEquippedStationCount = fullyEquippedStationCount;
    }

    public int getEffectiveStationCount() {
        return effectiveStationCount;
    }

    public void setEffectiveStationCount(int effectiveStationCount) {
        this.effectiveStationCount = effectiveStationCount;
    }

    public int getRecommendedThroughput() {
        return recommendedThroughput;
    }

    public void setRecommendedThroughput(int recommendedThroughput) {
        this.recommendedThroughput = recommendedThroughput;
    }

    public int getApprovedCapacity() {
        return approvedCapacity;
    }

    public void setApprovedCapacity(int approvedCapacity) {
        this.approvedCapacity = approvedCapacity;
    }

    public int getSafeConcurrentOccupancy() {
        return safeConcurrentOccupancy;
    }

    public void setSafeConcurrentOccupancy(int safeConcurrentOccupancy) {
        this.safeConcurrentOccupancy = safeConcurrentOccupancy;
    }

    public int getExpectedDwellMinutes() {
        return expectedDwellMinutes;
    }

    public void setExpectedDwellMinutes(int expectedDwellMinutes) {
        this.expectedDwellMinutes = expectedDwellMinutes;
    }

    public double getExpectedAccompanyingPersonsPerBooking() {
        return expectedAccompanyingPersonsPerBooking;
    }

    public void setExpectedAccompanyingPersonsPerBooking(double expectedAccompanyingPersonsPerBooking) {
        this.expectedAccompanyingPersonsPerBooking = expectedAccompanyingPersonsPerBooking;
    }

    public Integer getSessionServiceCeiling() {
        return sessionServiceCeiling;
    }

    public void setSessionServiceCeiling(Integer sessionServiceCeiling) {
        this.sessionServiceCeiling = sessionServiceCeiling;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionCapacityPlan that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
