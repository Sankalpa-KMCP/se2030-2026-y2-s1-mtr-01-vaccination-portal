package com.se2030.vaccinationportal.appointment.api.query;

import java.util.List;

/** Bounded citizen appointment and waitlist facts from one sequenced read operation. */
public record CitizenAppointmentAndWaitlistOverview(CitizenAppointmentOverview appointments,
        List<WaitlistSummary> waitlist) {
    public CitizenAppointmentAndWaitlistOverview {
        waitlist = List.copyOf(waitlist);
    }
}
