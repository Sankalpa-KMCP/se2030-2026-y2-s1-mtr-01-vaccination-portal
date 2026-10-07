package com.se2030.vaccinationportal.campaign.session.planning.service;

import java.time.Duration;
import java.time.Instant;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
 * Pure immutable value object representing a time interval for concurrent occupancy evaluation.
 * Half-open interval semantics {@code [startAt, max(endAt, startAt + expectedDwellMinutes))}.
 * Canonical Section 6.4 and BR-043.
 */
public record OccupancyInterval(
        Instant startAt,
        Instant endAt,
        int capacity,
        int expectedDwellMinutes,
        double accompanyingRatio
) {

    public OccupancyInterval {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("Start and end times are required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Interval start must be strictly before end");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must be non-negative");
        }
        if (expectedDwellMinutes < 0) {
            throw new IllegalArgumentException("Expected dwell minutes must be non-negative");
        }
        if (accompanyingRatio < 0.0 || accompanyingRatio > 1.0) {
            throw new IllegalArgumentException("Accompanying ratio must be between 0.0 and 1.0");
        }
    }

    public Instant getEffectiveDwellEnd() {
        Instant dwellEnd = startAt.plus(Duration.ofMinutes(expectedDwellMinutes));
        return dwellEnd.isAfter(endAt) ? dwellEnd : endAt;
    }

    public double getExpectedOccupancyUnits() {
        return capacity * (1.0 + accompanyingRatio);
    }
}
