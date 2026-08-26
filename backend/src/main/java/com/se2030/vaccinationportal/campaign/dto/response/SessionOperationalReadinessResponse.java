package com.se2030.vaccinationportal.campaign.dto.response;

import java.util.List;

/**
 * REST response DTO representing authoritative operational readiness diagnostics for a vaccination session.
 */
public record SessionOperationalReadinessResponse(
        Long sessionId,
        boolean ready,
        List<String> violations,
        Integer assignedVaccinators,
        Integer availableStations,
        Integer effectiveStations,
        Integer recommendedThroughput,
        Integer approvedCapacity,
        Integer revision
) {
}
