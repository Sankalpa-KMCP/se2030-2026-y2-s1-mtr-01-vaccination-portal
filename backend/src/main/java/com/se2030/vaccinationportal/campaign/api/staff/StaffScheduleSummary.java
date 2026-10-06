package com.se2030.vaccinationportal.campaign.api.staff;

import java.time.Instant;

public record StaffScheduleSummary(Long sessionId, String campaignCode, String campaignName,
        String centerCode, String centerName, String role, String sessionStatus,
        Instant startAt, Instant endAt, String timezone) {
}
