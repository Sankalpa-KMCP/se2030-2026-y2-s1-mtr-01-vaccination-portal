package com.se2030.vaccinationportal.appointment.dto;

import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import java.time.Instant;

/**
 * Response DTO representing a WaitlistEntry entity.
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
