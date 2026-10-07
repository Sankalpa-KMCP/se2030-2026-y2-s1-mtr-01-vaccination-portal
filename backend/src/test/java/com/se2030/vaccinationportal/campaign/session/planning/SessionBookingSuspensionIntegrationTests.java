package com.se2030.vaccinationportal.campaign.session.planning;

import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.session.planning.service.VaccinationSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MF02-R2 Session Booking Suspension Domain & Service Tests")
class SessionBookingSuspensionIntegrationTests {

    @Mock
    private VaccinationSessionRepository sessionRepository;

    @Mock
    private SessionChangeEventRepository sessionChangeEventRepository;

    @InjectMocks
    private VaccinationSessionService sessionService;

    private VaccinationCampaign sampleCampaign;
    private VaccinationCenter sampleCenter;
    private VaccinationSession sampleSession;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sessionService, "clock", Clock.systemUTC());

        sampleCampaign = new VaccinationCampaign(
                "CMP-2026-01",
                "Community Booster",
                "Desc",
                null,
                null,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 30),
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-11-29T23:59:59Z")
        );
        ReflectionTestUtils.setField(sampleCampaign, "id", 1001L);
        ReflectionTestUtils.setField(sampleCampaign, "status", CampaignStatus.PUBLISHED);

        sampleCenter = new VaccinationCenter(
                "VC-01", "City Center", "Colombo", "contact@center.gov",
                com.se2030.vaccinationportal.campaign.center.model.CenterStatus.ACTIVE, 50, 200, "Asia/Colombo"
        );
        ReflectionTestUtils.setField(sampleCenter, "id", 2001L);



        sampleSession = new VaccinationSession(
                sampleCampaign,
                sampleCenter,
                Instant.parse("2026-09-20T08:00:00Z"),
                Instant.parse("2026-09-20T17:00:00Z"),
                Instant.parse("2026-09-19T23:59:59Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(sampleSession, "id", 3001L);
        ReflectionTestUtils.setField(sampleSession, "lifecycleStatus", SessionLifecycleStatus.SCHEDULED);
    }

    @Nested
    @DisplayName("suspendBooking() Tests")
    class SuspendBookingTests {

        @Test
        @DisplayName("Successfully suspends booking for a SCHEDULED session and creates SessionChangeEvent")
        void suspendsBookingSuccessfully() {
            when(sessionRepository.findByIdForUpdate(3001L)).thenReturn(Optional.of(sampleSession));
            when(sessionRepository.save(any(VaccinationSession.class))).thenAnswer(inv -> inv.getArgument(0));

            VaccinationSession result = sessionService.suspendBooking(
                    3001L,
                    "Emergency vaccinator shortage at center",
                    "42"
            );

            assertNotNull(result);
            assertTrue(result.isBookingSuspended());
            verify(sessionRepository).save(sampleSession);

            ArgumentCaptor<SessionChangeEvent> eventCaptor = ArgumentCaptor.forClass(SessionChangeEvent.class);
            verify(sessionChangeEventRepository).save(eventCaptor.capture());

            SessionChangeEvent savedEvent = eventCaptor.getValue();
            assertEquals(SessionChangeType.BOOKING_SUSPENDED, savedEvent.getChangeType());
            assertEquals("Emergency vaccinator shortage at center", savedEvent.getReason());
            assertEquals("42", savedEvent.getActorId());
            assertEquals(0, savedEvent.getAffectedAppointmentCount());
            assertEquals(0, savedEvent.getAffectedWaitlistCount());
        }

        @Test
        @DisplayName("Rejects suspendBooking when session is in DRAFT lifecycle")
        void rejectsSuspendForDraftSession() {
            ReflectionTestUtils.setField(sampleSession, "lifecycleStatus", SessionLifecycleStatus.DRAFT);
            when(sessionRepository.findByIdForUpdate(3001L)).thenReturn(Optional.of(sampleSession));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    sessionService.suspendBooking(3001L, "Operational pause", "42")
            );
            assertTrue(ex.getMessage().contains("Cannot suspend booking for session in DRAFT lifecycle status"));
            verify(sessionChangeEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects suspendBooking when session is already suspended")
        void rejectsRepeatedSuspend() {
            sampleSession.suspendBooking();
            when(sessionRepository.findByIdForUpdate(3001L)).thenReturn(Optional.of(sampleSession));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    sessionService.suspendBooking(3001L, "Operational pause", "42")
            );
            assertTrue(ex.getMessage().contains("Booking is already suspended for session: 3001"));
            verify(sessionChangeEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects suspendBooking with blank or null reason")
        void rejectsBlankOrNullReason() {
            assertThrows(IllegalArgumentException.class, () ->
                    sessionService.suspendBooking(3001L, null, "42")
            );
            assertThrows(IllegalArgumentException.class, () ->
                    sessionService.suspendBooking(3001L, "   ", "42")
            );
        }

        @Test
        @DisplayName("Rejects suspendBooking with reason exceeding 500 characters")
        void rejectsOverlyLongReason() {
            String longReason = "a".repeat(501);
            assertThrows(IllegalArgumentException.class, () ->
                    sessionService.suspendBooking(3001L, longReason, "42")
            );
        }

        @Test
        @DisplayName("Rejects suspendBooking with blank or null actorId")
        void rejectsBlankOrNullActorId() {
            assertThrows(IllegalArgumentException.class, () ->
                    sessionService.suspendBooking(3001L, "Valid reason", null)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    sessionService.suspendBooking(3001L, "Valid reason", "   ")
            );
        }
    }

    @Nested
    @DisplayName("resumeBooking() Tests")
    class ResumeBookingTests {

        @Test
        @DisplayName("Successfully resumes booking for a suspended SCHEDULED session and creates SessionChangeEvent")
        void resumesBookingSuccessfully() {
            sampleSession.suspendBooking();
            when(sessionRepository.findByIdForUpdate(3001L)).thenReturn(Optional.of(sampleSession));
            when(sessionRepository.save(any(VaccinationSession.class))).thenAnswer(inv -> inv.getArgument(0));

            VaccinationSession result = sessionService.resumeBooking(
                    3001L,
                    "Additional staff rostered, resuming intake",
                    "42"
            );

            assertNotNull(result);
            assertFalse(result.isBookingSuspended());
            verify(sessionRepository).save(sampleSession);

            ArgumentCaptor<SessionChangeEvent> eventCaptor = ArgumentCaptor.forClass(SessionChangeEvent.class);
            verify(sessionChangeEventRepository).save(eventCaptor.capture());

            SessionChangeEvent savedEvent = eventCaptor.getValue();
            assertEquals(SessionChangeType.BOOKING_RESUMED, savedEvent.getChangeType());
            assertEquals("Additional staff rostered, resuming intake", savedEvent.getReason());
            assertEquals("42", savedEvent.getActorId());
        }

        @Test
        @DisplayName("Rejects resumeBooking when session is not currently suspended")
        void rejectsResumeWhenNotSuspended() {
            when(sessionRepository.findByIdForUpdate(3001L)).thenReturn(Optional.of(sampleSession));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    sessionService.resumeBooking(3001L, "Resuming intake", "42")
            );
            assertTrue(ex.getMessage().contains("Booking is not suspended for session: 3001"));
            verify(sessionChangeEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects resumeBooking when session is in COMPLETED lifecycle")
        void rejectsResumeForCompletedSession() {
            sampleSession.suspendBooking();
            ReflectionTestUtils.setField(sampleSession, "lifecycleStatus", SessionLifecycleStatus.COMPLETED);
            when(sessionRepository.findByIdForUpdate(3001L)).thenReturn(Optional.of(sampleSession));


            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    sessionService.resumeBooking(3001L, "Resuming intake", "42")
            );
            assertTrue(ex.getMessage().contains("Cannot resume booking for session in COMPLETED lifecycle status"));
            verify(sessionChangeEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects resumeBooking with blank reason")
        void rejectsBlankReasonForResume() {
            assertThrows(IllegalArgumentException.class, () ->
                    sessionService.resumeBooking(3001L, "   ", "42")
            );
        }
    }
}
