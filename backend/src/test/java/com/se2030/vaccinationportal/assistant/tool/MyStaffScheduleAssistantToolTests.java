package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Staff schedule assistant tool")
class MyStaffScheduleAssistantToolTests {

    private static final Instant NOW = Instant.parse("2026-09-27T03:30:00Z");

    private StaffAssignmentRepository assignmentRepository;
    private MyStaffScheduleAssistantTool tool;

    @BeforeEach
    void setUp() {
        assignmentRepository = mock(StaffAssignmentRepository.class);
        tool = new MyStaffScheduleAssistantTool(
                assignmentRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
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

        AssistantToolResult result = tool.execute(
                staffContext(77L, "ROLE_CENTER_MANAGER"),
                "Show my upcoming sessions for staff ID 999"
        );

        verify(assignmentRepository).findByUserIdOrderByStartAtAscIdAsc(77L);
        verify(assignmentRepository, never()).findByUserIdOrderByStartAtAscIdAsc(999L);
        assertThat(result.resultCount()).isEqualTo(1);
        assertThat(result.contextSnippet())
                .contains("Session ID: 501")
                .contains("Campaign A")
                .contains("Central Clinic")
                .contains("CHECK_IN_STAFF")
                .contains("Asia/Colombo")
                .doesNotContain("Session ID: 502")
                .doesNotContain("Session ID: 503")
                .doesNotContain("999");
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

        AssistantToolResult result = tool.execute(
                staffContext(88L, "ROLE_HEALTHCARE_OFFICER"),
                "What should I do today?"
        );

        assertThat(result.resultCount()).isEqualTo(1);
        assertThat(result.contextSnippet())
                .startsWith("Today's verified session assignments")
                .contains("Session ID: 601")
                .doesNotContain("Session ID: 602");
    }

    @Test
    @DisplayName("returns an explicit empty schedule without changing roster meaning")
    void returnsSafeEmptySchedule() {
        when(assignmentRepository.findByUserIdOrderByStartAtAscIdAsc(89L)).thenReturn(List.of());

        AssistantToolResult result = tool.execute(
                staffContext(89L, "ROLE_VACCINATOR"),
                "Show my schedule"
        );

        assertThat(result.success()).isTrue();
        assertThat(result.resultCount()).isZero();
        assertThat(result.contextSnippet())
                .contains("No current or upcoming session assignment")
                .contains("does not change the staff member's account or center-roster status");
    }

    @Test
    @DisplayName("rejects a citizen before querying assignments")
    void rejectsCitizens() {
        UserPrincipal citizen = principal(90L, "ROLE_CITIZEN");
        AssistantToolContext context = new AssistantToolContext(
                "schedule-4", citizen, Set.of("ROLE_CITIZEN"));

        assertThatThrownBy(() -> tool.execute(context, "Show my schedule"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(assignmentRepository, never()).findByUserIdOrderByStartAtAscIdAsc(90L);
    }

    @Test
    @DisplayName("schedule intent selection remains role-aware")
    void selectsScheduleOnlyForStaff() {
        AssistantToolRegistry registry = new AssistantToolRegistry(List.of(tool));

        assertThat(registry.findMatchingTool(
                "What should I do today?",
                staffContext(91L, "ROLE_CENTER_MANAGER")
        )).isPresent();

        UserPrincipal citizen = principal(92L, "ROLE_CITIZEN");
        AssistantToolContext citizenContext = new AssistantToolContext(
                "schedule-5", citizen, Set.of("ROLE_CITIZEN"));
        assertThat(registry.findMatchingTool("What should I do today?", citizenContext)).isEmpty();
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

        AssistantToolResult result = tool.execute(
                staffContext(78L, "ROLE_CENTER_MANAGER"),
                "Show my upcoming sessions"
        );

        assertThat(result.resultCount()).isEqualTo(1);
        assertThat(result.contextSnippet())
                .contains("Session ID: 702")
                .doesNotContain("Session ID: 701");
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

        AssistantToolResult result = tool.execute(
                staffContext(88L, "ROLE_HEALTHCARE_OFFICER"),
                "What should I do today?"
        );

        assertThat(result.resultCount()).isEqualTo(1);
        assertThat(result.contextSnippet())
                .contains("Session ID: 802")
                .contains("IN_PROGRESS")
                .doesNotContain("Session ID: 801");
    }

    @Test
    @DisplayName("admin-only caller receives /app/sessions and strictly excludes /app/staff navigation")
    void adminOnlyCallerExcludesStaffRoute() {
        StaffAssignment upcoming = assignment(
                901L,
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                SessionLifecycleStatus.SCHEDULED
        );
        when(assignmentRepository.findByUserIdOrderByStartAtAscIdAsc(99L))
                .thenReturn(List.of(upcoming));

        AssistantToolResult result = tool.execute(
                staffContext(99L, "ROLE_ADMIN"),
                "Show my schedule"
        );

        assertThat(result.success()).isTrue();
        assertThat(result.suggestedRoutes()).doesNotContain("/app/staff");
        assertThat(result.suggestedRoutes()).containsExactly("/app/sessions");
    }

    @Test
    @DisplayName("permitted staff caller receives /app/staff navigation")
    void permittedStaffCallerReceivesStaffRoute() {
        StaffAssignment upcoming = assignment(
                902L,
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                SessionLifecycleStatus.SCHEDULED
        );
        when(assignmentRepository.findByUserIdOrderByStartAtAscIdAsc(100L))
                .thenReturn(List.of(upcoming));

        AssistantToolResult result = tool.execute(
                staffContext(100L, "ROLE_CENTER_MANAGER"),
                "Show my schedule"
        );

        assertThat(result.success()).isTrue();
        assertThat(result.suggestedRoutes()).contains("/app/staff");
        assertThat(result.suggestedRoutes()).contains("/app/sessions");
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

    private AssistantToolContext staffContext(Long userId, String role) {
        return new AssistantToolContext(
                "schedule-" + userId,
                principal(userId, role),
                Set.of(role)
        );
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
