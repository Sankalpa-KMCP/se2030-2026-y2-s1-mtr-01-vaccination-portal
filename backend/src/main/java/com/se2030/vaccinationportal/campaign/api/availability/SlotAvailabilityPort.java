package com.se2030.vaccinationportal.campaign.api.availability;

import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;

import java.time.LocalDate;
import java.util.List;

/** Provides available session slots to other modules without reserving capacity. */
public interface SlotAvailabilityPort {

    /** Finds bookable slots for sessions starting in an inclusive date range, optionally by center. */
    List<AvailableSessionSummary> findAvailableSessions(
            Long campaignId,
            Long centerId,
            LocalDate fromDate,
            LocalDate toDate
    );
}
