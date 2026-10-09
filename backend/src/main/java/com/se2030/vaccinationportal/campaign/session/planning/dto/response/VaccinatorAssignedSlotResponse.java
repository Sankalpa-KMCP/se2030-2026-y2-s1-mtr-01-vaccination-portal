package com.se2030.vaccinationportal.campaign.session.planning.dto.response;

import java.time.Instant;

/** Read-only slot details exposed to the assigned vaccinator workspace. */
public record VaccinatorAssignedSlotResponse(
        Long id,
        Instant startTime,
        Instant endTime
) {
}
