package com.se2030.vaccinationportal.appointment.api.query;

import java.time.Instant;

public record AppointmentSummary(String status, Long campaignId, Long slotId, boolean linkedSlotPresent,
        Instant scheduledStart, Instant scheduledEnd, String campaignName,
        String centerName, String centerAddress, boolean confirmedPastTime) {
}
