package com.se2030.vaccinationportal.appointment.api.query;

import java.util.List;

public record CitizenAppointmentOverview(List<AppointmentSummary> activeOrUpcoming,
        List<AppointmentSummary> pastOrInactive, List<AppointmentSummary> checkedIn) {
    public CitizenAppointmentOverview {
        activeOrUpcoming = List.copyOf(activeOrUpcoming);
        pastOrInactive = List.copyOf(pastOrInactive);
        checkedIn = List.copyOf(checkedIn);
    }
}
