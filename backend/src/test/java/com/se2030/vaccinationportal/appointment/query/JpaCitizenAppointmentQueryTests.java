package com.se2030.vaccinationportal.appointment.query;

import com.se2030.vaccinationportal.appointment.api.query.CitizenAppointmentOverview;


import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.assistant.tool.AssistantToolContext;
import com.se2030.vaccinationportal.assistant.tool.MyAppointmentsAssistantTool;
import com.se2030.vaccinationportal.campaign.api.appointment.TimeSlotDisplayReader;
import com.se2030.vaccinationportal.campaign.api.appointment.TimeSlotDisplaySnapshot;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JpaCitizenAppointmentQueryTests {
    private static final Instant DEADLINE = Instant.parse("2026-01-01T12:00:00Z");
    private final UserPrincipal principal = UserPrincipal.fromClaims(42L, "citizen@example.com", List.of("ROLE_CITIZEN"));
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final WaitlistRepository waitlist = mock(WaitlistRepository.class);
    private final TimeSlotDisplayReader slots = mock(TimeSlotDisplayReader.class);
    private final Clock clock = mock(Clock.class);
    private final AtomicReference<Instant> currentTime = new AtomicReference<>(DEADLINE.minusSeconds(1));
    private final JpaCitizenAppointmentQuery query = new JpaCitizenAppointmentQuery(appointments, waitlist, slots, clock);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenAnswer(ignored -> currentTime.get());
        when(appointments.findByCitizenIdOrderByBookedAtDesc(42L)).thenReturn(List.of(
                new Appointment(42L, 10L, 100L, AppointmentStatus.CONFIRMED, DEADLINE.minusSeconds(3600))));
    }

    @Test
    void confirmedAppointmentBecomesPastWhenDeadlineCrossesDuringSlotRetrieval() {
        when(slots.findByIds(List.of(100L))).thenAnswer(ignored -> {
            assertThat(currentTime.get()).isBefore(DEADLINE);
            verifyNoInteractions(clock);
            currentTime.set(DEADLINE.plusSeconds(1));
            return List.of(slot());
        });

        CitizenAppointmentOverview result = query.appointmentsForCitizen(principal);

        assertThat(result.activeOrUpcoming()).isEmpty();
        assertThat(result.pastOrInactive()).singleElement().satisfies(appointment -> {
            assertThat(appointment.status()).isEqualTo("CONFIRMED");
            assertThat(appointment.confirmedPastTime()).isTrue();
        });
        verify(clock, times(1)).instant();
        verifyNoInteractions(waitlist);
    }

    @Test
    void combinedToolRequestCompletesWaitlistReadBeforeAppointmentComparisonTime() {
        when(waitlist.findByCitizenIdOrderByJoinedAtDesc(42L)).thenAnswer(ignored -> {
            assertThat(currentTime.get()).isBefore(DEADLINE);
            verifyNoInteractions(clock);
            currentTime.set(DEADLINE.plusSeconds(1));
            return List.of();
        });
        when(slots.findByIds(List.of(100L))).thenReturn(List.of(slot()));

        var result = new MyAppointmentsAssistantTool(query).execute(
                new AssistantToolContext("request", principal, Set.of("ROLE_CITIZEN")));

        assertThat(result.contextSnippet()).contains("Past or inactive appointment records",
                "scheduled time has passed but the recorded status remains CONFIRMED");
        assertThat(result.contextSnippet()).doesNotContain("Active or upcoming appointment records (");
        verify(clock, times(1)).instant();
    }

    @Test
    void combinedRequestKeepsAppointmentUpcomingWhenQueueCountCrossesDeadline() {
        Instant joinedAt = DEADLINE.minusSeconds(3600);
        WaitlistEntry entry = waitingEntry(joinedAt);
        when(waitlist.findByCitizenIdOrderByJoinedAtDesc(42L)).thenReturn(List.of(entry));
        when(slots.findByIds(List.of(100L))).thenReturn(List.of(slot()));
        when(waitlist.countQueuePosition(500L, WaitlistStatus.WAITING, joinedAt, 9L)).thenAnswer(ignored -> {
            assertThat(currentTime.get()).isBefore(DEADLINE);
            verify(clock, times(1)).instant();
            currentTime.set(DEADLINE.plusSeconds(1));
            return 3L;
        });

        var result = new MyAppointmentsAssistantTool(query).execute(
                new AssistantToolContext("request", principal, Set.of("ROLE_CITIZEN")));

        assertThat(currentTime.get()).isAfter(DEADLINE);
        assertThat(result.contextSnippet()).contains("Active or upcoming appointment records (", "Status: CONFIRMED",
                "Status: WAITING", "Current Queue Position: 3");
        assertThat(result.contextSnippet()).doesNotContain("Past or inactive appointment records (",
                "scheduled time has passed but the recorded status remains CONFIRMED");
        assertThat(result.resultCount()).isEqualTo(2);
        var order = inOrder(appointments, waitlist, slots, clock);
        order.verify(appointments).findByCitizenIdOrderByBookedAtDesc(42L);
        order.verify(waitlist).findByCitizenIdOrderByJoinedAtDesc(42L);
        order.verify(slots).findByIds(List.of(100L));
        order.verify(clock).instant();
        order.verify(waitlist).countQueuePosition(500L, WaitlistStatus.WAITING, joinedAt, 9L);
        order.verifyNoMoreInteractions();
    }

    @Test
    void waitlistOnlyRequestDoesNotReadAppointmentsOrSlotsOrSampleComparisonTime() {
        Instant joinedAt = DEADLINE.minusSeconds(3600);
        WaitlistEntry entry = waitingEntry(joinedAt);
        when(waitlist.findByCitizenIdOrderByJoinedAtDesc(42L)).thenReturn(List.of(entry));
        when(waitlist.countQueuePosition(500L, WaitlistStatus.WAITING, joinedAt, 9L)).thenReturn(3L);

        var result = new MyAppointmentsAssistantTool(query).execute(
                new AssistantToolContext("request", principal, Set.of("ROLE_CITIZEN")), "my waitlist");

        assertThat(result.contextSnippet()).contains("Status: WAITING", "Current Queue Position: 3");
        assertThat(result.resultCount()).isEqualTo(1);
        verify(waitlist, times(1)).findByCitizenIdOrderByJoinedAtDesc(42L);
        verify(waitlist, times(1)).countQueuePosition(500L, WaitlistStatus.WAITING, joinedAt, 9L);
        verifyNoInteractions(appointments, slots, clock);
    }

    @Test
    void principalScopedAppointmentsAreOrderedAndBoundedAtTheProvider() {
        var rows = new java.util.ArrayList<Appointment>();
        var schedule = new java.util.ArrayList<TimeSlotDisplaySnapshot>();
        for (long i = 10; i >= 1; i--) {
            rows.add(new Appointment(42L, i, i, AppointmentStatus.CONFIRMED, DEADLINE.minusSeconds(3600)));
            schedule.add(new TimeSlotDisplaySnapshot(i, DEADLINE.plusSeconds(i), DEADLINE.plusSeconds(100),
                    "Campaign " + i, "Clinic", "Address"));
        }
        when(appointments.findByCitizenIdOrderByBookedAtDesc(42L)).thenReturn(rows);
        when(slots.findByIds(rows.stream().map(Appointment::getSlotId).toList())).thenReturn(schedule);

        var result = query.appointmentsForCitizen(principal);

        assertThat(result.activeOrUpcoming()).extracting(a -> a.slotId()).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(result.pastOrInactive()).isEmpty();
        verify(appointments).findByCitizenIdOrderByBookedAtDesc(42L);
        verify(appointments, org.mockito.Mockito.never()).findByCitizenIdOrderByBookedAtDesc(999L);
    }

    @Test
    void waitlistIsBoundedAndCountsOnlyQualifiedWaitingRowsWithoutLoadingTheWholeQueue() {
        Instant joined = DEADLINE.minusSeconds(3600);
        var waiting = waitingEntry(joined);
        var inactive = mock(WaitlistEntry.class);
        when(inactive.getStatus()).thenReturn(WaitlistStatus.CANCELLED);
        when(waitlist.findByCitizenIdOrderByJoinedAtDesc(42L))
                .thenReturn(List.of(waiting, inactive, inactive, inactive, inactive, waiting));
        when(waitlist.countQueuePosition(500L, WaitlistStatus.WAITING, joined, 9L)).thenReturn(3L);

        var result = query.waitlistForCitizen(principal);

        assertThat(result).hasSize(5);
        assertThat(result.getFirst().queuePosition()).isEqualTo(3);
        assertThat(result.subList(1, 5)).allSatisfy(r -> assertThat(r.queuePosition()).isNull());
        verify(waitlist, times(1)).countQueuePosition(500L, WaitlistStatus.WAITING, joined, 9L);
        verify(waitlist, org.mockito.Mockito.never()).findBySessionIdAndStatusOrderByJoinedAtAscIdAsc(500L, WaitlistStatus.WAITING);
        verifyNoInteractions(appointments, slots, clock);
    }

    private static WaitlistEntry waitingEntry(Instant joinedAt) {
        WaitlistEntry entry = mock(WaitlistEntry.class);
        when(entry.getId()).thenReturn(9L);
        when(entry.getCampaignId()).thenReturn(10L);
        when(entry.getSessionId()).thenReturn(500L);
        when(entry.getStatus()).thenReturn(WaitlistStatus.WAITING);
        when(entry.getJoinedAt()).thenReturn(joinedAt);
        return entry;
    }

    private static TimeSlotDisplaySnapshot slot() {
        return new TimeSlotDisplaySnapshot(100L, DEADLINE.minusSeconds(1800), DEADLINE,
                "Campaign", "Center", "Address");
    }
}
