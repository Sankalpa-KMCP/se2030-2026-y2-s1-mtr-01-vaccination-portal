package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.SessionBookingStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.SessionSlotCapacitySummary;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.service.SessionBookingStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MF02-R2 Session Booking Status Authoritative Calculation Tests")
class SessionBookingStatusCalculationTests {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    private SessionBookingStatusService bookingStatusService;

    private final Instant baseTime = Instant.parse("2026-09-15T10:00:00Z");
    private Clock fixedClock;

    private VaccinationCampaign publishedCampaign;
    private VaccinationCenter sampleCenter;
    private VaccinationSession scheduledSession;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(baseTime, ZoneOffset.UTC);
        bookingStatusService = new SessionBookingStatusService(timeSlotRepository, fixedClock);

        publishedCampaign = new VaccinationCampaign(
                "CMP-2026-01",
                "National Booster Drive",
                "Booster Programme",
                null,
                null,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 30),
                Instant.parse("2026-09-01T00:00:00Z"), // booking open: Sept 1
                Instant.parse("2026-11-29T23:59:59Z")  // booking close: Nov 29
        );
        ReflectionTestUtils.setField(publishedCampaign, "status", CampaignStatus.PUBLISHED);

        sampleCenter = new VaccinationCenter(
                "VC-01", "Central Clinic", "Colombo 07", "contact@center.gov",
                com.se2030.vaccinationportal.campaign.model.CenterStatus.ACTIVE, 50, 200, "UTC"
        );
        ReflectionTestUtils.setField(sampleCenter, "id", 2001L);



        scheduledSession = new VaccinationSession(
                publishedCampaign,
                sampleCenter,
                Instant.parse("2026-09-20T08:00:00Z"), // session start: Sept 20
                Instant.parse("2026-09-20T17:00:00Z"), // session end: Sept 20
                Instant.parse("2026-09-19T23:59:59Z"), // session booking close: Sept 19
                "UTC"
        );
        ReflectionTestUtils.setField(scheduledSession, "id", 101L);
        ReflectionTestUtils.setField(scheduledSession, "lifecycleStatus", SessionLifecycleStatus.SCHEDULED);
    }

    @Nested
    @DisplayName("CLOSED Status Precedence & Lifetime Closure Cases")
    class ClosedStatusPrecedenceTests {

        @Test
        @DisplayName("Returns CLOSED for DRAFT session lifecycle")
        void returnsClosedForDraftSession() {
            ReflectionTestUtils.setField(scheduledSession, "lifecycleStatus", SessionLifecycleStatus.DRAFT);
            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession);
            assertEquals(SessionBookingStatus.CLOSED, status);
            verify(timeSlotRepository, never()).summarizeCapacityBySessionId(anyLong());
        }

        @Test
        @DisplayName("Returns CLOSED for IN_PROGRESS session lifecycle")
        void returnsClosedForInProgressSession() {
            ReflectionTestUtils.setField(scheduledSession, "lifecycleStatus", SessionLifecycleStatus.IN_PROGRESS);
            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession);
            assertEquals(SessionBookingStatus.CLOSED, status);
            verify(timeSlotRepository, never()).summarizeCapacityBySessionId(anyLong());
        }

        @Test
        @DisplayName("Returns CLOSED for COMPLETED session lifecycle")
        void returnsClosedForCompletedSession() {
            ReflectionTestUtils.setField(scheduledSession, "lifecycleStatus", SessionLifecycleStatus.COMPLETED);
            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession);
            assertEquals(SessionBookingStatus.CLOSED, status);
            verify(timeSlotRepository, never()).summarizeCapacityBySessionId(anyLong());
        }

        @Test
        @DisplayName("Returns CLOSED for CANCELLED session lifecycle")
        void returnsClosedForCancelledSession() {
            ReflectionTestUtils.setField(scheduledSession, "lifecycleStatus", SessionLifecycleStatus.CANCELLED);
            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession);
            assertEquals(SessionBookingStatus.CLOSED, status);
            verify(timeSlotRepository, never()).summarizeCapacityBySessionId(anyLong());
        }

        @Test
        @DisplayName("Returns CLOSED when campaign is DRAFT or SCHEDULED")
        void returnsClosedForUnpublishedCampaign() {
            ReflectionTestUtils.setField(publishedCampaign, "status", CampaignStatus.DRAFT);
            assertEquals(SessionBookingStatus.CLOSED, bookingStatusService.deriveBookingStatus(scheduledSession));

            ReflectionTestUtils.setField(publishedCampaign, "status", CampaignStatus.SCHEDULED);
            assertEquals(SessionBookingStatus.CLOSED, bookingStatusService.deriveBookingStatus(scheduledSession));
        }

        @Test
        @DisplayName("Returns CLOSED when campaign is operationally closed (COMPLETED, CANCELLED, ARCHIVED)")
        void returnsClosedForTerminalCampaign() {
            ReflectionTestUtils.setField(publishedCampaign, "status", CampaignStatus.COMPLETED);
            assertEquals(SessionBookingStatus.CLOSED, bookingStatusService.deriveBookingStatus(scheduledSession));

            ReflectionTestUtils.setField(publishedCampaign, "status", CampaignStatus.CANCELLED);
            assertEquals(SessionBookingStatus.CLOSED, bookingStatusService.deriveBookingStatus(scheduledSession));

            ReflectionTestUtils.setField(publishedCampaign, "status", CampaignStatus.ARCHIVED);
            assertEquals(SessionBookingStatus.CLOSED, bookingStatusService.deriveBookingStatus(scheduledSession));
        }

        @Test
        @DisplayName("Returns CLOSED when current time is strictly after session bookingCloseAt")
        void returnsClosedWhenSessionBookingCloseExpired() {
            Instant expiredTime = Instant.parse("2026-09-20T00:00:01Z");
            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession, expiredTime);
            assertEquals(SessionBookingStatus.CLOSED, status);
        }

        @Test
        @DisplayName("Returns CLOSED when current time is strictly after session startAt")
        void returnsClosedWhenSessionStartPassed() {
            Instant pastStartTime = Instant.parse("2026-09-20T08:00:01Z");
            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession, pastStartTime);
            assertEquals(SessionBookingStatus.CLOSED, status);
        }

        @Test
        @DisplayName("Returns CLOSED when current time is strictly after campaign bookingCloseAt")
        void returnsClosedWhenCampaignBookingCloseExpired() {
            Instant pastCampaignClose = Instant.parse("2026-11-30T00:00:00Z");
            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession, pastCampaignClose);
            assertEquals(SessionBookingStatus.CLOSED, status);
        }

        @Test
        @DisplayName("Returns CLOSED when session has 0 configured time slots")
        void returnsClosedWhenNoSlotsConfigured() {
            when(timeSlotRepository.summarizeCapacityBySessionId(101L))
                    .thenReturn(Optional.of(new SessionSlotCapacitySummary(101L, 0L, 0L, 0L)));

            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession);
            assertEquals(SessionBookingStatus.CLOSED, status);
        }

        @Test
        @DisplayName("Stale bookingSuspended=true on COMPLETED session strictly evaluates as CLOSED")
        void staleSuspensionOnCompletedSessionYieldsClosed() {
            scheduledSession.suspendBooking();
            ReflectionTestUtils.setField(scheduledSession, "lifecycleStatus", SessionLifecycleStatus.COMPLETED);

            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession);
            assertEquals(SessionBookingStatus.CLOSED, status);
        }

        @Test
        @DisplayName("Stale bookingSuspended=true on CANCELLED session strictly evaluates as CLOSED")
        void staleSuspensionOnCancelledSessionYieldsClosed() {
            scheduledSession.suspendBooking();
            ReflectionTestUtils.setField(scheduledSession, "lifecycleStatus", SessionLifecycleStatus.CANCELLED);

            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession);
            assertEquals(SessionBookingStatus.CLOSED, status);
        }
    }

    @Nested
    @DisplayName("SUSPENDED Status Cases")
    class SuspendedStatusTests {

        @Test
        @DisplayName("Returns SUSPENDED when session bookingSuspended is manually set to true")
        void returnsSuspendedWhenSessionManuallySuspended() {
            scheduledSession.suspendBooking();
            when(timeSlotRepository.summarizeCapacityBySessionId(101L))
                    .thenReturn(Optional.of(new SessionSlotCapacitySummary(101L, 5L, 100L, 10L)));


            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession);
            assertEquals(SessionBookingStatus.SUSPENDED, status);
        }

        @Test
        @DisplayName("Returns SUSPENDED when parent campaign status is SUSPENDED")
        void returnsSuspendedWhenCampaignSuspended() {
            ReflectionTestUtils.setField(publishedCampaign, "status", CampaignStatus.SUSPENDED);
            when(timeSlotRepository.summarizeCapacityBySessionId(101L))
                    .thenReturn(Optional.of(new SessionSlotCapacitySummary(101L, 5L, 100L, 10L)));

            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession);
            assertEquals(SessionBookingStatus.SUSPENDED, status);
        }
    }

    @Nested
    @DisplayName("NOT_OPEN Status and Exact Boundary Tests")
    class NotOpenStatusTests {

        @Test
        @DisplayName("Returns NOT_OPEN when evaluation instant is strictly before campaign bookingOpenAt")
        void returnsNotOpenBeforeCampaignBookingOpen() {
            Instant beforeOpen = Instant.parse("2026-08-31T23:59:59Z");
            when(timeSlotRepository.summarizeCapacityBySessionId(101L))
                    .thenReturn(Optional.of(new SessionSlotCapacitySummary(101L, 5L, 100L, 0L)));

            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession, beforeOpen);
            assertEquals(SessionBookingStatus.NOT_OPEN, status);
        }

        @Test
        @DisplayName("At exact campaign bookingOpenAt instant, session transitions to OPEN")
        void exactBookingOpenBoundaryTransitionsToOpen() {
            Instant exactOpen = Instant.parse("2026-09-01T00:00:00Z");
            when(timeSlotRepository.summarizeCapacityBySessionId(101L))
                    .thenReturn(Optional.of(new SessionSlotCapacitySummary(101L, 5L, 100L, 10L)));

            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession, exactOpen);
            assertEquals(SessionBookingStatus.OPEN, status);
        }
    }

    @Nested
    @DisplayName("FULL and OPEN Status & Capacity Tests")
    class FullAndOpenStatusTests {

        @Test
        @DisplayName("Returns FULL when valid active window and all slots have 0 remaining capacity")
        void returnsFullWhenAllSlotsAtCapacity() {
            when(timeSlotRepository.summarizeCapacityBySessionId(101L))
                    .thenReturn(Optional.of(new SessionSlotCapacitySummary(101L, 4L, 80L, 80L)));

            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession);
            assertEquals(SessionBookingStatus.FULL, status);
        }

        @Test
        @DisplayName("Returns OPEN when valid active window and at least one slot has capacity")
        void returnsOpenWhenCapacityAvailable() {
            when(timeSlotRepository.summarizeCapacityBySessionId(101L))
                    .thenReturn(Optional.of(new SessionSlotCapacitySummary(101L, 4L, 80L, 79L)));

            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession);
            assertEquals(SessionBookingStatus.OPEN, status);
        }

        @Test
        @DisplayName("At exact session bookingCloseAt instant, session remains eligible for OPEN/FULL")
        void exactSessionBookingCloseBoundaryRemainsEligible() {
            Instant exactClose = Instant.parse("2026-09-19T23:59:59Z");
            when(timeSlotRepository.summarizeCapacityBySessionId(101L))
                    .thenReturn(Optional.of(new SessionSlotCapacitySummary(101L, 4L, 80L, 40L)));

            SessionBookingStatus status = bookingStatusService.deriveBookingStatus(scheduledSession, exactClose);
            assertEquals(SessionBookingStatus.OPEN, status);
        }
    }

    @Nested
    @DisplayName("Batch Evaluation & N+1 Elimination Tests")
    class BatchEvaluationTests {

        @Test
        @DisplayName("Derives statuses for collection of sessions in single batch query without N+1 calls")
        void derivesStatusesInSingleBatchQuery() {
            VaccinationSession session1 = new VaccinationSession(
                    publishedCampaign, sampleCenter,
                    Instant.parse("2026-09-20T08:00:00Z"), Instant.parse("2026-09-20T17:00:00Z"),
                    Instant.parse("2026-09-19T23:59:59Z"), "UTC"
            );
            ReflectionTestUtils.setField(session1, "id", 101L);
            ReflectionTestUtils.setField(session1, "lifecycleStatus", SessionLifecycleStatus.SCHEDULED);

            VaccinationSession session2 = new VaccinationSession(
                    publishedCampaign, sampleCenter,
                    Instant.parse("2026-09-21T08:00:00Z"), Instant.parse("2026-09-21T17:00:00Z"),
                    Instant.parse("2026-09-20T23:59:59Z"), "UTC"
            );
            ReflectionTestUtils.setField(session2, "id", 102L);
            ReflectionTestUtils.setField(session2, "lifecycleStatus", SessionLifecycleStatus.DRAFT); // CLOSED immediately

            VaccinationSession session3 = new VaccinationSession(
                    publishedCampaign, sampleCenter,
                    Instant.parse("2026-09-22T08:00:00Z"), Instant.parse("2026-09-22T17:00:00Z"),
                    Instant.parse("2026-09-21T23:59:59Z"), "UTC"
            );
            ReflectionTestUtils.setField(session3, "id", 103L);
            ReflectionTestUtils.setField(session3, "lifecycleStatus", SessionLifecycleStatus.SCHEDULED);
            session3.suspendBooking(); // SUSPENDED

            when(timeSlotRepository.summarizeCapacityBySessionIds(Set.of(101L, 103L)))

                    .thenReturn(List.of(
                            new SessionSlotCapacitySummary(101L, 3L, 60L, 20L),
                            new SessionSlotCapacitySummary(103L, 3L, 60L, 0L)
                    ));

            Map<Long, SessionBookingStatus> resultMap = bookingStatusService.deriveBookingStatuses(List.of(session1, session2, session3));

            assertEquals(3, resultMap.size());
            assertEquals(SessionBookingStatus.OPEN, resultMap.get(101L));
            assertEquals(SessionBookingStatus.CLOSED, resultMap.get(102L));
            assertEquals(SessionBookingStatus.SUSPENDED, resultMap.get(103L));

            verify(timeSlotRepository, times(1)).summarizeCapacityBySessionIds(anyCollection());
            verify(timeSlotRepository, never()).summarizeCapacityBySessionId(anyLong());
        }
    }
}
