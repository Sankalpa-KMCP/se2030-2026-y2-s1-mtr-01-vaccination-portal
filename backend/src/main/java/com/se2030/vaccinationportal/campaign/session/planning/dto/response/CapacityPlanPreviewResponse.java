package com.se2030.vaccinationportal.campaign.session.planning.dto.response;

import java.time.Instant;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 */
public record CapacityPlanPreviewResponse(
        Long sessionId,
        int durationMinutes,
        int expectedServiceMinutes,
        int assignedVaccinatorCount,
        int availableStationCount,
        int fullyEquippedStationCount,
        int effectiveStationCount,
        int recommendedThroughput,
        Integer projectedApprovedCapacity,
        int safeConcurrentOccupancy,
        int expectedDwellMinutes,
        double expectedAccompanyingPersonsPerBooking,
        Integer sessionServiceCeiling,
        boolean overrideRequired,
        boolean overrideValid,
        Instant calculatedAt
) {}
