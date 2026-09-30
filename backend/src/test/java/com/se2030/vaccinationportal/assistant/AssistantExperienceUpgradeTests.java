package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.appointment.query.JpaCitizenAppointmentQuery;
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
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.session.planning.adapter.JpaTimeSlotDisplayReader;
import com.se2030.vaccinationportal.shared.auth.dto.CitizenProfileResponse;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.shared.auth.service.CitizenProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantExperienceUpgradeTests {

    @Test
    void upcomingIntentReturnsOnlyUpcomingWithFriendlyScheduleDetails() {
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        TimeSlotRepository slots = mock(TimeSlotRepository.class);
        WaitlistRepository waitlist = mock(WaitlistRepository.class);
        MyAppointmentsAssistantTool tool = appointmentTool(appointments, slots, waitlist);
        AssistantToolContext context = citizenContext(42L);
        Instant now = Instant.now();

        Appointment upcoming = new Appointment(42L, 10L, 100L, AppointmentStatus.CONFIRMED, now);
        Appointment completed = new Appointment(42L, 20L, 200L, AppointmentStatus.COMPLETED, now);
        TimeSlot futureSlot = mock(TimeSlot.class);
        VaccinationSession session = mock(VaccinationSession.class);
        VaccinationCampaign campaign = mock(VaccinationCampaign.class);
        VaccinationCenter center = mock(VaccinationCenter.class);
        when(futureSlot.getId()).thenReturn(100L);
        when(futureSlot.getStartTime()).thenReturn(now.plusSeconds(3600));
        when(futureSlot.getEndTime()).thenReturn(now.plusSeconds(5400));
        when(futureSlot.getSession()).thenReturn(session);
        when(session.getCampaign()).thenReturn(campaign);
        when(session.getCenter()).thenReturn(center);
        when(campaign.getName()).thenReturn("Community Booster Campaign");
        when(center.getName()).thenReturn("Colombo Central Clinic");
        when(center.getAddress()).thenReturn("Colombo 07");
        TimeSlot pastSlot = mock(TimeSlot.class);
        when(pastSlot.getId()).thenReturn(200L);
        when(pastSlot.getStartTime()).thenReturn(now.minusSeconds(5400));
        when(pastSlot.getEndTime()).thenReturn(now.minusSeconds(3600));

        when(appointments.findByCitizenIdOrderByBookedAtDesc(42L)).thenReturn(List.of(upcoming, completed));
        when(slots.findByIdIn(any())).thenReturn(List.of(futureSlot, pastSlot));

        AssistantToolResult result = tool.execute(context, "Show my upcoming appointments");

        assertThat(result.contextSnippet()).contains("Community Booster Campaign");
        assertThat(result.contextSnippet()).contains("Colombo Central Clinic");
        assertThat(result.contextSnippet()).contains("Colombo 07");
        assertThat(result.contextSnippet()).contains("Sri Lanka time");
        assertThat(result.contextSnippet()).doesNotContain("Status: COMPLETED");
        verify(waitlist, never()).findByCitizenIdOrderByJoinedAtDesc(42L);
    }

    @Test
    void queuePositionUsesCountQueryAndDoesNotLoadTheSessionQueue() {
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        TimeSlotRepository slots = mock(TimeSlotRepository.class);
        WaitlistRepository waitlist = mock(WaitlistRepository.class);
        MyAppointmentsAssistantTool tool = appointmentTool(appointments, slots, waitlist);
        WaitlistEntry own = mock(WaitlistEntry.class);
        Instant joinedAt = Instant.now();
        when(own.getId()).thenReturn(9L);
        when(own.getCampaignId()).thenReturn(10L);
        when(own.getSessionId()).thenReturn(500L);
        when(own.getStatus()).thenReturn(WaitlistStatus.WAITING);
        when(own.getJoinedAt()).thenReturn(joinedAt);
        when(waitlist.findByCitizenIdOrderByJoinedAtDesc(42L)).thenReturn(List.of(own));
        when(waitlist.countQueuePosition(500L, WaitlistStatus.WAITING, joinedAt, 9L)).thenReturn(3L);

        AssistantToolResult result = tool.execute(citizenContext(42L), "What is my queue position?");

        assertThat(result.contextSnippet()).contains("Current Queue Position: 3");
        verify(waitlist, never()).findBySessionIdAndStatusOrderByJoinedAtAsc(500L, WaitlistStatus.WAITING);
        verify(appointments, never()).findByCitizenIdOrderByBookedAtDesc(42L);
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
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        TimeSlotRepository slots = mock(TimeSlotRepository.class);
        WaitlistRepository waitlist = mock(WaitlistRepository.class);
        when(appointments.findByCitizenIdOrderByBookedAtDesc(42L)).thenReturn(List.of());
        MyAppointmentsAssistantTool tool = appointmentTool(appointments, slots, waitlist);
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

    private static MyAppointmentsAssistantTool appointmentTool(AppointmentRepository appointments,
            TimeSlotRepository slots, WaitlistRepository waitlist) {
        return new MyAppointmentsAssistantTool(new JpaCitizenAppointmentQuery(
                appointments, waitlist, new JpaTimeSlotDisplayReader(slots), Clock.fixed(Instant.now(), ZoneOffset.UTC)));
    }

    private static UserPrincipal citizenPrincipal(Long id) {
        return new UserPrincipal(id, "citizen@example.com", "password", true,
                List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")));
    }
}
