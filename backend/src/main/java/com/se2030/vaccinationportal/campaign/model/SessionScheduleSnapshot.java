package com.se2030.vaccinationportal.campaign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Immutable snapshot capturing session schedule, center routing, and lifecycle state
 * at the time of a material change event.
 */
@Embeddable
public class SessionScheduleSnapshot {

    public static final int MAX_TIMEZONE_LENGTH = 64;

    @NotNull(message = "Center ID is required")
    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @NotNull(message = "Schedule timezone is required")
    @Column(name = "schedule_timezone", nullable = false, length = MAX_TIMEZONE_LENGTH)
    private String scheduleTimezone;

    @NotNull(message = "Session start is required")
    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @NotNull(message = "Session end is required")
    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @NotNull(message = "Booking close time is required")
    @Column(name = "booking_close_at", nullable = false)
    private Instant bookingCloseAt;

    @NotNull(message = "Lifecycle status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 16)
    private SessionLifecycleStatus lifecycleStatus;

    public SessionScheduleSnapshot() {
    }

    public SessionScheduleSnapshot(
            Long centerId,
            String scheduleTimezone,
            Instant startAt,
            Instant endAt,
            Instant bookingCloseAt,
            SessionLifecycleStatus lifecycleStatus
    ) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        if (lifecycleStatus == null) {
            throw new IllegalArgumentException("Lifecycle status is required");
        }
        validateSchedule(startAt, endAt, bookingCloseAt, scheduleTimezone);

        this.centerId = centerId;
        this.scheduleTimezone = normalizeTimezone(scheduleTimezone);
        this.startAt = startAt;
        this.endAt = endAt;
        this.bookingCloseAt = bookingCloseAt;
        this.lifecycleStatus = lifecycleStatus;
    }

    /**
     * Creates an immutable snapshot from an existing {@link VaccinationSession}.
     *
     * @param session the session to capture
     * @return an immutable {@link SessionScheduleSnapshot}
     */
    public static SessionScheduleSnapshot from(VaccinationSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Vaccination session is required");
        }
        if (session.getCenter() == null || session.getCenter().getId() == null) {
            throw new IllegalArgumentException("Session must have an associated center with an ID");
        }
        return new SessionScheduleSnapshot(
                session.getCenter().getId(),
                session.getScheduleTimezone(),
                session.getStartAt(),
                session.getEndAt(),
                session.getBookingCloseAt(),
                session.getLifecycleStatus()
        );
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

    public Long getCenterId() {
        return centerId;
    }

    public String getScheduleTimezone() {
        return scheduleTimezone;
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

    public SessionLifecycleStatus getLifecycleStatus() {
        return lifecycleStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SessionScheduleSnapshot that = (SessionScheduleSnapshot) o;
        return Objects.equals(centerId, that.centerId)
                && Objects.equals(scheduleTimezone, that.scheduleTimezone)
                && Objects.equals(startAt, that.startAt)
                && Objects.equals(endAt, that.endAt)
                && Objects.equals(bookingCloseAt, that.bookingCloseAt)
                && lifecycleStatus == that.lifecycleStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(centerId, scheduleTimezone, startAt, endAt, bookingCloseAt, lifecycleStatus);
    }

    @Override
    public String toString() {
        return "SessionScheduleSnapshot{" +
                "centerId=" + centerId +
                ", scheduleTimezone='" + scheduleTimezone + '\'' +
                ", startAt=" + startAt +
                ", endAt=" + endAt +
                ", bookingCloseAt=" + bookingCloseAt +
                ", lifecycleStatus=" + lifecycleStatus +
                '}';
    }
}
