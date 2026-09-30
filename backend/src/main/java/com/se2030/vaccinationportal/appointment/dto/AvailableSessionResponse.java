package com.se2030.vaccinationportal.appointment.dto;

import com.se2030.vaccinationportal.campaign.api.availability.AvailableSessionSummary;
import java.time.Instant;

/**
 * REST response DTO for bookable session/slot availability search.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Search available vaccination sessions and slots.</li>
 * </ul>
 */
public record AvailableSessionResponse(
        Long sessionId,
        Long slotId,
        Long campaignId,
        Long centerId,
        String centerName,
        String centerAddress,
        Instant startAt,
        Instant endAt,
        Instant bookingCloseAt,
        int remainingCapacity
) {
    public static AvailableSessionResponse fromSummary(AvailableSessionSummary summary) {
        if (summary == null) {
            return null;
        }
        return new AvailableSessionResponse(
                summary.sessionId(),
                summary.slotId(),
                summary.campaignId(),
                summary.centerId(),
                summary.centerName(),
                summary.centerAddress(),
                summary.startAt(),
                summary.endAt(),
                summary.bookingCloseAt(),
                summary.remainingCapacity()
        );
    }
}
