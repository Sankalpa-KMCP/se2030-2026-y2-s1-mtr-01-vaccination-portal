package com.se2030.vaccinationportal.appointment.waitlist.dto;

import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import java.time.Instant;

/**
 * REST response DTO representing a WaitlistEntry entity and its queue position.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-05: Returned upon waitlist join, leave, and status queries with dynamic queue position.</li>
 * </ul>
 */
public record WaitlistEntryResponse(
        Long id,
        Long citizenId,
        Long campaignId,
        Long sessionId,
        WaitlistStatus status,
        Instant joinedAt,
        Long lockVersion,
        Integer position
) {
    public static WaitlistEntryResponse fromEntity(WaitlistEntry entry) {
        return fromEntity(entry, null);
    }

    public static WaitlistEntryResponse fromEntity(WaitlistEntry entry, Integer position) {
        if (entry == null) {
            return null;
        }
        return new WaitlistEntryResponse(
                entry.getId(),
                entry.getCitizenId(),
                entry.getCampaignId(),
                entry.getSessionId(),
                entry.getStatus(),
                entry.getJoinedAt(),
                entry.getLockVersion(),
                position
        );
    }
}
