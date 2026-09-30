package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.campaign.api.staff.StaffContextQuery;
import com.se2030.vaccinationportal.campaign.api.staff.StaffScheduleSummary;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Staff schedule assistant tool")
class MyStaffScheduleAssistantToolTests {

    private static final Instant NOW = Instant.parse("2026-09-27T03:30:00Z");

    private StaffContextQuery query;
    private MyStaffScheduleAssistantTool tool;

    @BeforeEach
    void setUp() {
        query = mock(StaffContextQuery.class);
        tool = new MyStaffScheduleAssistantTool(
                query,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("returns current and upcoming assignments for only the verified staff user")
    void returnsVerifiedUsersUpcomingAssignments() {
        StaffScheduleSummary upcoming = assignment(
                501L,
                NOW.plusSeconds(3600),
                NOW.plusSeconds(10800),
                "SCHEDULED"
        );
        when(query.scheduleForUser(argThat(p -> p.getUserId().equals(77L)), eq(NOW), eq(false))).thenReturn(List.of(upcoming));

        AssistantToolResult result = tool.execute(
                staffContext(77L, "ROLE_CENTER_MANAGER"),
                "Show my upcoming sessions for staff ID 999"
        );

        verify(query).scheduleForUser(argThat(p -> p.getUserId().equals(77L)), eq(NOW), eq(false));
        verify(query, never()).scheduleForUser(argThat(p -> p.getUserId().equals(999L)), eq(NOW), eq(false));
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
        StaffScheduleSummary today = assignment(
                601L,
                Instant.parse("2026-09-27T02:00:00Z"),
                Instant.parse("2026-09-27T05:00:00Z"),
                "SCHEDULED"
        );
        when(query.scheduleForUser(argThat(p -> p.getUserId().equals(88L)), eq(NOW), eq(true))).thenReturn(List.of(today));

        AssistantToolResult result = tool.execute(
                staffContext(88L, "ROLE_HEALTHCARE_OFFICER"),
                "What should I do today?"
        );

        verify(query).scheduleForUser(argThat(p -> p.getUserId().equals(88L)), eq(NOW), eq(true));
        assertThat(result.resultCount()).isEqualTo(1);
        assertThat(result.contextSnippet())
                .startsWith("Today's verified session assignments")
                .contains("Session ID: 601")
                .doesNotContain("Session ID: 602");
    }

    @Test
    @DisplayName("returns an explicit empty schedule without changing roster meaning")
    void returnsSafeEmptySchedule() {
        when(query.scheduleForUser(argThat(p -> p.getUserId().equals(89L)), eq(NOW), eq(false))).thenReturn(List.of());

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
        verify(query, never()).scheduleForUser(argThat(p -> p.getUserId().equals(90L)), eq(NOW), eq(false));
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
        StaffScheduleSummary scheduledUpcoming = assignment(
                702L,
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                "SCHEDULED"
        );
        when(query.scheduleForUser(argThat(p -> p.getUserId().equals(78L)), eq(NOW), eq(false))).thenReturn(List.of(scheduledUpcoming));

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
        StaffScheduleSummary activeToday = assignment(
                802L,
                Instant.parse("2026-09-27T02:00:00Z"),
                Instant.parse("2026-09-27T06:00:00Z"), // active now
                "IN_PROGRESS"
        );
        when(query.scheduleForUser(argThat(p -> p.getUserId().equals(88L)), eq(NOW), eq(true))).thenReturn(List.of(activeToday));

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
        StaffScheduleSummary upcoming = assignment(
                901L,
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                "SCHEDULED"
        );
        when(query.scheduleForUser(argThat(p -> p.getUserId().equals(99L)), eq(NOW), eq(false))).thenReturn(List.of(upcoming));

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
        StaffScheduleSummary upcoming = assignment(
                902L,
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                "SCHEDULED"
        );
        when(query.scheduleForUser(argThat(p -> p.getUserId().equals(100L)), eq(NOW), eq(false))).thenReturn(List.of(upcoming));

        AssistantToolResult result = tool.execute(
                staffContext(100L, "ROLE_CENTER_MANAGER"),
                "Show my schedule"
        );

        assertThat(result.success()).isTrue();
        assertThat(result.suggestedRoutes()).contains("/app/staff");
        assertThat(result.suggestedRoutes()).contains("/app/sessions");
    }
    private StaffScheduleSummary assignment(Long sessionId, Instant start, Instant end, String lifecycleStatus) {
        return new StaffScheduleSummary(sessionId, "CMP-A", "Campaign A", "CTR-001", "Central Clinic",
                "CHECK_IN_STAFF", lifecycleStatus, start, end, "Asia/Colombo");
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
