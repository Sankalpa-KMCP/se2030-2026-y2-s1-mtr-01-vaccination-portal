package com.se2030.vaccinationportal.appointment.api.query;

import java.util.List;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;

/** Read facts scoped to the verified citizen principal. */
public interface CitizenAppointmentQuery {
    CitizenAppointmentOverview appointmentsForCitizen(UserPrincipal principal);
    List<WaitlistSummary> waitlistForCitizen(UserPrincipal principal);

    /** Classifies appointments after base reads and before waitlist queue-position counts. */
    CitizenAppointmentAndWaitlistOverview appointmentsAndWaitlistForCitizen(UserPrincipal principal);
}
