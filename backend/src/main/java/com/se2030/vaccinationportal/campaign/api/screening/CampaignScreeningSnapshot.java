package com.se2030.vaccinationportal.campaign.api.screening;

import java.time.Instant;
import java.time.LocalDate;

/** Immutable intake and service-window facts from one campaign. */
public record CampaignScreeningSnapshot(
        Long id, LocalDate serviceEndDate, Instant bookingOpenAt, Instant bookingCloseAt,
        boolean intakePermitted) {
    public boolean isBookingWindowOpenAt(Instant at) {
        return at != null && intakePermitted && !at.isBefore(bookingOpenAt) && !at.isAfter(bookingCloseAt);
    }
}
