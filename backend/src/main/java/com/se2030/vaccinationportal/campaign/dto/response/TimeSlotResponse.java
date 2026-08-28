package com.se2030.vaccinationportal.campaign.dto.response;

import java.time.Instant;

/**
 * Response payload representing an operational time slot with capacity counters.
 */
public record TimeSlotResponse(
        Long id,
        Long sessionId,
        Instant startTime,
        Instant endTime,
        int capacity,
        int reservedCount,
        int availableCapacity,
        Long lockVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
