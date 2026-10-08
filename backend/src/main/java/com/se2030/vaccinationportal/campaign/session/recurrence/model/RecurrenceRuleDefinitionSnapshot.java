package com.se2030.vaccinationportal.campaign.session.recurrence.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable value representation of a recurrence rule's schedule and timing definition.
 */
public class RecurrenceRuleDefinitionSnapshot {

    private final Set<DayOfWeek> weekdays;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String timezone;

    public RecurrenceRuleDefinitionSnapshot(
            Set<DayOfWeek> weekdays,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String timezone
    ) {
        SessionRecurrenceRule.validateDefinition(weekdays, startDate, endDate, startTime, endTime, timezone);
        this.weekdays = Collections.unmodifiableSet(EnumSet.copyOf(weekdays));
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timezone = timezone.trim();
    }

    /**
     * Creates a snapshot from an existing {@link SessionRecurrenceRule}.
     *
     * @param rule Rule to capture
     * @return Immutable definition snapshot
     */
    public static RecurrenceRuleDefinitionSnapshot from(SessionRecurrenceRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Session recurrence rule is required");
        }
        return new RecurrenceRuleDefinitionSnapshot(
                rule.getWeekdays(),
                rule.getStartDate(),
                rule.getEndDate(),
                rule.getStartTime(),
                rule.getEndTime(),
                rule.getTimezone()
        );
    }

    /**
     * Checks if the definition produces an occurrence on the given date.
     *
     * @param date Date to evaluate
     * @return true if the date falls in [startDate, endDate] and matches a configured weekday
     */
    public boolean occursOn(LocalDate date) {
        return date != null
                && !date.isBefore(startDate)
                && !date.isAfter(endDate)
                && weekdays.contains(date.getDayOfWeek());
    }

    public Set<DayOfWeek> getWeekdays() {
        return weekdays;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecurrenceRuleDefinitionSnapshot that = (RecurrenceRuleDefinitionSnapshot) o;
        return Objects.equals(weekdays, that.weekdays)
                && Objects.equals(startDate, that.startDate)
                && Objects.equals(endDate, that.endDate)
                && Objects.equals(startTime, that.startTime)
                && Objects.equals(endTime, that.endTime)
                && Objects.equals(timezone, that.timezone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weekdays, startDate, endDate, startTime, endTime, timezone);
    }

    @Override
    public String toString() {
        return "RecurrenceRuleDefinitionSnapshot{" +
                "weekdays=" + weekdays +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", timezone='" + timezone + '\'' +
                '}';
    }
}
