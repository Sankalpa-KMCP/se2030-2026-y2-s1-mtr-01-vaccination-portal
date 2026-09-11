package com.se2030.vaccinationportal.campaign.dto.response;

import java.time.Instant;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 */
public record SessionCapacityPlanResponse(
        Long id,
        Long sessionId,
        int revision,
        int durationMinutes,
        int expectedServiceMinutes,
        int assignedVaccinatorCount,
        int availableStationCount,
        int fullyEquippedStationCount,
        int effectiveStationCount,
        int recommendedThroughput,
        int approvedCapacity,
        int safeConcurrentOccupancy,
        int expectedDwellMinutes,
        double expectedAccompanyingPersonsPerBooking,
        Integer sessionServiceCeiling,
        String overrideReason,
        String approvedBy,
        Instant createdAt,
        Instant updatedAt
) {}
