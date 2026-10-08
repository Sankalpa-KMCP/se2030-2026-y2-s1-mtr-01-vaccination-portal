package com.se2030.vaccinationportal.campaign.center.query;

import com.se2030.vaccinationportal.campaign.api.staff.StaffContextQuery;
import com.se2030.vaccinationportal.campaign.api.staff.StaffProfileSummary;
import com.se2030.vaccinationportal.campaign.api.staff.StaffRosterSummary;
import com.se2030.vaccinationportal.campaign.api.staff.StaffScheduleSummary;


import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.StaffAssignmentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adapts roster and session assignments into the signed-in staff member's profile and schedule. */
@Component
@Transactional(readOnly = true)
public class JpaStaffContextQuery implements StaffContextQuery {
    private static final int MAX_ROSTER = 10;
    private static final int MAX_SCHEDULE = 10;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Colombo");
    private final OperationalStaffRosterRepository rosters;
    private final StaffAssignmentRepository assignments;

    public JpaStaffContextQuery(OperationalStaffRosterRepository rosters, StaffAssignmentRepository assignments) {
        this.rosters = rosters;
        this.assignments = assignments;
    }

    @Override
    public StaffProfileSummary profileForUser(UserPrincipal principal) {
        Long userId = requiredUserId(principal);
        var owned = rosters.findByUserIdOrderByCenterIdAsc(userId);
        return new StaffProfileSummary(owned.size(), owned.stream().limit(MAX_ROSTER)
                .map(r -> new StaffRosterSummary(r.getCenter().getCode(), r.getCenter().getName(),
                        String.valueOf(r.getRole()), String.valueOf(r.getStatus()))).toList());
    }

    @Override
    public List<StaffScheduleSummary> scheduleForUser(UserPrincipal principal, Instant now, boolean todayOnly) {
        Long userId = requiredUserId(principal);
        return assignments.findByUserIdOrderByStartAtAscIdAsc(userId).stream()
                .filter(a -> active(a, now, todayOnly))
                .sorted(Comparator.comparing(StaffAssignment::getStartAt))
                .limit(MAX_SCHEDULE).map(a -> {
                    VaccinationSession s = a.getSession();
                    return new StaffScheduleSummary(s.getId(), s.getCampaign().getCode(), s.getCampaign().getName(),
                            s.getCenter().getCode(), s.getCenter().getName(), String.valueOf(a.getRole()),
                            String.valueOf(s.getLifecycleStatus()), a.getStartAt(), a.getEndAt(), zone(s).getId());
                }).toList();
    }

    @Override
    public boolean isActiveAtCenter(UserPrincipal principal, Long centerId) {
        if (centerId == null) return false;
        Long userId = requiredUserId(principal);
        return rosters.findByCenterIdAndUserId(centerId, userId)
                .map(roster -> roster.isActive())
                .orElse(false);
    }

    @Override
    public boolean isAssignedToSession(UserPrincipal principal, Long sessionId, Instant now) {
        if (sessionId == null || now == null) return false;
        Long userId = requiredUserId(principal);
        return assignments.findByUserIdOrderByStartAtAscIdAsc(userId).stream()
                .anyMatch(a -> a.getSession() != null
                        && sessionId.equals(a.getSession().getId())
                        && a.getRole() == StaffRole.VACCINATOR
                        && a.getStaffRoster() != null
                        && a.getStaffRoster().getStatus() == OperationalStaffStatus.ACTIVE
                        && a.getSession().getLifecycleStatus() != SessionLifecycleStatus.CANCELLED
                        && a.getSession().getLifecycleStatus() != SessionLifecycleStatus.COMPLETED
                        && a.getEndAt() != null
                        && a.getEndAt().isAfter(now));
    }

    private static boolean active(StaffAssignment a, Instant now, boolean todayOnly) {
        if (a == null || a.getSession() == null || a.getStartAt() == null || a.getEndAt() == null) return false;
        SessionLifecycleStatus status = a.getSession().getLifecycleStatus();
        if (status == SessionLifecycleStatus.CANCELLED || status == SessionLifecycleStatus.COMPLETED) return false;
        if (!todayOnly) return a.getEndAt().isAfter(now);
        ZoneId zone = zone(a.getSession());
        LocalDate today = LocalDate.ofInstant(now, zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant nextStart = today.plusDays(1).atStartOfDay(zone).toInstant();
        return a.getStartAt().isBefore(nextStart) && a.getEndAt().isAfter(start);
    }

    private static ZoneId zone(VaccinationSession session) {
        if (session == null || session.getScheduleTimezone() == null || session.getScheduleTimezone().isBlank())
            return DEFAULT_ZONE;
        try { return ZoneId.of(session.getScheduleTimezone()); }
        catch (RuntimeException ignored) { return DEFAULT_ZONE; }
    }

    private static Long requiredUserId(UserPrincipal principal) {
        if (principal == null) throw new IllegalArgumentException("Authenticated staff principal is required");
        return principal.getUserId();
    }
}
