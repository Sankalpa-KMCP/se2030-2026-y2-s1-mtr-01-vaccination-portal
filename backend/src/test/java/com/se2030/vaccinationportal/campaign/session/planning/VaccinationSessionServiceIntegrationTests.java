package com.se2030.vaccinationportal.campaign.session.planning;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.center.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.center.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.session.planning.service.VaccinationSessionService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationSessionServiceIntegrationTests {

    @Autowired private VaccinationSessionService service;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;

    private static final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);

    @BeforeEach
    void setUp() {
        VaccineType vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-SESS-SRV", "Session Service Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-SESS-SRV", "Session Service Target Group"));

        campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-SESS-SRV", "Session Service Campaign", "Description",
                vaccineType, targetGroup,
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"),
                CampaignStatus.DRAFT));

        center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-SESS-SRV", "Session Service Center", "456 Main St", "+94 11 333 4444",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        // Wednesday OperatingHours in Asia/Colombo (08:00 to 17:00)
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                center, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
    }

    @Test
    void createOneOffDraftSession_validParameters_success() {
        // Wednesday Oct 14: 09:00 to 12:00 in Asia/Colombo (UTC+5:30) is 03:30 to 06:30 UTC
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        VaccinationSession session = service.createOneOffDraftSession(
                campaign.getId(), center.getId(), startAt, endAt, bookingCloseAt);

        assertNotNull(session.getId());
        assertEquals(campaign.getId(), session.getCampaign().getId());
        assertEquals(center.getId(), session.getCenter().getId());
        assertNull(session.getRecurrenceRule());
        assertNull(session.getOccurrenceKey());
        assertNull(session.getOriginOccurrenceDate());
        assertFalse(session.isGenerated());
        assertEquals("Asia/Colombo", session.getScheduleTimezone());
        assertEquals(SessionLifecycleStatus.DRAFT, session.getLifecycleStatus());
        assertEquals(startAt, session.getStartAt());
        assertEquals(endAt, session.getEndAt());
        assertEquals(bookingCloseAt, session.getBookingCloseAt());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getUpdatedAt());

        // Verify retrieval
        Optional<VaccinationSession> retrieved = service.getSessionById(session.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(session.getId(), retrieved.get().getId());
    }

    @Test
    void createOneOffDraftSession_missingCampaignId_throws() {
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                service.createOneOffDraftSession(null, center.getId(), startAt, endAt, bookingCloseAt));
    }

    @Test
    void createOneOffDraftSession_missingCenterId_throws() {
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                service.createOneOffDraftSession(campaign.getId(), null, startAt, endAt, bookingCloseAt));
    }

    @Test
    void createOneOffDraftSession_nonExistentCampaignId_throws() {
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.createOneOffDraftSession(999_999L, center.getId(), startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("Campaign not found"));
    }

    @Test
    void createOneOffDraftSession_nonExistentCenterId_throws() {
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.createOneOffDraftSession(campaign.getId(), 999_999L, startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("Vaccination center not found"));
    }

    @Test
    void createOneOffDraftSession_overlappingSessionsAtSameCenter_bothPersist() {
        // AC-9: Overlapping sessions at the same center are not generically prohibited.
        // Session 1: 09:00 to 13:00 (Colombo) -> 03:30 to 07:30 UTC
        Instant start1 = Instant.parse("2026-10-14T03:30:00Z");
        Instant end1 = Instant.parse("2026-10-14T07:30:00Z");
        VaccinationSession session1 = service.createOneOffDraftSession(
                campaign.getId(), center.getId(), start1, end1, start1);

        // Session 2: 11:00 to 15:00 (Colombo) -> 05:30 to 09:30 UTC (overlaps Session 1)
        Instant start2 = Instant.parse("2026-10-14T05:30:00Z");
        Instant end2 = Instant.parse("2026-10-14T09:30:00Z");
        VaccinationSession session2 = service.createOneOffDraftSession(
                campaign.getId(), center.getId(), start2, end2, start2);

        assertNotNull(session1.getId());
        assertNotNull(session2.getId());
        assertEquals(2, service.getSessionsForCenter(center.getId()).size());
        assertEquals(2, service.getSessionsForCampaign(campaign.getId()).size());
    }

    @Test
    void queries_campaignAndCenterLookups_returnExpectedLists() {
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        VaccinationSession session = service.createOneOffDraftSession(
                campaign.getId(), center.getId(), startAt, endAt, bookingCloseAt);

        List<VaccinationSession> campaignSessions = service.getSessionsForCampaign(campaign.getId());
        assertEquals(1, campaignSessions.size());
        assertEquals(session.getId(), campaignSessions.get(0).getId());

        List<VaccinationSession> centerSessions = service.getSessionsForCenter(center.getId());
        assertEquals(1, centerSessions.size());
        assertEquals(session.getId(), centerSessions.get(0).getId());

        assertTrue(service.getSessionById(999_999L).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.getSessionById(null));
        assertThrows(IllegalArgumentException.class, () -> service.getSessionsForCampaign(null));
        assertThrows(IllegalArgumentException.class, () -> service.getSessionsForCenter(null));
    }

    @Test
    void createOneOffDraftSession_whenCampaignIsSuspended_throwsIllegalArgumentException() {
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        VaccineType vax = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-SESS-SUSP", "Vaccine"));
        TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-SESS-SUSP", "Group"));

        VaccinationCampaign suspendedCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-SUSP-1OFF", "Suspended Campaign", null, vax, tg,
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"), CampaignStatus.SUSPENDED));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                suspendedCampaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.createOneOffDraftSession(suspendedCampaign.getId(), center.getId(), startAt, endAt, bookingCloseAt));
        assertTrue(ex.getMessage().contains("Cannot create one-off session for suspended campaign"));
    }

    @Test
    void scheduleSession_whenCampaignIsSuspended_throwsIllegalArgumentException() {
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        VaccineType vax = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-SCHED-SUSP", "Vaccine"));
        TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-SCHED-SUSP", "Group"));

        VaccinationCampaign suspendedCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-SCHED-SUSP", "Suspended Campaign", null, vax, tg,
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"), CampaignStatus.SUSPENDED));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                suspendedCampaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        VaccinationSession draftSession = sessionRepository.saveAndFlush(new VaccinationSession(
                suspendedCampaign, center, startAt, endAt, bookingCloseAt, "Asia/Colombo"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.scheduleSession(draftSession.getId()));
        assertTrue(ex.getMessage().contains("Cannot schedule session for suspended campaign"));
        assertEquals(SessionLifecycleStatus.DRAFT, sessionRepository.findById(draftSession.getId()).orElseThrow().getLifecycleStatus());
    }

    @Test
    void createOneOffDraftSession_whenCampaignIsOperationallyClosed_throwsIllegalArgumentException() {
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        VaccineType vax = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-SESS-CLOSED", "Vaccine"));
        TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-SESS-CLOSED", "Group"));

        for (CampaignStatus status : List.of(CampaignStatus.COMPLETED, CampaignStatus.CANCELLED, CampaignStatus.ARCHIVED)) {
            VaccinationCampaign closedCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                    "CMP-CLOSED-" + status.name(), "Closed Campaign", null, vax, tg,
                    CAMPAIGN_START, CAMPAIGN_END,
                    Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"), status));
            assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                    closedCampaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    service.createOneOffDraftSession(closedCampaign.getId(), center.getId(), startAt, endAt, bookingCloseAt));
            assertTrue(ex.getMessage().contains("Cannot create one-off session for operationally closed campaign"));
        }
    }

    @Test
    void scheduleSession_whenCampaignIsOperationallyClosed_throwsIllegalArgumentException() {
        Instant startAt = Instant.parse("2026-10-14T03:30:00Z");
        Instant endAt = Instant.parse("2026-10-14T06:30:00Z");
        Instant bookingCloseAt = Instant.parse("2026-10-14T02:30:00Z");

        VaccineType vax = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-SCHED-CLOSED", "Vaccine"));
        TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-SCHED-CLOSED", "Group"));

        for (CampaignStatus status : List.of(CampaignStatus.COMPLETED, CampaignStatus.CANCELLED, CampaignStatus.ARCHIVED)) {
            VaccinationCampaign closedCampaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                    "CMP-SCHED-CLOSED-" + status.name(), "Closed Campaign", null, vax, tg,
                    CAMPAIGN_START, CAMPAIGN_END,
                    Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"), status));
            assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                    closedCampaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

            VaccinationSession draftSession = sessionRepository.saveAndFlush(new VaccinationSession(
                    closedCampaign, center, startAt, endAt, bookingCloseAt, "Asia/Colombo"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    service.scheduleSession(draftSession.getId()));
            assertTrue(ex.getMessage().contains("Cannot schedule session for operationally closed campaign"));
            assertEquals(SessionLifecycleStatus.DRAFT, sessionRepository.findById(draftSession.getId()).orElseThrow().getLifecycleStatus());
        }
    }

    @Test
    void startSession_allowedCampaignStates_succeeds() {
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(30);
        Instant startAt = Instant.now().minus(java.time.Duration.ofHours(1));
        Instant endAt = Instant.now().plus(java.time.Duration.ofHours(2));
        Instant bookingCloseAt = startAt.minus(java.time.Duration.ofHours(1));

        for (CampaignStatus status : List.of(CampaignStatus.PUBLISHED, CampaignStatus.ACTIVE)) {
            VaccineType vax = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-START-OK-" + status.name(), "Vaccine"));
            TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-START-OK-" + status.name(), "Group"));
            VaccinationCampaign c = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                    "CMP-START-OK-" + status.name(), "Campaign " + status.name(), null, vax, tg,
                    startDate, endDate,
                    Instant.now().minus(java.time.Duration.ofDays(10)), Instant.now().plus(java.time.Duration.ofDays(30)),
                    status));
            assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                    c, center, startDate, endDate.plusDays(1)));

            VaccinationSession session = new VaccinationSession(c, center, startAt, endAt, bookingCloseAt, "Asia/Colombo");
            session.schedule();
            session = sessionRepository.saveAndFlush(session);

            VaccinationSession started = service.startSession(session.getId());
            assertEquals(SessionLifecycleStatus.IN_PROGRESS, started.getLifecycleStatus());
            assertEquals(SessionLifecycleStatus.IN_PROGRESS, sessionRepository.findById(session.getId()).orElseThrow().getLifecycleStatus());
        }
    }

    @Test
    void startSession_forbiddenCampaignStates_throwsIllegalStateExceptionAndLeavesSessionUnchanged() {
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(30);
        Instant startAt = Instant.now().minus(java.time.Duration.ofHours(1));
        Instant endAt = Instant.now().plus(java.time.Duration.ofHours(2));
        Instant bookingCloseAt = startAt.minus(java.time.Duration.ofHours(1));

        List<CampaignStatus> forbidden = List.of(
                CampaignStatus.DRAFT,
                CampaignStatus.SCHEDULED,
                CampaignStatus.SUSPENDED,
                CampaignStatus.COMPLETED,
                CampaignStatus.CANCELLED,
                CampaignStatus.ARCHIVED
        );

        for (CampaignStatus status : forbidden) {
            VaccineType vax = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-START-NO-" + status.name(), "Vaccine"));
            TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-START-NO-" + status.name(), "Group"));
            VaccinationCampaign c = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                    "CMP-START-NO-" + status.name(), "Campaign " + status.name(), null, vax, tg,
                    startDate, endDate,
                    Instant.now().minus(java.time.Duration.ofDays(10)), Instant.now().plus(java.time.Duration.ofDays(30)),
                    status));
            assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                    c, center, startDate, endDate.plusDays(1)));

            VaccinationSession session = new VaccinationSession(c, center, startAt, endAt, bookingCloseAt, "Asia/Colombo");
            session.schedule();
            session = sessionRepository.saveAndFlush(session);

            Long sessionId = session.getId();
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.startSession(sessionId));
            assertTrue(ex.getMessage().contains("Cannot start vaccination session for campaign"));
            assertTrue(ex.getMessage().contains(status.name()));

            assertEquals(SessionLifecycleStatus.SCHEDULED, sessionRepository.findById(sessionId).orElseThrow().getLifecycleStatus());
        }
    }

    @Test
    void completeSession_allowedCampaignStates_succeeds() {
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(30);
        Instant startAt = Instant.now().minus(java.time.Duration.ofHours(2));
        Instant endAt = Instant.now().plus(java.time.Duration.ofHours(1));
        Instant bookingCloseAt = startAt.minus(java.time.Duration.ofHours(1));

        for (CampaignStatus status : List.of(CampaignStatus.PUBLISHED, CampaignStatus.ACTIVE, CampaignStatus.SUSPENDED)) {
            VaccineType vax = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-COMP-OK-" + status.name(), "Vaccine"));
            TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-COMP-OK-" + status.name(), "Group"));
            VaccinationCampaign c = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                    "CMP-COMP-OK-" + status.name(), "Campaign " + status.name(), null, vax, tg,
                    startDate, endDate,
                    Instant.now().minus(java.time.Duration.ofDays(10)), Instant.now().plus(java.time.Duration.ofDays(30)),
                    status));
            assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                    c, center, startDate, endDate.plusDays(1)));

            VaccinationSession session = new VaccinationSession(c, center, startAt, endAt, bookingCloseAt, "Asia/Colombo");
            session.schedule();
            session.start();
            session = sessionRepository.saveAndFlush(session);

            VaccinationSession completed = service.completeSession(session.getId());
            assertEquals(SessionLifecycleStatus.COMPLETED, completed.getLifecycleStatus());
            assertEquals(SessionLifecycleStatus.COMPLETED, sessionRepository.findById(session.getId()).orElseThrow().getLifecycleStatus());
        }
    }

    @Test
    void completeSession_forbiddenCampaignStates_throwsIllegalStateExceptionAndLeavesSessionUnchanged() {
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(30);
        Instant startAt = Instant.now().minus(java.time.Duration.ofHours(2));
        Instant endAt = Instant.now().plus(java.time.Duration.ofHours(1));
        Instant bookingCloseAt = startAt.minus(java.time.Duration.ofHours(1));

        List<CampaignStatus> forbidden = List.of(
                CampaignStatus.DRAFT,
                CampaignStatus.SCHEDULED,
                CampaignStatus.COMPLETED,
                CampaignStatus.CANCELLED,
                CampaignStatus.ARCHIVED
        );

        for (CampaignStatus status : forbidden) {
            VaccineType vax = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-COMP-NO-" + status.name(), "Vaccine"));
            TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-COMP-NO-" + status.name(), "Group"));
            VaccinationCampaign c = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                    "CMP-COMP-NO-" + status.name(), "Campaign " + status.name(), null, vax, tg,
                    startDate, endDate,
                    Instant.now().minus(java.time.Duration.ofDays(10)), Instant.now().plus(java.time.Duration.ofDays(30)),
                    status));
            assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                    c, center, startDate, endDate.plusDays(1)));

            VaccinationSession session = new VaccinationSession(c, center, startAt, endAt, bookingCloseAt, "Asia/Colombo");
            session.schedule();
            session.start();
            session = sessionRepository.saveAndFlush(session);

            Long sessionId = session.getId();
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.completeSession(sessionId));
            assertTrue(ex.getMessage().contains("Cannot complete vaccination session for campaign"));
            assertTrue(ex.getMessage().contains(status.name()));

            assertEquals(SessionLifecycleStatus.IN_PROGRESS, sessionRepository.findById(sessionId).orElseThrow().getLifecycleStatus());
        }
    }
}
