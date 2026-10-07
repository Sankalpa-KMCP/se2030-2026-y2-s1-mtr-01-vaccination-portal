package com.se2030.vaccinationportal.screening.api.query;

import java.util.List;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;

/** Latest recorded assessments, scoped to the verified citizen principal. */
public interface CitizenEligibilityQuery {
    List<EligibilitySummary> latestForCitizen(UserPrincipal principal);
}
