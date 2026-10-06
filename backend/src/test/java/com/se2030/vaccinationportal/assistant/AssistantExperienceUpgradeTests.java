package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.service.AssistantService;
import com.se2030.vaccinationportal.assistant.service.InMemoryAssistantRateLimiter;
import com.se2030.vaccinationportal.assistant.tool.AssistantPersonalizedContextBuilder;
import com.se2030.vaccinationportal.assistant.tool.AssistantToolContext;
import com.se2030.vaccinationportal.assistant.tool.AssistantToolRegistry;
import com.se2030.vaccinationportal.assistant.tool.AssistantToolResult;
import com.se2030.vaccinationportal.assistant.tool.MyAppointmentsAssistantTool;
import com.se2030.vaccinationportal.assistant.tool.MyProfileCompletionAssistantTool;
import com.se2030.vaccinationportal.appointment.api.query.CitizenAppointmentQuery;
import com.se2030.vaccinationportal.appointment.api.query.CitizenAppointmentOverview;
import com.se2030.vaccinationportal.appointment.api.query.AppointmentSummary;
import com.se2030.vaccinationportal.appointment.api.query.WaitlistSummary;
import com.se2030.vaccinationportal.shared.auth.dto.CitizenProfileResponse;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.shared.auth.service.CitizenProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantExperienceUpgradeTests {

    @Test
    void upcomingIntentReturnsOnlyUpcomingWithFriendlyScheduleDetails() {
        CitizenAppointmentQuery query = mock(CitizenAppointmentQuery.class);
        MyAppointmentsAssistantTool tool = new MyAppointmentsAssistantTool(query);
        AssistantToolContext context = citizenContext(42L);
        Instant now = Instant.now();

        when(query.appointmentsForCitizen(context.principal())).thenReturn(new CitizenAppointmentOverview(
                List.of(new AppointmentSummary("CONFIRMED", 10L, 100L, true,
                        now.plusSeconds(3600), now.plusSeconds(5400), "Community Booster Campaign",
                        "Colombo Central Clinic", "Colombo 07", false)),
                List.of(new AppointmentSummary("COMPLETED", 20L, 200L, true,
                        now.minusSeconds(5400), now.minusSeconds(3600), null, null, null, false)), List.of()));

        AssistantToolResult result = tool.execute(context, "Show my upcoming appointments");

        assertThat(result.contextSnippet()).contains("Community Booster Campaign");
        assertThat(result.contextSnippet()).contains("Colombo Central Clinic");
        assertThat(result.contextSnippet()).contains("Colombo 07");
        assertThat(result.contextSnippet()).contains("Sri Lanka time");
        assertThat(result.contextSnippet()).doesNotContain("Status: COMPLETED");
        verify(query, never()).waitlistForCitizen(any());
        verify(query, never()).appointmentsAndWaitlistForCitizen(any());
    }

    @Test
    void queuePositionIsRenderedFromTheProviderWithoutAppointmentQueries() {
        CitizenAppointmentQuery query = mock(CitizenAppointmentQuery.class);
        MyAppointmentsAssistantTool tool = new MyAppointmentsAssistantTool(query);
        when(query.waitlistForCitizen(argThat(p -> p.getUserId().equals(42L)))).thenReturn(
                List.of(new WaitlistSummary("WAITING", 10L, 500L, 3)));

        AssistantToolResult result = tool.execute(citizenContext(42L), "What is my queue position?");

        assertThat(result.contextSnippet()).contains("Current Queue Position: 3");
        verify(query).waitlistForCitizen(argThat(p -> p.getUserId().equals(42L)));
        verify(query, never()).appointmentsForCitizen(any());
        verify(query, never()).appointmentsAndWaitlistForCitizen(any());
    }

    @Test
    void profileCompletionReportsMissingFieldsWithoutRawValues() {
        CitizenProfileService profiles = mock(CitizenProfileService.class);
        MyProfileCompletionAssistantTool tool = new MyProfileCompletionAssistantTool(profiles);
        when(profiles.getOwnProfile(42L)).thenReturn(new CitizenProfileResponse(
                42L, "Citizen Name", "citizen@example.com", null, AccountStatus.ACTIVE,
                List.of("ROLE_CITIZEN"), Instant.now(), null, null, null, Instant.now()));

        AssistantToolResult result = tool.execute(citizenContext(42L));

        assertThat(result.contextSnippet()).contains("Profile completion: INCOMPLETE");
        assertThat(result.contextSnippet()).contains("phone number, date of birth, gender, address");
        assertThat(result.contextSnippet()).doesNotContain("Citizen Name");
        assertThat(result.contextSnippet()).doesNotContain("citizen@example.com");
    }

    @Test
    void personalizedRoutesBecomeDeterministicAuthorizedActions() {
        CitizenAppointmentQuery query = mock(CitizenAppointmentQuery.class);
        when(query.appointmentsForCitizen(any())).thenReturn(new CitizenAppointmentOverview(List.of(), List.of(), List.of()));
        MyAppointmentsAssistantTool tool = new MyAppointmentsAssistantTool(query);
        AssistantToolRegistry registry = new AssistantToolRegistry(List.of(tool));
        DeterministicFakeAiResponseProvider provider = new DeterministicFakeAiResponseProvider();
        provider.setCannedActions(List.of());
        AssistantProperties properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setApiKey("test");
        properties.setModel("test");
        AssistantService service = new AssistantService(provider, new InMemoryAssistantRateLimiter(properties),
                new AssistantPersonalizedContextBuilder(registry), registry);
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setRemoteAddr("127.0.0.1");

        AssistantChatResponse response = service.processMessage(
                new AssistantChatRequest("Show my upcoming appointments", List.of(), "/app/citizen"),
                citizenPrincipal(42L), http);

        assertThat(response.suggestedActions()).extracting(action -> action.path())
                .containsExactly("/app/appointments/mine", "/app/appointments/book");
    }

    private static AssistantToolContext citizenContext(Long id) {
        return new AssistantToolContext("request", citizenPrincipal(id), Set.of("ROLE_CITIZEN"));
    }

    private static UserPrincipal citizenPrincipal(Long id) {
        return new UserPrincipal(id, "citizen@example.com", "password", true,
                List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")));
    }
}
