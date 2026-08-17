package com.se2030.vaccinationportal.campaign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationSessionServiceQueryTests {

    @Autowired
    private VaccinationSessionService sessionService;

    @Autowired
    private VaccinationSessionRepository sessionRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private CampaignCenterAssignmentRepository assignmentRepository;

    @Autowired
    private OperatingHoursRepository operatingHoursRepository;

    private VaccinationCampaign campaign1;
    private VaccinationCampaign campaign2;
    private VaccinationCenter center1;
    private VaccinationCenter center2;

    private VaccinationSession session1;
    private VaccinationSession session2;
    private VaccinationSession session3;

    private static final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);

    @BeforeEach
    void setUp() {
        VaccineType vax = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-QRY-01", "Query Vaccine"));
        TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-QRY-01", "Query Target Group"));

        campaign1 = campaignRepository.saveAndFlush(new VaccinationCampaign(
                "CMP-QRY-01", "Query Campaign 1", "Desc", vax, tg,
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z")
        ));

        campaign2 = campaignRepository.saveAndFlush(new VaccinationCampaign(
                "CMP-QRY-02", "Query Campaign 2", "Desc", vax, tg,
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z")
        ));

        center1 = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-QRY-01", "Query Center 1", "123 Main St", "+94 11 1111111",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"
        ));

        center2 = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-QRY-02", "Query Center 2", "456 Side St", "+94 11 2222222",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"
        ));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign1, center1, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)
        ));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign1, center2, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)
        ));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign2, center1, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)
        ));

        operatingHoursRepository.saveAndFlush(new OperatingHours(
                center1, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)
        ));
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                center2, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)
        ));

        // Session 1: Campaign 1, Center 1, 2026-10-14 03:30 UTC, DRAFT
        session1 = sessionService.createOneOffDraftSession(
                campaign1.getId(),
                center1.getId(),
                Instant.parse("2026-10-14T03:30:00Z"),
                Instant.parse("2026-10-14T06:30:00Z"),
                Instant.parse("2026-10-14T02:30:00Z")
        );

        // Session 2: Campaign 1, Center 2, 2026-10-14 05:30 UTC, SCHEDULED
        session2 = sessionService.createOneOffDraftSession(
                campaign1.getId(),
                center2.getId(),
                Instant.parse("2026-10-14T05:30:00Z"),
                Instant.parse("2026-10-14T08:30:00Z"),
                Instant.parse("2026-10-14T04:30:00Z")
        );
        session2 = sessionService.scheduleSession(session2.getId());

        // Session 3: Campaign 2, Center 1, 2026-10-14 07:30 UTC, DRAFT
        session3 = sessionService.createOneOffDraftSession(
                campaign2.getId(),
                center1.getId(),
                Instant.parse("2026-10-14T07:30:00Z"),
                Instant.parse("2026-10-14T10:30:00Z"),
                Instant.parse("2026-10-14T06:30:00Z")
        );
    }

    // ==========================================
    // Required Session Lookup
    // ==========================================

    @Test
    @DisplayName("getRequiredSessionById returns session when exists")
    void getRequiredSessionByIdSuccess() {
        VaccinationSession found = sessionService.getRequiredSessionById(session1.getId());
        assertNotNull(found);
        assertEquals(session1.getId(), found.getId());
    }

    @Test
    @DisplayName("getRequiredSessionById throws ResourceNotFoundException when missing or null")
    void getRequiredSessionByIdNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> sessionService.getRequiredSessionById(999999L));
        assertThrows(ResourceNotFoundException.class, () -> sessionService.getRequiredSessionById(null));
    }

    // ==========================================
    // List Query Filter Combinations & Ordering
    // ==========================================

    @Test
    @DisplayName("getSessions with no filters returns all sessions ordered by startAt ASC, id ASC")
    void getSessionsNoFilters() {
        List<VaccinationSession> sessions = sessionService.getSessions(null, null, null);
        assertTrue(sessions.size() >= 3);
        // Verify ascending order by startAt ASC, id ASC
        for (int i = 0; i < sessions.size() - 1; i++) {
            VaccinationSession current = sessions.get(i);
            VaccinationSession next = sessions.get(i + 1);
            int startCmp = current.getStartAt().compareTo(next.getStartAt());
            if (startCmp == 0) {
                assertTrue(current.getId() <= next.getId());
            } else {
                assertTrue(startCmp < 0);
            }
        }
        List<Long> ids = sessions.stream().map(VaccinationSession::getId).toList();
        assertTrue(ids.contains(session1.getId()));
        assertTrue(ids.contains(session2.getId()));
        assertTrue(ids.contains(session3.getId()));
    }

    @Test
    @DisplayName("getSessions with campaignId filter returns only sessions for that campaign")
    void getSessionsByCampaignId() {
        List<VaccinationSession> c1Sessions = sessionService.getSessions(campaign1.getId(), null, null);
        assertEquals(2, c1Sessions.size());
        assertTrue(c1Sessions.stream().allMatch(s -> s.getCampaign().getId().equals(campaign1.getId())));

        List<VaccinationSession> c2Sessions = sessionService.getSessions(campaign2.getId(), null, null);
        assertEquals(1, c2Sessions.size());
        assertEquals(session3.getId(), c2Sessions.get(0).getId());
    }

    @Test
    @DisplayName("getSessions with centerId filter returns only sessions for that center")
    void getSessionsByCenterId() {
        List<VaccinationSession> center1Sessions = sessionService.getSessions(null, center1.getId(), null);
        assertEquals(2, center1Sessions.size());
        assertTrue(center1Sessions.stream().allMatch(s -> s.getCenter().getId().equals(center1.getId())));

        List<VaccinationSession> center2Sessions = sessionService.getSessions(null, center2.getId(), null);
        assertEquals(1, center2Sessions.size());
        assertEquals(session2.getId(), center2Sessions.get(0).getId());
    }

    @Test
    @DisplayName("getSessions with status filter returns only sessions in that status")
    void getSessionsByStatus() {
        List<VaccinationSession> draftSessions = sessionService.getSessions(null, null, SessionLifecycleStatus.DRAFT);
        assertTrue(draftSessions.size() >= 2);
        assertTrue(draftSessions.stream().allMatch(s -> s.getLifecycleStatus() == SessionLifecycleStatus.DRAFT));

        List<VaccinationSession> scheduledSessions = sessionService.getSessions(null, null, SessionLifecycleStatus.SCHEDULED);
        assertTrue(scheduledSessions.size() >= 1);
        assertTrue(scheduledSessions.stream().allMatch(s -> s.getLifecycleStatus() == SessionLifecycleStatus.SCHEDULED));
    }

    @Test
    @DisplayName("getSessions with campaignId and centerId combined filters")
    void getSessionsByCampaignAndCenter() {
        List<VaccinationSession> c1c1Sessions = sessionService.getSessions(campaign1.getId(), center1.getId(), null);
        assertEquals(1, c1c1Sessions.size());
        assertEquals(session1.getId(), c1c1Sessions.get(0).getId());
    }

    @Test
    @DisplayName("getSessions with campaignId and status combined filters")
    void getSessionsByCampaignAndStatus() {
        List<VaccinationSession> c1DraftSessions = sessionService.getSessions(campaign1.getId(), null, SessionLifecycleStatus.DRAFT);
        assertEquals(1, c1DraftSessions.size());
        assertEquals(session1.getId(), c1DraftSessions.get(0).getId());

        List<VaccinationSession> c1ScheduledSessions = sessionService.getSessions(campaign1.getId(), null, SessionLifecycleStatus.SCHEDULED);
        assertEquals(1, c1ScheduledSessions.size());
        assertEquals(session2.getId(), c1ScheduledSessions.get(0).getId());
    }

    @Test
    @DisplayName("getSessions with centerId and status combined filters")
    void getSessionsByCenterAndStatus() {
        List<VaccinationSession> ctr1DraftSessions = sessionService.getSessions(null, center1.getId(), SessionLifecycleStatus.DRAFT);
        assertEquals(2, ctr1DraftSessions.size());

        List<VaccinationSession> ctr2DraftSessions = sessionService.getSessions(null, center2.getId(), SessionLifecycleStatus.DRAFT);
        assertEquals(0, ctr2DraftSessions.size());
    }

    @Test
    @DisplayName("getSessions with all three filters (campaignId, centerId, status)")
    void getSessionsByAllThreeFilters() {
        List<VaccinationSession> match = sessionService.getSessions(campaign1.getId(), center1.getId(), SessionLifecycleStatus.DRAFT);
        assertEquals(1, match.size());
        assertEquals(session1.getId(), match.get(0).getId());

        List<VaccinationSession> noMatch = sessionService.getSessions(campaign1.getId(), center1.getId(), SessionLifecycleStatus.SCHEDULED);
        assertEquals(0, noMatch.size());
    }

    @Test
    @DisplayName("getSessions with nonexistent ID filter returns empty list")
    void getSessionsNonExistentFilterReturnsEmpty() {
        List<VaccinationSession> result = sessionService.getSessions(999999L, null, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==========================================
    // Typed Missing Exception Tests (Task 36)
    // ==========================================

    @Test
    @DisplayName("createOneOffDraftSession throws ResourceNotFoundException on nonexistent campaign or center")
    void createOneOffDraftSessionMissingParentThrowsResourceNotFoundException() {
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.createOneOffDraftSession(999999L, center1.getId(), startAt, endAt, bookingCloseAt));

        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.createOneOffDraftSession(campaign1.getId(), 999999L, startAt, endAt, bookingCloseAt));
    }

    @Test
    @DisplayName("Lifecycle mutations throw ResourceNotFoundException for nonexistent session IDs")
    void lifecycleMutationsMissingSessionThrowsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () -> sessionService.scheduleSession(999999L));
        assertThrows(ResourceNotFoundException.class, () -> sessionService.startSession(999999L));
        assertThrows(ResourceNotFoundException.class, () -> sessionService.completeSession(999999L));
    }

    // ==========================================
    // Session Change Events Query Tests (Task 44)
    // ==========================================

    @Test
    @DisplayName("getSessionChangeEvents returns empty list for existing session with no changes")
    void getSessionChangeEventsEmpty() {
        List<com.se2030.vaccinationportal.campaign.model.SessionChangeEvent> events =
                sessionService.getSessionChangeEvents(session1.getId());
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("getSessionChangeEvents throws ResourceNotFoundException when session does not exist")
    void getSessionChangeEventsMissingSession() {
        assertThrows(ResourceNotFoundException.class, () -> sessionService.getSessionChangeEvents(999999L));
        assertThrows(IllegalArgumentException.class, () -> sessionService.getSessionChangeEvents(null));
    }

    @Test
    @DisplayName("getSessionChangeEvents returns events ordered deterministically by createdAt ASC, id ASC")
    void getSessionChangeEventsOrdered() {
        Instant newStartAt = Instant.parse("2026-10-14T04:30:00Z");
        Instant newEndAt = Instant.parse("2026-10-14T07:30:00Z");
        Instant newBookingCloseAt = Instant.parse("2026-10-14T03:30:00Z");

        SessionChangePreview preview = sessionService.previewReschedule(
                session1.getId(), newStartAt, newEndAt, newBookingCloseAt
        );

        sessionService.confirmReschedule(
                session1.getId(),
                newStartAt,
                newEndAt,
                newBookingCloseAt,
                preview.previewToken(),
                "First adjustment",
                "officer-1"
        );

        SessionChangePreview preview2 = sessionService.previewCancellation(session1.getId());
        sessionService.confirmCancellation(
                session1.getId(),
                preview2.previewToken(),
                "Final cancellation",
                "officer-2"
        );

        List<com.se2030.vaccinationportal.campaign.model.SessionChangeEvent> events =
                sessionService.getSessionChangeEvents(session1.getId());
        assertEquals(2, events.size());
        assertEquals(com.se2030.vaccinationportal.campaign.model.SessionChangeType.RESCHEDULED, events.get(0).getChangeType());
        assertEquals("First adjustment", events.get(0).getReason());
        assertEquals(com.se2030.vaccinationportal.campaign.model.SessionChangeType.CANCELLED, events.get(1).getChangeType());
        assertEquals("Final cancellation", events.get(1).getReason());
        assertTrue(events.get(0).getCreatedAt().compareTo(events.get(1).getCreatedAt()) <= 0);
    }

    // ==========================================
    // Typed Missing Exception Tests for Change Operations (Task 45)
    // ==========================================

    @Test
    @DisplayName("Change-control operations throw ResourceNotFoundException for missing session or target center")
    void changeControlOperationsMissingResourceThrowsResourceNotFoundException() {
        Instant start = Instant.parse("2026-10-14T04:30:00Z");
        Instant end = Instant.parse("2026-10-14T07:30:00Z");
        Instant close = Instant.parse("2026-10-14T03:30:00Z");

        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.previewCancellation(999999L));
        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.confirmCancellation(999999L, "token", "reason", "actor"));

        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.previewReschedule(999999L, start, end, close));
        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.confirmReschedule(999999L, start, end, close, "token", "reason", "actor"));

        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.previewCenterTransfer(999999L, center2.getId(), start, end, close));
        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.previewCenterTransfer(session1.getId(), 999999L, start, end, close));

        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.confirmCenterTransfer(999999L, center2.getId(), start, end, close, "token", "reason", "actor"));
        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.confirmCenterTransfer(session1.getId(), 999999L, start, end, close, "token", "reason", "actor"));
    }
}
