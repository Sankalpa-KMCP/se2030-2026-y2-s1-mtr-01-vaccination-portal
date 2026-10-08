package com.se2030.vaccinationportal.campaign.session.planning.dto.response;

import java.time.Instant;
import java.util.List;

/** Read-only view of a current vaccination session assigned to the authenticated vaccinator. */
public record VaccinatorAssignedSessionResponse(
        Long id,
        String campaignCode,
        String campaignName,
        String centerCode,
        String centerName,
        String status,
        Instant startAt,
        Instant endAt,
        String timezone,
        List<VaccinatorAssignedSlotResponse> slots
) {
}
