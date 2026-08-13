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
import com.se2030.vaccinationportal.campaign.service.VaccinationCenterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying reverse configuration-impact protection rules for VaccinationCenter deactivation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CenterReverseImpactIntegrationTests {

    @Autowired private VaccinationCenterService centerService;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private Clock clock;

    private VaccineType vaccineType;
    private TargetGroup targetGroup;
    private VaccinationCenter center1;
    private VaccinationCenter center2;
    private VaccinationCampaign campaign;

    @BeforeEach
    void setUp() {
        vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-REV-CTR", "Rev Center Vaccine", "dose", true)
        );
        targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-REV-CTR", "Rev Center Target", true)
        );

        center1 = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-REV-1", "Main Center", "123 Main St", "555-0199",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"
        ));
        center2 = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-REV-2", "Secondary Center", "456 Side St", "555-0198",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"
        ));

        LocalDate today = LocalDate.now(clock.getZone());
        campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-REV-CTR", "Rev Center Campaign", "Desc", vaccineType, targetGroup,
                today.minusDays(10), today.plusDays(30),
                Instant.now(clock).minusSeconds(3600), Instant.now(clock).plusSeconds(86400 * 180),
                CampaignStatus.ACTIVE
        ));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center1, today.minusDays(10), today.plusDays(30)
        ));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center2, today.minusDays(10), today.plusDays(30)
        ));

        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center1, day, LocalTime.of(8, 0), LocalTime.of(17, 0),
                    today.minusDays(10), today.plusDays(30)
            ));
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center2, day, LocalTime.of(8, 0), LocalTime.of(17, 0),
                    today.minusDays(10), today.plusDays(30)
            ));
        }
    }

    private VaccinationSession createSession(VaccinationCenter targetCenter, Instant startAt, Instant endAt, SessionLifecycleStatus status) {
        VaccinationSession session = new VaccinationSession(
                campaign, targetCenter, startAt, endAt, startAt.minusSeconds(3600), targetCenter.getTimezone()
        );
        if (status != SessionLifecycleStatus.DRAFT) {
            if (status == SessionLifecycleStatus.SCHEDULED) {
                session.schedule();
            } else if (status == SessionLifecycleStatus.IN_PROGRESS) {
                session.schedule();
                session.start();
            } else if (status == SessionLifecycleStatus.COMPLETED) {
                session.schedule();
                session.start();
                session.complete();
            } else if (status == SessionLifecycleStatus.CANCELLED) {
                session.cancel();
            }
        }
        return sessionRepository.saveAndFlush(session);
    }

    @Test
    @DisplayName("deactivateCenter rejects when future DRAFT session depends on center")
    void deactivateCenter_rejects_whenFutureDraftSessionExists() {
        ZoneId zoneId = ZoneId.of(center1.getTimezone());
        Instant futureStart = LocalDate.now(clock.getZone()).plusDays(5).atTime(9, 0).atZone(zoneId).toInstant();
        Instant futureEnd = futureStart.plusSeconds(3600);

        createSession(center1, futureStart, futureEnd, SessionLifecycleStatus.DRAFT);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> centerService.deactivateCenter(center1.getId())
        );
        assertTrue(ex.getMessage().contains("Cannot deactivate vaccination center"));
        assertEquals(CenterStatus.ACTIVE, centerRepository.findById(center1.getId()).orElseThrow().getCenterStatus());
    }

    @Test
    @DisplayName("deactivateCenter rejects when future SCHEDULED session depends on center")
    void deactivateCenter_rejects_whenFutureScheduledSessionExists() {
        ZoneId zoneId = ZoneId.of(center1.getTimezone());
        Instant futureStart = LocalDate.now(clock.getZone()).plusDays(5).atTime(10, 0).atZone(zoneId).toInstant();
        Instant futureEnd = futureStart.plusSeconds(3600);

        createSession(center1, futureStart, futureEnd, SessionLifecycleStatus.SCHEDULED);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> centerService.deactivateCenter(center1.getId())
        );
        assertTrue(ex.getMessage().contains("Cannot deactivate vaccination center"));
    }

    @Test
    @DisplayName("deactivateCenter rejects when IN_PROGRESS session depends on center")
    void deactivateCenter_rejects_whenInProgressSessionExists() {
        ZoneId zoneId = ZoneId.of(center1.getTimezone());
        Instant now = clock.instant();
        Instant sessionStart = now.minusSeconds(1800);
        Instant sessionEnd = now.plusSeconds(1800);

        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.IN_PROGRESS);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> centerService.deactivateCenter(center1.getId())
        );
        assertTrue(ex.getMessage().contains("Cannot deactivate vaccination center"));
    }

    @Test
    @DisplayName("deactivateCenter succeeds when only COMPLETED, CANCELLED, or elapsed sessions exist")
    void deactivateCenter_succeeds_whenOnlyTerminalOrElapsedSessionsExist() {
        ZoneId zoneId = ZoneId.of(center1.getTimezone());
        Instant pastStart = LocalDate.now(clock.getZone()).minusDays(5).atTime(9, 0).atZone(zoneId).toInstant();
        Instant pastEnd = pastStart.plusSeconds(3600);

        createSession(center1, pastStart, pastEnd, SessionLifecycleStatus.COMPLETED);

        Instant futureStart = LocalDate.now(clock.getZone()).plusDays(5).atTime(9, 0).atZone(zoneId).toInstant();
        Instant futureEnd = futureStart.plusSeconds(3600);

        createSession(center1, futureStart, futureEnd, SessionLifecycleStatus.CANCELLED);

        assertDoesNotThrow(() -> centerService.deactivateCenter(center1.getId()));
        assertEquals(CenterStatus.INACTIVE, centerRepository.findById(center1.getId()).orElseThrow().getCenterStatus());
    }

    @Test
    @DisplayName("deactivateCenter succeeds when session is on an unrelated center")
    void deactivateCenter_succeeds_whenSessionIsOnUnrelatedCenter() {
        ZoneId zoneId = ZoneId.of(center2.getTimezone());
        Instant futureStart = LocalDate.now(clock.getZone()).plusDays(5).atTime(9, 0).atZone(zoneId).toInstant();
        Instant futureEnd = futureStart.plusSeconds(3600);

        createSession(center2, futureStart, futureEnd, SessionLifecycleStatus.SCHEDULED);

        assertDoesNotThrow(() -> centerService.deactivateCenter(center1.getId()));
        assertEquals(CenterStatus.INACTIVE, centerRepository.findById(center1.getId()).orElseThrow().getCenterStatus());
        assertEquals(CenterStatus.ACTIVE, centerRepository.findById(center2.getId()).orElseThrow().getCenterStatus());
    }
}
