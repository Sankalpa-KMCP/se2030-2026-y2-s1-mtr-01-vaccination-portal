package com.se2030.vaccinationportal.campaign.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Bounded weekly definition from which concrete vaccination sessions may later be generated. */
@Entity
@Table(
        name = "session_recurrence_rules",
        indexes = {
                @Index(name = "idx_recurrence_rule_campaign_range",
                        columnList = "campaign_id, start_date, end_date, recurrence_status"),
                @Index(name = "idx_recurrence_rule_center_range",
                        columnList = "center_id, start_date, end_date, recurrence_status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recurrence_rule_generation_key", columnNames = "generation_key")
        }
)
public class SessionRecurrenceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Campaign is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, updatable = false)
    private VaccinationCampaign campaign;

    @NotNull(message = "Center is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false, updatable = false)
    private VaccinationCenter center;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "session_recurrence_rule_weekdays",
            joinColumns = @JoinColumn(name = "recurrence_rule_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_recurrence_rule_weekday",
                    columnNames = {"recurrence_rule_id", "day_of_week"})
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 16)
    private Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);

    @NotNull(message = "Recurrence start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "Recurrence end date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Recurrence start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "Recurrence end time is required")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @NotNull(message = "Recurrence timezone is required")
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @NotNull(message = "Recurrence status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_status", nullable = false, length = 16)
    private SessionRecurrenceRuleStatus status = SessionRecurrenceRuleStatus.ACTIVE;

    @NotNull(message = "Recurrence generation key is required")
    @Column(name = "generation_key", nullable = false, length = 36, updatable = false)
    private String generationKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SessionRecurrenceRule() {
    }

    public SessionRecurrenceRule(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            Set<DayOfWeek> weekdays,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String timezone
    ) {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign is required");
        }
        if (center == null) {
            throw new IllegalArgumentException("Center is required");
        }
        Set<DayOfWeek> candidateWeekdays = copyAndValidateDefinition(
                weekdays, startDate, endDate, startTime, endTime, timezone);
        this.campaign = campaign;
        this.center = center;
        this.weekdays = candidateWeekdays;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timezone = normalizeTimezone(timezone);
        this.generationKey = UUID.randomUUID().toString();
    }

    public static void validateDefinition(
            Set<DayOfWeek> weekdays,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String timezone
    ) {
        copyAndValidateDefinition(weekdays, startDate, endDate, startTime, endTime, timezone);
    }

    public void updateDefinition(
            Set<DayOfWeek> weekdays,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String timezone
    ) {
        requireActive();
        Set<DayOfWeek> candidateWeekdays = copyAndValidateDefinition(
                weekdays, startDate, endDate, startTime, endTime, timezone);
        String candidateTimezone = normalizeTimezone(timezone);
        this.weekdays = candidateWeekdays;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timezone = candidateTimezone;
    }

    public void shortenEndDate(LocalDate newEndDate) {
        requireActive();
        if (newEndDate == null) {
            throw new IllegalArgumentException("Recurrence end date is required");
        }
        if (!newEndDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Recurrence end date must shorten the current range");
        }
        copyAndValidateDefinition(weekdays, startDate, newEndDate, startTime, endTime, timezone);
        endDate = newEndDate;
    }

    public void cancel() {
        requireActive();
        status = SessionRecurrenceRuleStatus.CANCELLED;
    }

    public boolean occursOn(LocalDate date) {
        return date != null
                && status == SessionRecurrenceRuleStatus.ACTIVE
                && !date.isBefore(startDate)
                && !date.isAfter(endDate)
                && weekdays.contains(date.getDayOfWeek());
    }

    private static Set<DayOfWeek> copyAndValidateDefinition(
            Set<DayOfWeek> weekdays,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String timezone
    ) {
        if (weekdays == null) {
            throw new IllegalArgumentException("Recurrence weekdays are required");
        }
        if (weekdays.isEmpty()) {
            throw new IllegalArgumentException("At least one recurrence weekday is required");
        }
        if (weekdays.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Recurrence weekdays cannot contain null");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Recurrence start date is required");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("Recurrence end date is required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Recurrence end date cannot be before start date");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("Recurrence start time is required");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("Recurrence end time is required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Recurrence start time must be before end time");
        }
        normalizeTimezone(timezone);

        EnumSet<DayOfWeek> copy = EnumSet.copyOf(weekdays);
        boolean hasOccurrence = copy.stream()
                .map(day -> startDate.with(TemporalAdjusters.nextOrSame(day)))
                .anyMatch(date -> !date.isAfter(endDate));
        if (!hasOccurrence) {
            throw new IllegalArgumentException("Recurrence range must contain a selected weekday");
        }
        return copy;
    }

    private static String normalizeTimezone(String timezone) {
        if (timezone == null) {
            throw new IllegalArgumentException("Recurrence timezone is required");
        }
        String candidate = timezone.trim();
        if (candidate.isEmpty()) {
            throw new IllegalArgumentException("Recurrence timezone is required");
        }
        try {
            return ZoneId.of(candidate).getId();
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Recurrence timezone is invalid: " + candidate, error);
        }
    }

    private void requireActive() {
        if (status != SessionRecurrenceRuleStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active recurrence rule can be changed");
        }
    }

    private void validateConstraints() {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign is required");
        }
        if (center == null) {
            throw new IllegalArgumentException("Center is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Recurrence status is required");
        }
        validateGenerationKey();
        copyAndValidateDefinition(weekdays, startDate, endDate, startTime, endTime, timezone);
    }

    private void validateGenerationKey() {
        if (generationKey == null || generationKey.length() != 36) {
            throw new IllegalArgumentException("Recurrence generation key is invalid");
        }
        try {
            if (!UUID.fromString(generationKey).toString().equals(generationKey)) {
                throw new IllegalArgumentException("Recurrence generation key is invalid");
            }
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Recurrence generation key is invalid", error);
        }
    }

    @PrePersist
    protected void onCreate() {
        validateConstraints();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        validateConstraints();
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public VaccinationCampaign getCampaign() {
        return campaign;
    }

    public VaccinationCenter getCenter() {
        return center;
    }

    public Set<DayOfWeek> getWeekdays() {
        return Collections.unmodifiableSet(EnumSet.copyOf(weekdays));
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public SessionRecurrenceRuleStatus getStatus() {
        return status;
    }

    public String getGenerationKey() {
        return generationKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
