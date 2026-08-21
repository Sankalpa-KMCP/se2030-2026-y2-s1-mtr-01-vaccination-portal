package com.se2030.vaccinationportal.campaign.dto.response;

import java.time.Instant;

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
