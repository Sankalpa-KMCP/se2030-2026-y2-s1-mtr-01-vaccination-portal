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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

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
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public OperatingHours() {
    }

    public OperatingHours(VaccinationCenter center,
                          DayOfWeek dayOfWeek,
                          LocalTime opening,
                          LocalTime closing,
                          LocalDate effectiveFrom,
                          LocalDate effectiveTo) {
        this.center = center;
        this.dayOfWeek = dayOfWeek;
        this.opening = opening;
        this.closing = closing;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        validateConstraints();
    }

    public OperatingHours(VaccinationCenter center,
                          DayOfWeek dayOfWeek,
                          LocalTime opening,
                          LocalTime closing,
                          LocalDate effectiveFrom) {
        this(center, dayOfWeek, opening, closing, effectiveFrom, null);
    }

    /**
     * Validates domain constraints and business invariants for OperatingHours.
     *
     * @throws IllegalArgumentException if any constraint is violated
     */
    public void validateConstraints() {
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
     * Updates schedule parameters and revalidates domain invariants.
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
        this.dayOfWeek = dayOfWeek;
        this.opening = opening;
        this.closing = closing;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        validateConstraints();
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

    public VaccinationCenter getCenter() {
        return center;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getOpening() {
        return opening;
    }

    public void setOpening(LocalTime opening) {
        this.opening = opening;
    }

    public LocalTime getClosing() {
        return closing;
    }

    public void setClosing(LocalTime closing) {
        this.closing = closing;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
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
        OperatingHours that = (OperatingHours) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(center != null ? center.getId() : null, that.center != null ? that.center.getId() : null) &&
                dayOfWeek == that.dayOfWeek &&
                Objects.equals(opening, that.opening) &&
                Objects.equals(closing, that.closing) &&
                Objects.equals(effectiveFrom, that.effectiveFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, center != null ? center.getId() : null, dayOfWeek, opening, closing, effectiveFrom);
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
