package com.se2030.vaccinationportal.campaign.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.dto.response.SessionResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionScheduleSnapshotResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionSummaryResponse;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionBookingStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;

import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.service.SessionChangePreview;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SessionDtoMapperTests {

    @Test
    @DisplayName("Maps null session safely to null")
    void mapsNullsSafely() {
        assertNull(SessionDtoMapper.toResponse(null));
        assertNull(SessionDtoMapper.toSummaryResponse(null));
    }

    @Test
    @DisplayName("Maps one-off VaccinationSession to SessionResponse and SessionSummaryResponse")
    void mapsOneOffVaccinationSession() {
        VaccinationCampaign campaign = new VaccinationCampaign(
                "CMP-2026-01",
                "Campaign 1",
                "Desc",
                null,
                null,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 30),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-11-29T18:00:00Z")
        );
        ReflectionTestUtils.setField(campaign, "id", 1001L);

        VaccinationCenter center = new VaccinationCenter(
                "CTR-COLOMBO-01",
                "Colombo Central Clinic",
                "123 Main Street",
                "+94 11 2345678",
                CenterStatus.ACTIVE,
                150,
                50,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(center, "id", 2001L);

        Instant startAt = Instant.parse("2026-10-15T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-15T12:00:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-15T08:00:00Z");

        VaccinationSession session = new VaccinationSession(
                campaign,
                center,
                startAt,
                endAt,
                bookingCloseAt,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(session, "id", 5001L);
        Instant createdAt = Instant.parse("2026-08-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-01T10:30:00Z");
        ReflectionTestUtils.setField(session, "createdAt", createdAt);
        ReflectionTestUtils.setField(session, "updatedAt", updatedAt);

        SessionResponse response = SessionDtoMapper.toResponse(session);
        assertNotNull(response);
        assertEquals(5001L, response.id());
        assertEquals(1001L, response.campaignId());
        assertEquals(2001L, response.centerId());
        assertNull(response.recurrenceRuleId());
        assertNull(response.occurrenceKey());
        assertNull(response.originOccurrenceDate());
        assertEquals(startAt, response.startAt());
        assertEquals(endAt, response.endAt());
        assertEquals(bookingCloseAt, response.bookingCloseAt());
        assertEquals("Asia/Colombo", response.scheduleTimezone());
        assertEquals(SessionLifecycleStatus.DRAFT, response.lifecycleStatus());
        assertFalse(response.generated());
        assertEquals(SessionBookingStatus.CLOSED, response.bookingStatus());
        assertFalse(response.bookingSuspended());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());

        SessionSummaryResponse summary = SessionDtoMapper.toSummaryResponse(session);
        assertNotNull(summary);
        assertEquals(5001L, summary.id());
        assertEquals(1001L, summary.campaignId());
        assertEquals(2001L, summary.centerId());
        assertEquals(startAt, summary.startAt());
        assertEquals(endAt, summary.endAt());
        assertEquals(bookingCloseAt, summary.bookingCloseAt());
        assertEquals("Asia/Colombo", summary.scheduleTimezone());
        assertEquals(SessionLifecycleStatus.DRAFT, summary.lifecycleStatus());
        assertFalse(summary.generated());
        assertEquals(SessionBookingStatus.CLOSED, summary.bookingStatus());
        assertFalse(summary.bookingSuspended());
    }


    @Test
    @DisplayName("Maps recurring generated VaccinationSession with recurrence rule fidelity")
    void mapsRecurringGeneratedVaccinationSession() {
        VaccinationCampaign campaign = new VaccinationCampaign(
                "CMP-2026-02",
                "Campaign 2",
                "Desc",
                null,
                null,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 30),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-11-29T18:00:00Z")
        );
        ReflectionTestUtils.setField(campaign, "id", 1002L);

        VaccinationCenter center = new VaccinationCenter(
                "CTR-KANDY-01",
                "Kandy Hospital",
                "45 Hill Street",
                "+94 81 2233445",
                CenterStatus.ACTIVE,
                100,
                40,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(center, "id", 2002L);

        SessionRecurrenceRule rule = new SessionRecurrenceRule();
        ReflectionTestUtils.setField(rule, "id", 3002L);

        Instant startAt = Instant.parse("2026-10-20T08:30:00Z");
        Instant endAt = Instant.parse("2026-10-20T11:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-20T07:30:00Z");
        LocalDate originDate = LocalDate.of(2026, 10, 20);
        String occurrenceKey = "RR-3002-2026-10-20";

        VaccinationSession session = new VaccinationSession(
                campaign,
                center,
                rule,
                occurrenceKey,
                originDate,
                startAt,
                endAt,
                bookingCloseAt,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(session, "id", 5002L);
        ReflectionTestUtils.setField(session, "lifecycleStatus", SessionLifecycleStatus.SCHEDULED);
        Instant createdAt = Instant.parse("2026-08-05T08:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-05T08:15:00Z");
        ReflectionTestUtils.setField(session, "createdAt", createdAt);
        ReflectionTestUtils.setField(session, "updatedAt", updatedAt);

        SessionResponse response = SessionDtoMapper.toResponse(session);
        assertNotNull(response);
        assertEquals(5002L, response.id());
        assertEquals(1002L, response.campaignId());
        assertEquals(2002L, response.centerId());
        assertEquals(3002L, response.recurrenceRuleId());
        assertEquals(occurrenceKey, response.occurrenceKey());
        assertEquals(originDate, response.originOccurrenceDate());
        assertEquals(startAt, response.startAt());
        assertEquals(endAt, response.endAt());
        assertEquals(bookingCloseAt, response.bookingCloseAt());
        assertEquals("Asia/Colombo", response.scheduleTimezone());
        assertEquals(SessionLifecycleStatus.SCHEDULED, response.lifecycleStatus());
        assertTrue(response.generated());
        assertEquals(SessionBookingStatus.OPEN, response.bookingStatus());
        assertFalse(response.bookingSuspended());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());

        SessionSummaryResponse summary = SessionDtoMapper.toSummaryResponse(session);
        assertNotNull(summary);
        assertEquals(5002L, summary.id());
        assertEquals(1002L, summary.campaignId());
        assertEquals(2002L, summary.centerId());
        assertEquals(startAt, summary.startAt());
        assertEquals(endAt, summary.endAt());
        assertEquals(bookingCloseAt, summary.bookingCloseAt());
        assertEquals("Asia/Colombo", summary.scheduleTimezone());
        assertEquals(SessionLifecycleStatus.SCHEDULED, summary.lifecycleStatus());
        assertTrue(summary.generated());
        assertEquals(SessionBookingStatus.OPEN, summary.bookingStatus());
        assertFalse(summary.bookingSuspended());
    }


    @Test
    @DisplayName("Maps SessionScheduleSnapshot to SessionScheduleSnapshotResponse correctly and handles null safely")
    void mapsSessionScheduleSnapshot() {
        assertNull(SessionDtoMapper.toSnapshotResponse(null));

        SessionScheduleSnapshot snapshot = new SessionScheduleSnapshot(
                200L,
                "Asia/Colombo",
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                SessionLifecycleStatus.DRAFT
        );

        SessionScheduleSnapshotResponse response = SessionDtoMapper.toSnapshotResponse(snapshot);
        assertNotNull(response);
        assertEquals(200L, response.centerId());
        assertEquals("Asia/Colombo", response.scheduleTimezone());
        assertEquals(Instant.parse("2026-10-15T09:00:00Z"), response.startAt());
        assertEquals(Instant.parse("2026-10-15T12:00:00Z"), response.endAt());
        assertEquals(Instant.parse("2026-10-15T08:00:00Z"), response.bookingCloseAt());
        assertEquals(SessionLifecycleStatus.DRAFT, response.lifecycleStatus());
    }

    @Test
    @DisplayName("Maps SessionChangePreview to SessionChangePreviewResponse correctly and handles null safely")
    void mapsSessionChangePreview() {
        assertNull(SessionDtoMapper.toPreviewResponse(null));

        SessionScheduleSnapshot oldSnapshot = new SessionScheduleSnapshot(
                200L,
                "Asia/Colombo",
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                SessionLifecycleStatus.DRAFT
        );

        SessionScheduleSnapshot newSnapshot = new SessionScheduleSnapshot(
                201L,
                "Asia/Colombo",
                Instant.parse("2026-10-16T10:00:00Z"),
                Instant.parse("2026-10-16T13:00:00Z"),
                Instant.parse("2026-10-16T09:00:00Z"),
                SessionLifecycleStatus.DRAFT
        );

        SessionChangePreview preview = new SessionChangePreview(
                500L,
                com.se2030.vaccinationportal.campaign.model.SessionChangeType.CENTER_TRANSFERRED,
                oldSnapshot,
                newSnapshot,
                5,
                2,
                "preview-token-xyz"
        );

        com.se2030.vaccinationportal.campaign.dto.response.SessionChangePreviewResponse response =
                SessionDtoMapper.toPreviewResponse(preview);

        assertNotNull(response);
        assertEquals(500L, response.sessionId());
        assertEquals(com.se2030.vaccinationportal.campaign.model.SessionChangeType.CENTER_TRANSFERRED, response.changeType());
        assertNotNull(response.oldSnapshot());
        assertEquals(200L, response.oldSnapshot().centerId());
        assertNotNull(response.newSnapshot());
        assertEquals(201L, response.newSnapshot().centerId());
        assertEquals(5, response.affectedAppointmentCount());
        assertEquals(2, response.affectedWaitlistCount());
        assertEquals("preview-token-xyz", response.previewToken());
    }

    @Test
    @DisplayName("Maps SessionChangeEvent to SessionChangeEventResponse correctly including cancellation snapshot")
    void mapsSessionChangeEvent() {
        assertNull(SessionDtoMapper.toChangeEventResponse(null));

        VaccinationCampaign campaign = new VaccinationCampaign(
                "CMP-2026-01",
                "Campaign 1",
                "Desc",
                null,
                null,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 30),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-11-29T18:00:00Z")
        );
        ReflectionTestUtils.setField(campaign, "id", 100L);

        VaccinationCenter center = new VaccinationCenter(
                "CTR-COLOMBO-01",
                "Colombo Clinic",
                "123 Main St",
                "+94 11 2345678",
                CenterStatus.ACTIVE,
                150,
                50,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(center, "id", 200L);

        VaccinationSession session = new VaccinationSession(
                campaign,
                center,
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(session, "id", 500L);

        SessionScheduleSnapshot oldSnapshot = new SessionScheduleSnapshot(
                200L,
                "Asia/Colombo",
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                SessionLifecycleStatus.DRAFT
        );

        SessionScheduleSnapshot newCancelledSnapshot = new SessionScheduleSnapshot(
                200L,
                "Asia/Colombo",
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                SessionLifecycleStatus.CANCELLED
        );

        com.se2030.vaccinationportal.campaign.model.SessionChangeEvent event =
                new com.se2030.vaccinationportal.campaign.model.SessionChangeEvent(
                        session,
                        com.se2030.vaccinationportal.campaign.model.SessionChangeType.CANCELLED,
                        oldSnapshot,
                        newCancelledSnapshot,
                        "Severe cyclone alert",
                        12,
                        3,
                        "officer-42"
                );
        ReflectionTestUtils.setField(event, "id", 9001L);
        Instant createdAt = Instant.parse("2026-08-10T12:00:00Z");
        ReflectionTestUtils.setField(event, "createdAt", createdAt);

        com.se2030.vaccinationportal.campaign.dto.response.SessionChangeEventResponse response =
                SessionDtoMapper.toChangeEventResponse(event);

        assertNotNull(response);
        assertEquals(9001L, response.id());
        assertEquals(500L, response.sessionId());
        assertEquals(com.se2030.vaccinationportal.campaign.model.SessionChangeType.CANCELLED, response.changeType());
        assertEquals(SessionLifecycleStatus.DRAFT, response.oldSnapshot().lifecycleStatus());
        assertEquals(SessionLifecycleStatus.CANCELLED, response.newSnapshot().lifecycleStatus());
        assertEquals("Severe cyclone alert", response.reason());
        assertEquals(12, response.affectedAppointmentCount());
        assertEquals(3, response.affectedWaitlistCount());
        assertEquals("officer-42", response.actorId());
        assertEquals(createdAt, response.createdAt());
    }
}
