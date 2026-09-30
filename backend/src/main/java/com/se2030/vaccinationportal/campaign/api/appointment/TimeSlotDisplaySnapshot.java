package com.se2030.vaccinationportal.campaign.api.appointment;

import java.time.Instant;

public record TimeSlotDisplaySnapshot(Long id, Instant startTime, Instant endTime,
        String campaignName, String centerName, String centerAddress) {
}
