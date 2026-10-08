package com.se2030.vaccinationportal.campaign.core;

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
import com.se2030.vaccinationportal.campaign.core.service.VaccinationCampaignService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying reverse configuration-impact rules for VaccinationCampaign service dates.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignReverseImpactIntegrationTests {

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private Clock clock;
    @PersistenceContext private EntityManager entityManager;

    private VaccineType vaccineType;
    private TargetGroup targetGroup;
    private VaccinationCenter center;

    @BeforeEach
    void setUp() {
        vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-REV-CAMPAIGN", "Rev Campaign Vaccine", "dose", true)
        );
        targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-REV-CAMPAIGN", "Rev Campaign Target", true)
        );
        center = centerRepository.saveAndFlush(
                new VaccinationCenter(
                        "CTR-REV-CAMP-01",
                        "Rev Campaign Center",
                        "123 Main St",
                        "555-0199",
                        CenterStatus.ACTIVE,
                        50,
                        20,
                        "UTC"
                )
        );

        LocalDate today = LocalDate.now(clock);
        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(
                    new OperatingHours(
                            center,
                            day,
                            LocalTime.of(8, 0),
                            LocalTime.of(20, 0),
                            today.minusYears(1),
                            today.plusYears(1)
                    )
            );
        }
    }

    private VaccinationCampaign createCampaign(String code, LocalDate start, LocalDate end) {
        return campaignRepository.saveAndFlush(
                CampaignTestFixtures.campaign(
                        code,
                        "Campaign " + code,
                        "Desc",
                        vaccineType,
                        targetGroup,
                        start,
                        end,
                        Instant.now(clock).minusSeconds(3600),
                        Instant.now(clock).plusSeconds(3600),
                        CampaignStatus.ACTIVE
                )
        );
    }

    private CampaignCenterAssignment createAssignment(VaccinationCampaign c, LocalDate start, LocalDate end) {
        return assignmentRepository.saveAndFlush(
                new CampaignCenterAssignment(c, center, start, end)
        );
    }

    @Test
    @DisplayName("Shortening service start date past an active future session is rejected")
    void shortenServiceStartPastFutureSessionThrowsException() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(10);
        LocalDate end = today.plusDays(60);
        VaccinationCampaign campaign = createCampaign("CMP-REV-01", start, end);
        createAssignment(campaign, start, end);

        // Session on today + 5 days
        LocalDate sessionDate = today.plusDays(5);
        Instant sessionStart = sessionDate.atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant sessionEnd = sessionDate.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession session = new VaccinationSession(
                campaign, center, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Attempting to move campaign start to today + 10 (past session on today + 5) must fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                campaignService.updateServiceDates(campaign.getId(), today.plusDays(10), end)
        );
        assertTrue(ex.getMessage().contains("Cannot shorten campaign service dates"));
    }

    @Test
    @DisplayName("Shortening service end date before an active future session is rejected")
    void shortenServiceEndBeforeFutureSessionThrowsException() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(10);
        LocalDate end = today.plusDays(60);
        VaccinationCampaign campaign = createCampaign("CMP-REV-02", start, end);
        createAssignment(campaign, start, end);

        // Session on today + 50 days
        LocalDate sessionDate = today.plusDays(50);
        Instant sessionStart = sessionDate.atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant sessionEnd = sessionDate.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession session = new VaccinationSession(
                campaign, center, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Attempting to move campaign end to today + 40 (before session on today + 50) must fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                campaignService.updateServiceDates(campaign.getId(), start, today.plusDays(40))
        );
        assertTrue(ex.getMessage().contains("Cannot shorten campaign service dates"));
    }

    @Test
    @DisplayName("Expanding campaign dates when sessions exist is allowed")
    void expandCampaignDatesAllowed() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(10);
        LocalDate end = today.plusDays(60);
        VaccinationCampaign campaign = createCampaign("CMP-REV-03", start, end);
        createAssignment(campaign, today, today.plusDays(100));

        LocalDate sessionDate = today.plusDays(20);
        Instant sessionStart = sessionDate.atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant sessionEnd = sessionDate.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession session = new VaccinationSession(
                campaign, center, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        assertDoesNotThrow(() ->
                campaignService.updateServiceDates(campaign.getId(), today, today.plusDays(80))
        );
        VaccinationCampaign updated = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(today, updated.getServiceStartDate());
        assertEquals(today.plusDays(80), updated.getServiceEndDate());
    }

    @Test
    @DisplayName("Session on exact start/end boundary dates is allowed")
    void sessionOnExactBoundaryDatesAllowed() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(10);
        LocalDate end = today.plusDays(30);
        VaccinationCampaign campaign = createCampaign("CMP-REV-04", today, today.plusDays(60));
        createAssignment(campaign, today, today.plusDays(60));

        // Session on start boundary (today + 10)
        LocalDate sessionDate = today.plusDays(10);
        Instant sessionStart = sessionDate.atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant sessionEnd = sessionDate.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession session = new VaccinationSession(
                campaign, center, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        assertDoesNotThrow(() ->
                campaignService.updateServiceDates(campaign.getId(), start, end)
        );
    }

    @Test
    @DisplayName("Cross-midnight session evaluated correctly in center scheduling timezone")
    void crossMidnightSessionEvaluatedInTimezone() {
        LocalDate today = LocalDate.now(clock);
        VaccinationCenter tokioCenter = centerRepository.saveAndFlush(
                new VaccinationCenter(
                        "CTR-TOKYO", "Tokyo Center", "Tokyo", "123", CenterStatus.ACTIVE, 100, 20, "Asia/Tokyo"
                )
        );
        LocalDate start = today.plusDays(5);
        LocalDate end = today.plusDays(30);
        VaccinationCampaign campaign = createCampaign("CMP-REV-05", start, end);
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, tokioCenter, start, end));

        // Session on today + 5 from 23:00 JST to 01:00 JST (next day JST)
        LocalDate sessionDayJst = today.plusDays(5);
        ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");
        ZonedDateTime startJst = sessionDayJst.atTime(23, 0).atZone(tokyoZone);
        ZonedDateTime endJst = sessionDayJst.plusDays(1).atTime(1, 0).atZone(tokyoZone);

        Instant sessionStart = startJst.toInstant();
        Instant sessionEnd = endJst.toInstant();
        VaccinationSession session = new VaccinationSession(
                campaign, tokioCenter, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "Asia/Tokyo"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Moving start to today + 6 must fail because session touches today + 5 in Tokyo timezone
        assertThrows(IllegalArgumentException.class, () ->
                campaignService.updateServiceDates(campaign.getId(), today.plusDays(6), end)
        );
    }

    @Test
    @DisplayName("IN_PROGRESS session blocks campaign service date shortening")
    void inProgressSessionBlocksServiceDateShortening() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(10);
        LocalDate end = today.plusDays(60);
        VaccinationCampaign campaign = createCampaign("CMP-REV-06", start, end);
        createAssignment(campaign, start, end);

        // Session ending in future with status IN_PROGRESS on today
        Instant now = clock.instant();
        Instant sessionStart = now.minusSeconds(1800);
        Instant sessionEnd = now.plusSeconds(1800);
        VaccinationSession session = new VaccinationSession(
                campaign, center, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.schedule();
        session.start();
        sessionRepository.saveAndFlush(session);

        LocalDate tomorrow = today.plusDays(1);
        assertThrows(IllegalArgumentException.class, () ->
                campaignService.updateServiceDates(campaign.getId(), tomorrow, end)
        );
    }

    @Test
    @DisplayName("COMPLETED and CANCELLED historical sessions do not block date shortening")
    void completedAndCancelledSessionsDoNotBlockShortening() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(20);
        LocalDate end = today.plusDays(60);
        VaccinationCampaign campaign = createCampaign("CMP-REV-07", start, end);
        createAssignment(campaign, start, end);

        // Past COMPLETED session on today - 15
        LocalDate pastDate = today.minusDays(15);
        Instant pastStart = pastDate.atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant pastEnd = pastDate.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession completedSession = new VaccinationSession(
                campaign, center, pastStart, pastEnd, pastStart.minusSeconds(3600), "UTC"
        );
        completedSession.schedule();
        completedSession.start();
        completedSession.complete();
        sessionRepository.saveAndFlush(completedSession);

        // Future CANCELLED session on today + 5
        LocalDate futureDate = today.plusDays(5);
        Instant futureStart = futureDate.atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant futureEnd = futureDate.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession cancelledSession = new VaccinationSession(
                campaign, center, futureStart, futureEnd, futureStart.minusSeconds(3600), "UTC"
        );
        cancelledSession.transitionLifecycleStatus(SessionLifecycleStatus.CANCELLED);
        sessionRepository.saveAndFlush(cancelledSession);

        // Moving start to today + 10 should succeed because past COMPLETED and CANCELLED sessions do not block
        assertDoesNotThrow(() ->
                campaignService.updateServiceDates(campaign.getId(), today.plusDays(10), end)
        );
    }

    @Test
    @DisplayName("Session belonging to another campaign does not block date shortening")
    void sessionOnOtherCampaignDoesNotBlock() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(10);
        LocalDate end = today.plusDays(60);
        VaccinationCampaign campaignA = createCampaign("CMP-REV-08A", start, end);
        VaccinationCampaign campaignB = createCampaign("CMP-REV-08B", start, end);
        createAssignment(campaignA, start, end);
        createAssignment(campaignB, start, end);

        // Session on Campaign B on today + 5
        LocalDate sessionDate = today.plusDays(5);
        Instant sessionStart = sessionDate.atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant sessionEnd = sessionDate.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession sessionB = new VaccinationSession(
                campaignB, center, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        sessionB.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(sessionB);

        // Updating Campaign A service dates to start today + 10 should succeed
        assertDoesNotThrow(() ->
                campaignService.updateServiceDates(campaignA.getId(), today.plusDays(10), end)
        );
    }

    @Test
    @DisplayName("updateServiceDates rejects equal start and end dates with IllegalArgumentException")
    void updateServiceDates_equalStartAndEndDate_throwsIllegalArgumentException() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(10);
        LocalDate end = today.plusDays(60);
        VaccinationCampaign campaign = createCampaign("CMP-REV-09", start, end);
        createAssignment(campaign, start, end);

        LocalDate equalDate = today.plusDays(20);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> campaignService.updateServiceDates(campaign.getId(), equalDate, equalDate)
        );
        assertTrue(ex.getMessage().contains("must be after service start date"));
    }

    @ParameterizedTest(name = "{0} campaign service dates are immutable")
    @EnumSource(value = CampaignStatus.class, names = {"COMPLETED", "CANCELLED", "ARCHIVED"})
    @DisplayName("Operationally closed campaign service dates cannot be changed")
    void operationallyClosedCampaignServiceDatesCannotBeChanged(CampaignStatus status) {
        LocalDate today = LocalDate.now(clock);
        LocalDate originalStart = today.minusDays(10);
        LocalDate originalEnd = today.plusDays(60);
        VaccinationCampaign campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-REV-TERM-" + status,
                "Terminal campaign " + status,
                "Desc",
                vaccineType,
                targetGroup,
                originalStart,
                originalEnd,
                clock.instant().minusSeconds(3600),
                originalEnd.atTime(23, 59, 59).toInstant(ZoneOffset.UTC),
                status
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                campaignService.updateServiceDates(
                        campaign.getId(),
                        originalStart.minusDays(1),
                        originalEnd.plusDays(1)
                )
        );

        assertTrue(exception.getMessage().contains(status.name()));
        assertEquals(originalStart, campaign.getServiceStartDate());
        assertEquals(originalEnd, campaign.getServiceEndDate());
    }

    @Test
    @DisplayName("Existing invalid booking window prevents service date mutation")
    void invalidBookingWindowPreventsServiceDateMutation() {
        LocalDate today = LocalDate.now(clock);
        LocalDate originalStart = today.minusDays(10);
        LocalDate originalEnd = today.plusDays(60);
        VaccinationCampaign campaign = createCampaign("CMP-REV-BOOKING", originalStart, originalEnd);

        entityManager.createNativeQuery("""
                        UPDATE vaccination_campaigns
                        SET booking_close_at = booking_open_at
                        WHERE id = :campaignId
                        """)
                .setParameter("campaignId", campaign.getId())
                .executeUpdate();
        entityManager.clear();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                campaignService.updateServiceDates(
                        campaign.getId(),
                        originalStart.minusDays(1),
                        originalEnd.plusDays(1)
                )
        );

        assertTrue(exception.getMessage().contains("booking close time"));
        VaccinationCampaign unchanged = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(originalStart, unchanged.getServiceStartDate());
        assertEquals(originalEnd, unchanged.getServiceEndDate());
    }
}
