package com.se2030.vaccinationportal.campaign.core.validation;

import java.time.Instant;
import java.time.LocalDate;

/** Date fields shared by campaign create and update requests. */
public interface CampaignDates {
    LocalDate serviceStartDate();
    LocalDate serviceEndDate();
    Instant bookingOpenAt();
    Instant bookingCloseAt();
}
