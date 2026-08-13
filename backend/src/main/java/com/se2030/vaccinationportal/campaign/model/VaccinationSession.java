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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Concrete scheduled vaccination session hosted at a vaccination center for a campaign.
 * Represents an operational interval during which vaccination appointments may be conducted.
 */
@Entity
@Table(
        name = "vaccination_sessions",
        indexes = {
                @Index(name = "idx_session_campaign_schedule",
                        columnList = "campaign_id, start_at, end_at, lifecycle_status"),
                @Index(name = "idx_session_center_schedule",
                        columnList = "center_id, start_at, end_at, lifecycle_status"),
                @Index(name = "idx_session_recurrence_origin",
                        columnList = "recurrence_rule_id, origin_occurrence_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_session_occurrence_key", columnNames = "occurrence_key")
        }
)
public class VaccinationSession {

    public static final int MAX_OCCURRENCE_KEY_LENGTH = 64;
    public static final int MAX_TIMEZONE_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Campaign is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, updatable = false)
    private VaccinationCampaign campaign;

    @NotNull(message = "Center is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private VaccinationCenter center;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_rule_id", updatable = false)
    private SessionRecurrenceRule recurrenceRule;

    @Column(name = "occurrence_key", length = MAX_OCCURRENCE_KEY_LENGTH, updatable = false)
    private String occurrenceKey;

    @Column(name = "origin_occurrence_date", updatable = false)
    private LocalDate originOccurrenceDate;

    @NotNull(message = "Session start is required")
    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @NotNull(message = "Session end is required")
    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @NotNull(message = "Booking close time is required")
    @Column(name = "booking_close_at", nullable = false)
    private Instant bookingCloseAt;

    @NotNull(message = "Schedule timezone is required")
    @Column(name = "schedule_timezone", nullable = false, length = MAX_TIMEZONE_LENGTH)
    private String scheduleTimezone;

    @NotNull(message = "Lifecycle status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 16)
    private SessionLifecycleStatus lifecycleStatus = SessionLifecycleStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public VaccinationSession() {
    }

    public VaccinationSession(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            Instant startAt,
            Instant endAt,
            Instant bookingCloseAt,
            String scheduleTimezone
    ) {
        this(campaign, center, null, null, null, startAt, endAt, bookingCloseAt, scheduleTimezone);
    }

    public VaccinationSession(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            SessionRecurrenceRule recurrenceRule,
            String occurrenceKey,
            LocalDate originOccurrenceDate,
            Instant startAt,
            Instant endAt,
            Instant bookingCloseAt,
            String scheduleTimezone
    ) {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign is required");
        }
        if (center == null) {
            throw new IllegalArgumentException("Center is required");
        }
        validateSchedule(startAt, endAt, bookingCloseAt, scheduleTimezone);
        String validatedKey = validateProvenance(recurrenceRule, occurrenceKey, originOccurrenceDate);

        this.campaign = campaign;
        this.center = center;
        this.recurrenceRule = recurrenceRule;
        this.occurrenceKey = validatedKey;
        this.originOccurrenceDate = originOccurrenceDate;
        this.startAt = startAt;
        this.endAt = endAt;
        this.bookingCloseAt = bookingCloseAt;
        this.scheduleTimezone = normalizeTimezone(scheduleTimezone);
        this.lifecycleStatus = SessionLifecycleStatus.DRAFT;
    }

    public static void validateSchedule(
            Instant startAt,
            Instant endAt,
            Instant bookingCloseAt,
            String scheduleTimezone
    ) {
        if (startAt == null) {
            throw new IllegalArgumentException("Session start is required");
        }
        if (endAt == null) {
            throw new IllegalArgumentException("Session end is required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Session start must be before session end");
        }
        if (bookingCloseAt == null) {
            throw new IllegalArgumentException("Booking close time is required");
        }
        if (bookingCloseAt.isAfter(startAt)) {
            throw new IllegalArgumentException("Booking close time cannot be after session start");
        }
        normalizeTimezone(scheduleTimezone);
    }

    public static String validateProvenance(
            SessionRecurrenceRule recurrenceRule,
            String occurrenceKey,
            LocalDate originOccurrenceDate
    ) {
        boolean hasRule = recurrenceRule != null;
        boolean hasKey = occurrenceKey != null;
        boolean hasDate = originOccurrenceDate != null;

        if (!hasRule && !hasKey && !hasDate) {
            return null;
        }
        if (hasRule && hasKey && hasDate) {
            String candidateKey = occurrenceKey.trim();
            if (candidateKey.isEmpty()) {
                throw new IllegalArgumentException("Occurrence key cannot be blank");
            }
            if (candidateKey.length() > MAX_OCCURRENCE_KEY_LENGTH) {
                throw new IllegalArgumentException("Occurrence key cannot exceed 64 characters");
            }
            return candidateKey;
        }
        if (hasRule && !hasKey) {
            throw new IllegalArgumentException("Recurrence rule requires an occurrence key");
        }
        if (hasRule && !hasDate) {
            throw new IllegalArgumentException("Recurrence rule requires an origin occurrence date");
        }
        if (hasKey && !hasRule) {
            throw new IllegalArgumentException("Occurrence key requires a recurrence rule");
        }
        if (hasKey && !hasDate) {
            throw new IllegalArgumentException("Occurrence key requires an origin occurrence date");
        }
        if (hasDate && !hasRule) {
            throw new IllegalArgumentException("Origin occurrence date requires a recurrence rule");
        }
        throw new IllegalArgumentException("Origin occurrence date requires an occurrence key");
    }

    private static String normalizeTimezone(String timezone) {
        if (timezone == null) {
            throw new IllegalArgumentException("Schedule timezone is required");
        }
        String candidate = timezone.trim();
        if (candidate.isEmpty()) {
            throw new IllegalArgumentException("Schedule timezone is required");
        }
        try {
            return ZoneId.of(candidate).getId();
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Schedule timezone is invalid: " + candidate, error);
        }
    }

    private void validateConstraints() {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign is required");
        }
        if (center == null) {
            throw new IllegalArgumentException("Center is required");
        }
        if (lifecycleStatus == null) {
            throw new IllegalArgumentException("Lifecycle status is required");
        }
        validateSchedule(startAt, endAt, bookingCloseAt, scheduleTimezone);
        validateProvenance(recurrenceRule, occurrenceKey, originOccurrenceDate);
    }

    @PrePersist
    protected void onCreate() {
        validateConstraints();
        this.scheduleTimezone = normalizeTimezone(this.scheduleTimezone);
        if (this.occurrenceKey != null) {
            this.occurrenceKey = this.occurrenceKey.trim();
        }
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
        this.scheduleTimezone = normalizeTimezone(this.scheduleTimezone);
        if (this.occurrenceKey != null) {
            this.occurrenceKey = this.occurrenceKey.trim();
        }
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

    public SessionRecurrenceRule getRecurrenceRule() {
        return recurrenceRule;
    }

    public String getOccurrenceKey() {
        return occurrenceKey;
    }

    public LocalDate getOriginOccurrenceDate() {
        return originOccurrenceDate;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public Instant getBookingCloseAt() {
        return bookingCloseAt;
    }

    public String getScheduleTimezone() {
        return scheduleTimezone;
    }

    public SessionLifecycleStatus getLifecycleStatus() {
        return lifecycleStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isGenerated() {
        return recurrenceRule != null;
    }

    /**
     * Reschedules the session to a new schedule interval and/or new hosting center.
     * Preserves recurrence rule provenance, occurrence key, and origin occurrence date for generated occurrences.
     *
     * @param newCenter         Authoritative hosting center
     * @param newStartAt        New session start instant
     * @param newEndAt          New session end instant
     * @param newBookingCloseAt New booking close instant
     */
    public void reschedule(
            VaccinationCenter newCenter,
            Instant newStartAt,
            Instant newEndAt,
            Instant newBookingCloseAt
    ) {
        if (this.lifecycleStatus == SessionLifecycleStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot reschedule cancelled session");
        }
        if (this.lifecycleStatus != SessionLifecycleStatus.DRAFT && this.lifecycleStatus != SessionLifecycleStatus.SCHEDULED) {
            throw new IllegalArgumentException("Cannot reschedule session in " + this.lifecycleStatus + " lifecycle status");
        }
        if (newCenter == null) {
            throw new IllegalArgumentException("Center is required");
        }
        validateSchedule(newStartAt, newEndAt, newBookingCloseAt, newCenter.getTimezone());

        this.center = newCenter;
        this.scheduleTimezone = normalizeTimezone(newCenter.getTimezone());
        this.startAt = newStartAt;
        this.endAt = newEndAt;
        this.bookingCloseAt = newBookingCloseAt;
    }

    /**
     * Schedules a DRAFT session (transitions to SCHEDULED).
     */
    public void schedule() {
        transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
    }

    /**
     * Starts a SCHEDULED session (transitions to IN_PROGRESS).
     */
    public void start() {
        transitionLifecycleStatus(SessionLifecycleStatus.IN_PROGRESS);
    }

    /**
     * Completes an IN_PROGRESS session (transitions to COMPLETED).
     */
    public void complete() {
        transitionLifecycleStatus(SessionLifecycleStatus.COMPLETED);
    }

    /**
     * Cancels the session, setting lifecycle status to CANCELLED.
     * Preserves the session entity and recurrence provenance.
     */
    public void cancel() {
        transitionLifecycleStatus(SessionLifecycleStatus.CANCELLED);
    }

    /**
     * Updates lifecycle status for state transitions.
     * Enforces the valid session lifecycle state machine:
     * DRAFT -> SCHEDULED, DRAFT -> CANCELLED
     * SCHEDULED -> IN_PROGRESS, SCHEDULED -> CANCELLED
     * IN_PROGRESS -> COMPLETED
     * All other transitions (including terminal COMPLETED and CANCELLED) are rejected.
     *
     * @param targetStatus New lifecycle status
     */
    public void transitionLifecycleStatus(SessionLifecycleStatus targetStatus) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("Lifecycle status is required");
        }
        if (this.lifecycleStatus == targetStatus) {
            throw new IllegalStateException("Session is already in " + targetStatus + " lifecycle status");
        }
        boolean isValid = false;
        switch (this.lifecycleStatus) {
            case DRAFT:
                isValid = (targetStatus == SessionLifecycleStatus.SCHEDULED || targetStatus == SessionLifecycleStatus.CANCELLED);
                break;
            case SCHEDULED:
                isValid = (targetStatus == SessionLifecycleStatus.IN_PROGRESS || targetStatus == SessionLifecycleStatus.CANCELLED);
                break;
            case IN_PROGRESS:
                isValid = (targetStatus == SessionLifecycleStatus.COMPLETED);
                break;
            case COMPLETED:
            case CANCELLED:
                isValid = false;
                break;
        }

        if (!isValid) {
            throw new IllegalStateException(
                    "Invalid session lifecycle transition from " + this.lifecycleStatus + " to " + targetStatus
            );
        }

        this.lifecycleStatus = targetStatus;
    }
}
