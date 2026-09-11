package com.se2030.vaccinationportal.campaign.service;

import java.time.Instant;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
 * Pure immutable result of concurrent occupancy evaluation across session/slot intervals.
 * Canonical Section 6.4 and MF02-BR-22.
 */
public record OccupancyEvaluationResult(
        boolean safe,
        double peakOccupancyUnits,
        int safeConcurrentOccupancy,
        Instant peakIntervalStart
) {

    public static OccupancyEvaluationResult safe(double peakOccupancyUnits, int safeConcurrentOccupancy, Instant peakIntervalStart) {
        return new OccupancyEvaluationResult(true, peakOccupancyUnits, safeConcurrentOccupancy, peakIntervalStart);
    }

    public static OccupancyEvaluationResult exceeded(double peakOccupancyUnits, int safeConcurrentOccupancy, Instant peakIntervalStart) {
        return new OccupancyEvaluationResult(false, peakOccupancyUnits, safeConcurrentOccupancy, peakIntervalStart);
    }
}
