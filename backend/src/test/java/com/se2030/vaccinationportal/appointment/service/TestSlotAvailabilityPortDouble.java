package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.campaign.api.availability.AvailableSessionSummary;
import com.se2030.vaccinationportal.campaign.api.availability.SlotAvailabilityPort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Test double for {@link SlotAvailabilityPort} used in unit and integration testing.
 */
public class TestSlotAvailabilityPortDouble implements SlotAvailabilityPort {

    private final List<AvailableSessionSummary> availableSessions = new ArrayList<>();

    public void addSession(AvailableSessionSummary summary) {
        availableSessions.add(summary);
    }

    public void clear() {
        availableSessions.clear();
    }

    @Override
    public List<AvailableSessionSummary> findAvailableSessions(
            Long campaignId,
            Long centerId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return availableSessions.stream()
                .filter(s -> campaignId == null || campaignId.equals(s.campaignId()))
                .filter(s -> centerId == null || centerId.equals(s.centerId()))
                .toList();
    }
}
