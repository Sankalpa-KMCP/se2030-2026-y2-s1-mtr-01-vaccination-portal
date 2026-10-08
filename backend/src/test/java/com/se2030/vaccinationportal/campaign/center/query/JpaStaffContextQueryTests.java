package com.se2030.vaccinationportal.campaign.center.query;

import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalStaffRosterRepository;

import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaStaffContextQueryTests {
    private static final Instant NOW = Instant.parse("2026-09-27T03:30:00Z");
    private final StaffAssignmentRepository assignmentRepository = mock(StaffAssignmentRepository.class);
    private final OperationalStaffRosterRepository rosterRepository = mock(OperationalStaffRosterRepository.class);
    private final JpaStaffContextQuery query = new JpaStaffContextQuery(rosterRepository, assignmentRepository);

    @Test
    void activeCenterAccessUsesAuthenticatedUsersRosterStatus() {
        UserPrincipal principal = principal(77L, "ROLE_CENTER_MANAGER");
        OperationalStaffRoster active = mock(OperationalStaffRoster.class);
        when(active.isActive()).thenReturn(true);
        when(rosterRepository.findByCenterIdAndUserId(501L, 77L)).thenReturn(Optional.of(active));

        assertThat(query.isActiveAtCenter(principal, 501L)).isTrue();
        assertThat(query.isActiveAtCenter(principal, 999L)).isFalse();
        verify(rosterRepository).findByCenterIdAndUserId(501L, 77L);
        verify(rosterRepository).findByCenterIdAndUserId(999L, 77L);
    }
    @Test
    @DisplayName("returns current and upcoming assignments for only the verified staff user")
    void returnsVerifiedUsersUpcomingAssignments() {
        StaffAssignment upcoming = assignment(
                501L,
                NOW.plusSeconds(3600),
                NOW.plusSeconds(10800),
                SessionLifecycleStatus.SCHEDULED
        );
        StaffAssignment finished = assignment(
                502L,
                NOW.minusSeconds(10800),
                NOW.minusSeconds(3600),
                SessionLifecycleStatus.COMPLETED
        );
        StaffAssignment cancelled = assignment(
                503L,
                NOW.plusSeconds(14400),
                NOW.plusSeconds(18000),
                SessionLifecycleStatus.CANCELLED
        );
        when(assignmentRepository.findByUserIdOrderByStartAtAscIdAsc(77L))
                .thenReturn(List.of(finished, cancelled, upcoming));

        var result = query.scheduleForUser(principal(77L, "ROLE_CENTER_MANAGER"), NOW, false);
        verify(assignmentRepository).findByUserIdOrderByStartAtAscIdAsc(77L);
        verify(assignmentRepository, never()).findByUserIdOrderByStartAtAscIdAsc(999L);
        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.sessionId()).isEqualTo(501L);
            assertThat(summary.campaignName()).isEqualTo("Campaign A");
            assertThat(summary.centerName()).isEqualTo("Central Clinic");
            assertThat(summary.role()).isEqualTo("CHECK_IN_STAFF");
            assertThat(summary.timezone()).isEqualTo("Asia/Colombo");
        });
        assertThat(result).extracting(summary -> summary.sessionId()).doesNotContain(502L, 503L);
    }
    @Test
    @DisplayName("today queries include assignments overlapping the center-local day")
    void filtersTodayUsingSessionTimezone() {
        StaffAssignment today = assignment(
                601L,
                Instant.parse("2026-09-27T02:00:00Z"),
                Instant.parse("2026-09-27T05:00:00Z"),
                SessionLifecycleStatus.SCHEDULED
        );
        StaffAssignment tomorrow = assignment(
                602L,
                Instant.parse("2026-09-28T02:00:00Z"),
                Instant.parse("2026-09-28T05:00:00Z"),
                SessionLifecycleStatus.SCHEDULED
        );
        when(assignmentRepository.findByUserIdOrderByStartAtAscIdAsc(88L))
                .thenReturn(List.of(today, tomorrow));

        var result = query.scheduleForUser(principal(88L, "ROLE_CENTER_MANAGER"), NOW, true);
        verify(assignmentRepository).findByUserIdOrderByStartAtAscIdAsc(88L);
        verify(assignmentRepository, never()).findByUserIdOrderByStartAtAscIdAsc(999L);
        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.sessionId()).isEqualTo(601L);
            assertThat(summary.campaignName()).isEqualTo("Campaign A");
            assertThat(summary.centerName()).isEqualTo("Central Clinic");
            assertThat(summary.role()).isEqualTo("CHECK_IN_STAFF");
            assertThat(summary.timezone()).isEqualTo("Asia/Colombo");
        });
        assertThat(result).extracting(summary -> summary.sessionId()).doesNotContain(602L);
    }
    @Test
    @DisplayName("excludes COMPLETED session with future scheduled end time from current and upcoming schedule")
    void excludesCompletedSessionWithFutureEndTime() {
        StaffAssignment completedFutureEnd = assignment(
                701L,
                NOW.minusSeconds(1800),
                NOW.plusSeconds(3600), // end time is in the future!
                SessionLifecycleStatus.COMPLETED
        );
        StaffAssignment scheduledUpcoming = assignment(
                702L,
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                SessionLifecycleStatus.SCHEDULED
        );
        when(assignmentRepository.findByUserIdOrderByStartAtAscIdAsc(78L))
                .thenReturn(List.of(completedFutureEnd, scheduledUpcoming));

        var result = query.scheduleForUser(principal(78L, "ROLE_CENTER_MANAGER"), NOW, false);
        verify(assignmentRepository).findByUserIdOrderByStartAtAscIdAsc(78L);
        verify(assignmentRepository, never()).findByUserIdOrderByStartAtAscIdAsc(999L);
        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.sessionId()).isEqualTo(702L);
            assertThat(summary.campaignName()).isEqualTo("Campaign A");
            assertThat(summary.centerName()).isEqualTo("Central Clinic");
            assertThat(summary.role()).isEqualTo("CHECK_IN_STAFF");
            assertThat(summary.timezone()).isEqualTo("Asia/Colombo");
        });
        assertThat(result).extracting(summary -> summary.sessionId()).doesNotContain(701L);
    }
    @Test
    @DisplayName("excludes COMPLETED session during today's date from today's guidance")
    void excludesCompletedSessionDuringToday() {
        StaffAssignment completedToday = assignment(
                801L,
                Instant.parse("2026-09-27T01:00:00Z"),
                Instant.parse("2026-09-27T04:00:00Z"), // overlaps today and now (NOW = 03:30:00Z)
                SessionLifecycleStatus.COMPLETED
        );
        StaffAssignment activeToday = assignment(
                802L,
                Instant.parse("2026-09-27T02:00:00Z"),
                Instant.parse("2026-09-27T06:00:00Z"), // active now
                SessionLifecycleStatus.IN_PROGRESS
        );
        when(assignmentRepository.findByUserIdOrderByStartAtAscIdAsc(88L))
                .thenReturn(List.of(completedToday, activeToday));

        var result = query.scheduleForUser(principal(88L, "ROLE_CENTER_MANAGER"), NOW, true);
        verify(assignmentRepository).findByUserIdOrderByStartAtAscIdAsc(88L);
        verify(assignmentRepository, never()).findByUserIdOrderByStartAtAscIdAsc(999L);
        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.sessionId()).isEqualTo(802L);
            assertThat(summary.campaignName()).isEqualTo("Campaign A");
            assertThat(summary.centerName()).isEqualTo("Central Clinic");
            assertThat(summary.role()).isEqualTo("CHECK_IN_STAFF");
            assertThat(summary.timezone()).isEqualTo("Asia/Colombo");
        });
        assertThat(result).extracting(summary -> summary.sessionId()).doesNotContain(801L);
    }
    @Test
    void profileUsesVerifiedStaffIdentityAndBoundsRosterFactsWithoutLosingTotalCount() {
        var center = mock(com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter.class);
        when(center.getCode()).thenReturn("CTR-001");
        when(center.getName()).thenReturn("Central Clinic");
        var roster = mock(com.se2030.vaccinationportal.campaign.center.model.OperationalStaffRoster.class);
        when(roster.getCenter()).thenReturn(center);
        when(roster.getRole()).thenReturn(com.se2030.vaccinationportal.campaign.center.model.StaffRole.SUPERVISOR);
        when(roster.getStatus()).thenReturn(com.se2030.vaccinationportal.campaign.center.model.OperationalStaffStatus.ACTIVE);
        when(rosterRepository.findByUserIdOrderByCenterIdAsc(77L))
                .thenReturn(java.util.Collections.nCopies(12, roster));

        var result = query.profileForUser(principal(77L, "ROLE_CENTER_MANAGER"));

        assertThat(result.totalAssignments()).isEqualTo(12);
        assertThat(result.roster()).hasSize(10).allSatisfy(r -> {
            assertThat(r.centerCode()).isEqualTo("CTR-001");
            assertThat(r.centerName()).isEqualTo("Central Clinic");
            assertThat(r.role()).isEqualTo("SUPERVISOR");
            assertThat(r.status()).isEqualTo("ACTIVE");
        });
        verify(rosterRepository).findByUserIdOrderByCenterIdAsc(77L);
        verify(rosterRepository, never()).findByUserIdOrderByCenterIdAsc(999L);
    }

    private StaffAssignment assignment(
            Long sessionId,
            Instant start,
            Instant end,
            SessionLifecycleStatus lifecycleStatus
    ) {
        VaccinationCampaign campaign = mock(VaccinationCampaign.class);
        when(campaign.getCode()).thenReturn("CMP-A");
        when(campaign.getName()).thenReturn("Campaign A");

        VaccinationCenter center = mock(VaccinationCenter.class);
        when(center.getCode()).thenReturn("CTR-001");
        when(center.getName()).thenReturn("Central Clinic");

        VaccinationSession session = mock(VaccinationSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.getCampaign()).thenReturn(campaign);
        when(session.getCenter()).thenReturn(center);
        when(session.getScheduleTimezone()).thenReturn("Asia/Colombo");
        when(session.getLifecycleStatus()).thenReturn(lifecycleStatus);

        StaffAssignment assignment = mock(StaffAssignment.class);
        when(assignment.getSession()).thenReturn(session);
        when(assignment.getRole()).thenReturn(StaffRole.CHECK_IN_STAFF);
        when(assignment.getStartAt()).thenReturn(start);
        when(assignment.getEndAt()).thenReturn(end);
        return assignment;
    }

    private UserPrincipal principal(Long userId, String role) {
        return new UserPrincipal(
                userId,
                "user" + userId + "@example.com",
                "pass",
                true,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
