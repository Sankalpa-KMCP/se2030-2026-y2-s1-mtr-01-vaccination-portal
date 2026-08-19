package com.se2030.vaccinationportal.campaign.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSeriesChangeRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewSeriesChangeRequest;
import com.se2030.vaccinationportal.campaign.dto.response.RecurrenceGenerationResponse;
import com.se2030.vaccinationportal.campaign.dto.response.RecurrenceRuleDefinitionSnapshotResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SeriesChangePreviewResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SeriesChangeResultResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SeriesOccurrencePreviewItemResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionRecurrenceRuleResponse;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.RecurrenceRuleDefinitionSnapshot;
import com.se2030.vaccinationportal.campaign.model.SeriesOccurrenceCategory;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.service.RecurrenceGenerationResult;
import com.se2030.vaccinationportal.campaign.service.SeriesChangePreview;
import com.se2030.vaccinationportal.campaign.service.SeriesChangeResult;
import com.se2030.vaccinationportal.campaign.service.SeriesOccurrencePreviewItem;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RecurrenceRuleDtoMapperTests {

    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private SessionRecurrenceRule rule;

    @BeforeEach
    void setUp() {
        campaign = new VaccinationCampaign(
                "CMP-2026-AUTUMN",
                "Autumn 2026 Campaign",
                "Community booster drive",
                null,
                null,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 30),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-11-29T18:00:00Z")
        );
        ReflectionTestUtils.setField(campaign, "id", 100L);

        center = new VaccinationCenter(
                "CTR-COLOMBO-01",
                "Colombo Central Vaccination Center",
                "123 Main Street, Colombo 01",
                "+94 11 2345678",
                CenterStatus.ACTIVE,
                150,
                50,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(center, "id", 200L);

        rule = new SessionRecurrenceRule(
                campaign,
                center,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 31),
                LocalTime.of(8, 30),
                LocalTime.of(16, 30),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(rule, "id", 1L);
        ReflectionTestUtils.setField(rule, "generationKey", "gen-key-uuid-12345");
        ReflectionTestUtils.setField(rule, "createdAt", Instant.parse("2026-08-01T10:00:00Z"));
        ReflectionTestUtils.setField(rule, "updatedAt", Instant.parse("2026-08-01T10:00:00Z"));
    }

    @Test
    @DisplayName("toResponse maps SessionRecurrenceRule fields correctly")
    void testToResponse() {
        SessionRecurrenceRuleResponse response = RecurrenceRuleDtoMapper.toResponse(rule);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(100L, response.campaignId());
        assertEquals(200L, response.centerId());
        assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), response.weekdays());
        assertEquals(LocalDate.of(2026, 9, 1), response.startDate());
        assertEquals(LocalDate.of(2026, 10, 31), response.endDate());
        assertEquals(LocalTime.of(8, 30), response.startTime());
        assertEquals(LocalTime.of(16, 30), response.endTime());
        assertEquals("Asia/Colombo", response.timezone());
        assertEquals(SessionRecurrenceRuleStatus.ACTIVE, response.status());
        assertEquals("gen-key-uuid-12345", response.generationKey());
        assertEquals(Instant.parse("2026-08-01T10:00:00Z"), response.createdAt());
        assertEquals(Instant.parse("2026-08-01T10:00:00Z"), response.updatedAt());

        assertNull(RecurrenceRuleDtoMapper.toResponse(null));
    }

    @Test
    @DisplayName("toSnapshotResponse maps RecurrenceRuleDefinitionSnapshot correctly")
    void testToSnapshotResponse() {
        RecurrenceRuleDefinitionSnapshot snapshot = RecurrenceRuleDefinitionSnapshot.from(rule);
        RecurrenceRuleDefinitionSnapshotResponse response = RecurrenceRuleDtoMapper.toSnapshotResponse(snapshot);

        assertNotNull(response);
        assertEquals(snapshot.getWeekdays(), response.weekdays());
        assertEquals(snapshot.getStartDate(), response.startDate());
        assertEquals(snapshot.getEndDate(), response.endDate());
        assertEquals(snapshot.getStartTime(), response.startTime());
        assertEquals(snapshot.getEndTime(), response.endTime());
        assertEquals(snapshot.getTimezone(), response.timezone());

        assertNull(RecurrenceRuleDtoMapper.toSnapshotResponse(null));
    }

    @Test
    @DisplayName("toDefinitionSnapshot converts PreviewSeriesChangeRequest correctly")
    void testToDefinitionSnapshotFromPreviewRequest() {
        PreviewSeriesChangeRequest request = new PreviewSeriesChangeRequest(
                Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 11, 15),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                "Asia/Colombo"
        );

        RecurrenceRuleDefinitionSnapshot snapshot = RecurrenceRuleDtoMapper.toDefinitionSnapshot(request);

        assertNotNull(snapshot);
        assertEquals(Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), snapshot.getWeekdays());
        assertEquals(LocalDate.of(2026, 9, 15), snapshot.getStartDate());
        assertEquals(LocalDate.of(2026, 11, 15), snapshot.getEndDate());
        assertEquals(LocalTime.of(9, 0), snapshot.getStartTime());
        assertEquals(LocalTime.of(17, 0), snapshot.getEndTime());
        assertEquals("Asia/Colombo", snapshot.getTimezone());

        assertNull(RecurrenceRuleDtoMapper.toDefinitionSnapshot((PreviewSeriesChangeRequest) null));
    }

    @Test
    @DisplayName("toDefinitionSnapshot converts ConfirmSeriesChangeRequest correctly")
    void testToDefinitionSnapshotFromConfirmRequest() {
        ConfirmSeriesChangeRequest request = new ConfirmSeriesChangeRequest(
                Set.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 1),
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                "Asia/Colombo",
                "token-123",
                "Change reason",
                "ACTOR-1"
        );

        RecurrenceRuleDefinitionSnapshot snapshot = RecurrenceRuleDtoMapper.toDefinitionSnapshot(request);

        assertNotNull(snapshot);
        assertEquals(Set.of(DayOfWeek.MONDAY), snapshot.getWeekdays());
        assertEquals(LocalDate.of(2026, 9, 1), snapshot.getStartDate());

        assertNull(RecurrenceRuleDtoMapper.toDefinitionSnapshot((ConfirmSeriesChangeRequest) null));
    }

    @Test
    @DisplayName("toPreviewItemResponse maps SeriesOccurrencePreviewItem correctly")
    void testToPreviewItemResponse() {
        SessionScheduleSnapshot curSnapshot = new SessionScheduleSnapshot(
                200L, "Asia/Colombo", Instant.parse("2026-09-01T03:00:00Z"), Instant.parse("2026-09-01T11:00:00Z"), Instant.parse("2026-09-01T03:00:00Z"), SessionLifecycleStatus.DRAFT);
        SessionScheduleSnapshot propSnapshot = new SessionScheduleSnapshot(
                200L, "Asia/Colombo", Instant.parse("2026-09-01T03:30:00Z"), Instant.parse("2026-09-01T11:30:00Z"), Instant.parse("2026-09-01T03:30:00Z"), SessionLifecycleStatus.DRAFT);

        SeriesOccurrencePreviewItem item = new SeriesOccurrencePreviewItem(
                "gen-key-uuid-12345:2026-09-01",
                LocalDate.of(2026, 9, 1),
                SeriesOccurrenceCategory.RETAINED,
                501L,
                curSnapshot,
                propSnapshot,
                2,
                1
        );

        SeriesOccurrencePreviewItemResponse response = RecurrenceRuleDtoMapper.toPreviewItemResponse(item);

        assertNotNull(response);
        assertEquals("gen-key-uuid-12345:2026-09-01", response.occurrenceKey());
        assertEquals(LocalDate.of(2026, 9, 1), response.originOccurrenceDate());
        assertEquals(SeriesOccurrenceCategory.RETAINED, response.category());
        assertEquals(501L, response.sessionId());
        assertNotNull(response.currentSnapshot());
        assertNotNull(response.proposedSnapshot());
        assertEquals(2, response.affectedAppointmentCount());
        assertEquals(1, response.affectedWaitlistCount());

        assertNull(RecurrenceRuleDtoMapper.toPreviewItemResponse(null));
    }

    @Test
    @DisplayName("toPreviewResponse maps SeriesChangePreview correctly")
    void testToPreviewResponse() {
        RecurrenceRuleDefinitionSnapshot curDef = RecurrenceRuleDefinitionSnapshot.from(rule);
        SeriesChangePreview preview = new SeriesChangePreview(
                1L, curDef, curDef, Collections.emptyList(), 0, 0, "preview-token-xyz"
        );

        SeriesChangePreviewResponse response = RecurrenceRuleDtoMapper.toPreviewResponse(preview);

        assertNotNull(response);
        assertEquals(1L, response.ruleId());
        assertNotNull(response.currentRuleDefinition());
        assertNotNull(response.proposedRuleDefinition());
        assertTrue(response.occurrenceEntries().isEmpty());
        assertEquals(0, response.totalAffectedAppointmentsCount());
        assertEquals(0, response.totalAffectedWaitlistCount());
        assertEquals("preview-token-xyz", response.previewToken());

        assertNull(RecurrenceRuleDtoMapper.toPreviewResponse(null));
    }

    @Test
    @DisplayName("toResultResponse maps SeriesChangeResult correctly")
    void testToResultResponse() {
        RecurrenceRuleDefinitionSnapshot curDef = RecurrenceRuleDefinitionSnapshot.from(rule);
        SeriesChangePreview preview = new SeriesChangePreview(
                1L, curDef, curDef, Collections.emptyList(), 0, 0, "preview-token-xyz"
        );
        SeriesChangeResult result = new SeriesChangeResult(
                rule, preview, 10, 3, 2, 1, 0, 13
        );

        SeriesChangeResultResponse response = RecurrenceRuleDtoMapper.toResultResponse(result);

        assertNotNull(response);
        assertNotNull(response.rule());
        assertNotNull(response.preview());
        assertEquals(10, response.rescheduledCount());
        assertEquals(3, response.cancelledCount());
        assertEquals(2, response.newCount());
        assertEquals(1, response.preservedCount());
        assertEquals(0, response.ineligibleCount());
        assertEquals(13, response.eventsCreatedCount());

        assertNull(RecurrenceRuleDtoMapper.toResultResponse(null));
    }

    @Test
    @DisplayName("toGenerationResponse maps RecurrenceGenerationResult correctly")
    void testToGenerationResponse() {
        VaccinationSession session = new VaccinationSession(
                campaign,
                center,
                Instant.parse("2026-09-01T03:00:00Z"),
                Instant.parse("2026-09-01T11:00:00Z"),
                Instant.parse("2026-09-01T03:00:00Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(session, "id", 501L);

        RecurrenceGenerationResult result = new RecurrenceGenerationResult(5, 2, List.of(session));
        RecurrenceGenerationResponse response = RecurrenceRuleDtoMapper.toGenerationResponse(result);

        assertNotNull(response);
        assertEquals(5, response.createdCount());
        assertEquals(2, response.existingCount());
        assertEquals(1, response.sessions().size());
        assertEquals(501L, response.sessions().get(0).id());

        assertNull(RecurrenceRuleDtoMapper.toGenerationResponse(null));
    }
}
