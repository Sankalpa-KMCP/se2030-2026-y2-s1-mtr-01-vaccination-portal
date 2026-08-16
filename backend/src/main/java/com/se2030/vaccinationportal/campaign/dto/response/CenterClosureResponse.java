package com.se2030.vaccinationportal.campaign.dto.response;

import com.se2030.vaccinationportal.campaign.model.CenterClosureStatus;
import java.time.Instant;

/**
 * Detailed view of a CenterClosure record.
 */
public record CenterClosureResponse(
        Long id,
        Long centerId,
        Instant startAt,
        Instant endAt,
        String reason,
        CenterClosureStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
