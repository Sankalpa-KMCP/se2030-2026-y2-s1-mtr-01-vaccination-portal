package com.se2030.vaccinationportal.campaign.api.session;

import java.time.Instant;
import java.util.List;

/** Immutable MF-02 session-change message exposed across the module boundary. */
public record SessionChangeNotice(
        Long eventId,
        Long sessionId,
        List<Long> slotIds,
        String changeType,
        Long oldCenterId,
        Long newCenterId,
        Instant oldStartAt,
        Instant newStartAt,
        String reason,
        String actorId,
        Instant createdAt
) {
}
