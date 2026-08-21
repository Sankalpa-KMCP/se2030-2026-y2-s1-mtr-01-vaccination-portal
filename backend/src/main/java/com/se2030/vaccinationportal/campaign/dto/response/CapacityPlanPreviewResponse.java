package com.se2030.vaccinationportal.campaign.dto.response;

import java.time.Instant;

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
