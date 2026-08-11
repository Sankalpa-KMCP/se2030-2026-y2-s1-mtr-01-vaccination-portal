package com.se2030.vaccinationportal.campaign;

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
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
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

        campaign = campaignRepository.saveAndFlush(new VaccinationCampaign(
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
}
