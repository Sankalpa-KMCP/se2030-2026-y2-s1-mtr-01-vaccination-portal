package com.se2030.vaccinationportal.campaign.session.planning.service;

import java.time.Duration;
import java.time.Instant;

/** Represents occupancy from slot start until the later of slot end or expected dwell end. */
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
