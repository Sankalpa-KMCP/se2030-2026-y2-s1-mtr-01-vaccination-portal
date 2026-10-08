package com.se2030.vaccinationportal.campaign.api.staff;

import java.time.Instant;
import java.util.List;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;

/** Roster and schedule facts for the verified staff principal. */
public interface StaffContextQuery {
    StaffProfileSummary profileForUser(UserPrincipal principal);
    List<StaffScheduleSummary> scheduleForUser(UserPrincipal principal, Instant now, boolean todayOnly);
    boolean isActiveAtCenter(UserPrincipal principal, Long centerId);
    boolean isAssignedToSession(UserPrincipal principal, Long sessionId, Instant now);
}
