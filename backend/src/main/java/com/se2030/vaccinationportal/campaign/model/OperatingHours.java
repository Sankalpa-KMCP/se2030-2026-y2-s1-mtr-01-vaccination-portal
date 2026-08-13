package com.se2030.vaccinationportal.campaign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Domain entity representing regular weekly operating hours for a vaccination center.
 * Defined by canonical specification WBVP-SE2030 section 6.2 / 12 (Table 12.1).
 *
 * <p>Domain Semantics:</p>
 * <ul>
 *   <li>One record represents: one center + one weekday + one opening/closing interval + one inclusive effective-date period.</li>
 *   <li>{@code center}: Mandatory parent vaccination center (immutable owner).</li>
 *   <li>{@code dayOfWeek}: Day of the week ({@link DayOfWeek}).</li>
 *   <li>{@code opening}: Opening time (must be strictly before {@code closing}).</li>
 *   <li>{@code closing}: Closing time (must be strictly after {@code opening}).</li>
 *   <li>{@code effectiveFrom}: Inclusive start date of schedule validity.</li>
 *   <li>{@code effectiveTo}: Inclusive end date of schedule validity (nullable for open-ended schedules).</li>
 * </ul>
 */
@Entity
@Table(
        name = "operating_hours",
        indexes = {
                @Index(name = "idx_operating_hours_center_day", columnList = "center_id, day_of_week")
        }
)
public class OperatingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Center is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private VaccinationCenter center;

    @NotNull(message = "Day of week is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 16)
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Opening time is required")
    @Column(name = "opening_time", nullable = false)
    private LocalTime opening;

    @NotNull(message = "Closing time is required")
    @Column(name = "closing_time", nullable = false)
    private LocalTime closing;

    @NotNull(message = "Effective from date is required")
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public OperatingHours() {
    }

    public OperatingHours(VaccinationCenter center,
                          DayOfWeek dayOfWeek,
                          LocalTime opening,
                          LocalTime closing,
                          LocalDate effectiveFrom,
                          LocalDate effectiveTo) {
        validateParameters(center, dayOfWeek, opening, closing, effectiveFrom, effectiveTo);
        this.center = center;
        this.dayOfWeek = dayOfWeek;
        this.opening = opening;
        this.closing = closing;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public OperatingHours(VaccinationCenter center,
                          DayOfWeek dayOfWeek,
                          LocalTime opening,
                          LocalTime closing,
                          LocalDate effectiveFrom) {
        this(center, dayOfWeek, opening, closing, effectiveFrom, null);
    }

    /**
     * Validates candidate parameters for operating hours schedule.
     *
     * @param center        Vaccination center
     * @param dayOfWeek     Day of week
     * @param opening       Opening time
     * @param closing       Closing time
     * @param effectiveFrom Effective from date
     * @param effectiveTo   Effective to date (nullable)
     * @throws IllegalArgumentException if any constraint is violated
     */
    public static void validateParameters(VaccinationCenter center,
                                          DayOfWeek dayOfWeek,
                                          LocalTime opening,
                                          LocalTime closing,
                                          LocalDate effectiveFrom,
                                          LocalDate effectiveTo) {
        if (center == null) {
            throw new IllegalArgumentException("Center is required");
        }
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("Day of week is required");
        }
        if (opening == null) {
            throw new IllegalArgumentException("Opening time is required");
        }
        if (closing == null) {
            throw new IllegalArgumentException("Closing time is required");
        }
        if (!opening.isBefore(closing)) {
            if (opening.equals(closing)) {
                throw new IllegalArgumentException("Opening time (" + opening + ") cannot equal closing time (" + closing + ")");
            } else {
                throw new IllegalArgumentException("Opening time (" + opening + ") must be strictly before closing time (" + closing + ")");
            }
        }
        if (effectiveFrom == null) {
            throw new IllegalArgumentException("Effective from date is required");
        }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Effective to date (" + effectiveTo + ") cannot be before effective from date (" + effectiveFrom + ")");
        }
    }

    /**
     * Validates domain constraints and business invariants for OperatingHours.
     *
     * @throws IllegalArgumentException if any constraint is violated
     */
    public void validateConstraints() {
        validateParameters(this.center, this.dayOfWeek, this.opening, this.closing, this.effectiveFrom, this.effectiveTo);
    }

    /**
     * Updates schedule parameters with candidate pre-validation.
     * Preserves existing entity state if validation fails.
     * Preserves immutable parent center reference.
     *
     * @param dayOfWeek     Updated day of week
     * @param opening       Updated opening time
     * @param closing       Updated closing time
     * @param effectiveFrom Updated effective from date
     * @param effectiveTo   Updated effective to date (nullable)
     */
    public void updateSchedule(DayOfWeek dayOfWeek,
                               LocalTime opening,
                               LocalTime closing,
                               LocalDate effectiveFrom,
                               LocalDate effectiveTo) {
        validateParameters(this.center, dayOfWeek, opening, closing, effectiveFrom, effectiveTo);
        this.dayOfWeek = dayOfWeek;
        this.opening = opening;
        this.closing = closing;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    /**
     * Evaluates whether these operating hours are effective on the given date (inclusive boundary).
     *
     * @param date Date to evaluate
     * @return true if date is within [effectiveFrom, effectiveTo] (or effectiveTo is null and date >= effectiveFrom)
     */
    public boolean isEffectiveOn(LocalDate date) {
        if (date == null) {
            return false;
        }
        boolean afterOrOnFrom = !date.isBefore(effectiveFrom);
        boolean beforeOrOnTo = (effectiveTo == null || !date.isAfter(effectiveTo));
        return afterOrOnFrom && beforeOrOnTo;
    }

    /**
     * Evaluates whether the given time falls within the operating interval [opening, closing).
     *
     * @param time Time to evaluate
     * @return true if time is on or after opening and strictly before closing
     */
    public boolean isOpenAt(LocalTime time) {
        if (time == null) {
            return false;
        }
        return (!time.isBefore(opening)) && time.isBefore(closing);
    }

    @PrePersist
    protected void onCreate() {
        validateConstraints();
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
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public VaccinationCenter getCenter() {
        return center;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getOpening() {
        return opening;
    }

    public LocalTime getClosing() {
        return closing;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void endOn(LocalDate effectiveTo) {
        if (effectiveTo == null || effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Effective to date cannot be null or before effective from date");
        }
        this.effectiveTo = effectiveTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "OperatingHours{" +
                "id=" + id +
                ", centerId=" + (center != null ? center.getId() : null) +
                ", dayOfWeek=" + dayOfWeek +
                ", opening=" + opening +
                ", closing=" + closing +
                ", effectiveFrom=" + effectiveFrom +
                ", effectiveTo=" + effectiveTo +
                '}';
    }
}
