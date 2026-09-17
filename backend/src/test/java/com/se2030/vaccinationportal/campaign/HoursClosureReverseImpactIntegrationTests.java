package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CenterClosureService;
import com.se2030.vaccinationportal.campaign.service.OperatingHoursService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying reverse configuration-impact rules for OperatingHours changes and CenterClosure creation/updates.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(HoursClosureReverseImpactIntegrationTests.FixedClockConfig.class)
class HoursClosureReverseImpactIntegrationTests {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-08-12T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired private OperatingHoursService hoursService;
    @Autowired private CenterClosureService closureService;
    @Autowired private OperatingHoursRepository hoursRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
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
                new VaccineType("VAX-REV-HOURS", "Rev Hours Vaccine", "dose", true)
        );
        targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-REV-HOURS", "Rev Hours Target", true)
        );

        center1 = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-HOURS-1", "Main Center", "123 Main St", "555-0199",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"
        ));
        center2 = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-HOURS-2", "Other Center", "456 Side St", "555-0198",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"
        ));

        LocalDate today = LocalDate.now(clock);
        campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-HOURS-1", "Hours Closure Campaign", "Desc", vaccineType, targetGroup,
                today.minusDays(10), today.plusDays(30),
                Instant.now(clock).minusSeconds(3600), today.plusDays(30).atTime(23, 59, 59).toInstant(ZoneOffset.UTC),
                CampaignStatus.ACTIVE
        ));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center1, today.minusDays(10), today.plusDays(30)
        ));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center2, today.minusDays(10), today.plusDays(30)
        ));
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

    // -------------------------------------------------------------------------
    // OperatingHours Reverse Protection Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateOperatingHours rejects shift shortening that exposes a dependent session end")
    void updateOperatingHours_rejects_whenShiftShortenedAcrossSession() {
        LocalDate today = LocalDate.now(clock);
        // Shift on MONDAY 08:00 - 17:00
        OperatingHours shift = hoursService.createOperatingHours(
                center1.getId(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                today.minusDays(5), today.plusDays(30)
        );

        LocalDate monday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        ZoneId zoneId = ZoneId.of(center1.getTimezone());
        Instant sessionStart = monday.atTime(9, 0).atZone(zoneId).toInstant();
        Instant sessionEnd = monday.atTime(16, 0).atZone(zoneId).toInstant();

        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.SCHEDULED);

        // Attempt to shorten closing time to 15:00 (session ends at 16:00)
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> hoursService.updateOperatingHours(
                        shift.getId(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(15, 0),
                        today.minusDays(5), today.plusDays(30)
                )
        );
        assertTrue(ex.getMessage().contains("Operating hours change leaves session end time"));
    }

    @Test
    @DisplayName("endDateOperatingHours rejects ending effective period before dependent session")
    void endDateOperatingHours_rejects_whenEndedBeforeSessionDate() {
        LocalDate today = LocalDate.now(clock);
        OperatingHours shift = hoursService.createOperatingHours(
                center1.getId(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                today.minusDays(5), today.plusDays(30)
        );

        LocalDate futureMonday = today.plusDays(14);
        while (futureMonday.getDayOfWeek() != DayOfWeek.MONDAY) {
            futureMonday = futureMonday.plusDays(1);
        }
        ZoneId zoneId = ZoneId.of(center1.getTimezone());
        Instant sessionStart = futureMonday.atTime(9, 0).atZone(zoneId).toInstant();
        Instant sessionEnd = futureMonday.atTime(16, 0).atZone(zoneId).toInstant();

        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.DRAFT);

        // Attempt to end shift prior to futureMonday (e.g., end on futureMonday minus 1 day)
        LocalDate newEndDate = futureMonday.minusDays(1);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> hoursService.endDateOperatingHours(shift.getId(), newEndDate)
        );
        assertTrue(ex.getMessage().contains("without operating hours"));
    }

    @Test
    @DisplayName("updateOperatingHours allows safe shift expansion")
    void updateOperatingHours_allows_safeExpansion() {
        LocalDate today = LocalDate.now(clock);
        OperatingHours shift = hoursService.createOperatingHours(
                center1.getId(), DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(16, 0),
                today.minusDays(5), today.plusDays(30)
        );

        LocalDate monday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        ZoneId zoneId = ZoneId.of(center1.getTimezone());
        Instant sessionStart = monday.atTime(9, 0).atZone(zoneId).toInstant();
        Instant sessionEnd = monday.atTime(16, 0).atZone(zoneId).toInstant();

        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.SCHEDULED);

        // Expand shift 08:00 - 18:00
        assertDoesNotThrow(() -> hoursService.updateOperatingHours(
                shift.getId(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0),
                today.minusDays(5), today.plusDays(30)
        ));
    }

    @Test
    @DisplayName("updateOperatingHours handles multi-shift contiguous coverage correctly")
    void updateOperatingHours_allows_multiShiftContiguousCoverage() {
        LocalDate today = LocalDate.now(clock);
        // Shift 1: 08:00 - 12:00
        OperatingHours shift1 = hoursService.createOperatingHours(
                center1.getId(), DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0),
                today.minusDays(5), today.plusDays(30)
        );
        // Shift 2: 12:00 - 17:00
        OperatingHours shift2 = hoursService.createOperatingHours(
                center1.getId(), DayOfWeek.TUESDAY, LocalTime.of(12, 0), LocalTime.of(17, 0),
                today.minusDays(5), today.plusDays(30)
        );

        LocalDate tuesday = today.with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        ZoneId zoneId = ZoneId.of(center1.getTimezone());
        Instant sessionStart = tuesday.atTime(9, 0).atZone(zoneId).toInstant();
        Instant sessionEnd = tuesday.atTime(16, 0).atZone(zoneId).toInstant();

        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.SCHEDULED);

        // Update shift 2 closing time (12:00 - 18:00) maintaining contiguity with shift 1 [08:00 - 12:00]
        assertDoesNotThrow(() -> hoursService.updateOperatingHours(
                shift2.getId(), DayOfWeek.TUESDAY, LocalTime.of(12, 0), LocalTime.of(18, 0),
                today.minusDays(5), today.plusDays(30)
        ));
    }

    @Test
    @DisplayName("updateOperatingHours rejects introducing a gap between shifts across session")
    void updateOperatingHours_rejects_gapIntroducedAcrossSession() {
        LocalDate today = LocalDate.now(clock);
        OperatingHours shift1 = hoursService.createOperatingHours(
                center1.getId(), DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0),
                today.minusDays(5), today.plusDays(30)
        );
        OperatingHours shift2 = hoursService.createOperatingHours(
                center1.getId(), DayOfWeek.WEDNESDAY, LocalTime.of(12, 0), LocalTime.of(17, 0),
                today.minusDays(5), today.plusDays(30)
        );

        LocalDate wednesday = today.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
        ZoneId zoneId = ZoneId.of(center1.getTimezone());
        Instant sessionStart = wednesday.atTime(9, 0).atZone(zoneId).toInstant();
        Instant sessionEnd = wednesday.atTime(16, 0).atZone(zoneId).toInstant();

        assertTrue(sessionStart.isAfter(clock.instant()), "Gap test requires a future session fixture");
        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.SCHEDULED);

        // Update shift 2 to start at 13:00 creating a 12:00 - 13:00 gap
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> hoursService.updateOperatingHours(
                        shift2.getId(), DayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(17, 0),
                        today.minusDays(5), today.plusDays(30)
                )
        );
        assertTrue(ex.getMessage().contains("introduces a gap in coverage"));
    }

    @Test
    @DisplayName("updateOperatingHours allows exact half-open boundary shift")
    void updateOperatingHours_allows_exactHalfOpenBoundaries() {
        LocalDate today = LocalDate.now(clock);
        OperatingHours shift = hoursService.createOperatingHours(
                center1.getId(), DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                today.minusDays(5), today.plusDays(30)
        );

        LocalDate thursday = today.with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
        ZoneId zoneId = ZoneId.of(center1.getTimezone());
        Instant sessionStart = thursday.atTime(8, 0).atZone(zoneId).toInstant();
        Instant sessionEnd = thursday.atTime(17, 0).atZone(zoneId).toInstant();

        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.SCHEDULED);

        // Update shift maintaining exact [08:00 - 17:00] boundary
        assertDoesNotThrow(() -> hoursService.updateOperatingHours(
                shift.getId(), DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                today.minusDays(5), today.plusDays(30)
        ));
    }

    @Test
    @DisplayName("updateOperatingHours ignores unrelated weekdays and terminal sessions")
    void updateOperatingHours_ignores_unrelatedWeekdayAndTerminalSessions() {
        LocalDate today = LocalDate.now(clock);
        OperatingHours fridayShift = hoursService.createOperatingHours(
                center1.getId(), DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                today.minusDays(5), today.plusDays(30)
        );

        OperatingHours mondayShift = hoursService.createOperatingHours(
                center1.getId(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                today.minusDays(5), today.plusDays(30)
        );

        LocalDate monday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        ZoneId zoneId = ZoneId.of(center1.getTimezone());
        Instant sessionStart = monday.atTime(9, 0).atZone(zoneId).toInstant();
        Instant sessionEnd = monday.atTime(16, 0).atZone(zoneId).toInstant();

        // Session is on Monday; mutating Friday shift should not be blocked by Monday session
        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.SCHEDULED);

        // Terminal/cancelled session on Friday
        LocalDate friday = today.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
        Instant friSessionStart = friday.atTime(9, 0).atZone(zoneId).toInstant();
        Instant friSessionEnd = friday.atTime(16, 0).atZone(zoneId).toInstant();
        createSession(center1, friSessionStart, friSessionEnd, SessionLifecycleStatus.CANCELLED);

        assertDoesNotThrow(() -> hoursService.updateOperatingHours(
                fridayShift.getId(), DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(12, 0),
                today.minusDays(5), today.plusDays(30)
        ));
    }

    // -------------------------------------------------------------------------
    // CenterClosure Reverse Protection Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createClosure rejects when active closure overlaps future DRAFT or SCHEDULED session")
    void createClosure_rejects_whenOverlappingFutureSession() {
        LocalDate today = LocalDate.now(clock);
        LocalDate futureDate = today.plusDays(5);
        ZoneId zoneId = ZoneId.of(center1.getTimezone());

        Instant sessionStart = futureDate.atTime(10, 0).atZone(zoneId).toInstant();
        Instant sessionEnd = futureDate.atTime(14, 0).atZone(zoneId).toInstant();

        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.SCHEDULED);

        // Overlapping closure: 11:00 to 13:00
        Instant closureStart = futureDate.atTime(11, 0).atZone(zoneId).toInstant();
        Instant closureEnd = futureDate.atTime(13, 0).atZone(zoneId).toInstant();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> closureService.createClosure(center1.getId(), closureStart, closureEnd, "Emergency Maintenance")
        );
        assertTrue(ex.getMessage().contains("Active center closure overlaps an active or scheduled session"));
    }

    @Test
    @DisplayName("updateFutureClosure rejects when update causes closure to overlap future session")
    void updateFutureClosure_rejects_whenUpdatedToOverlapSession() {
        LocalDate today = LocalDate.now(clock);
        LocalDate futureDate = today.plusDays(5);
        ZoneId zoneId = ZoneId.of(center1.getTimezone());

        Instant sessionStart = futureDate.atTime(10, 0).atZone(zoneId).toInstant();
        Instant sessionEnd = futureDate.atTime(14, 0).atZone(zoneId).toInstant();

        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.DRAFT);

        // Non-overlapping closure initially: 07:00 to 09:00
        Instant initClosureStart = futureDate.atTime(7, 0).atZone(zoneId).toInstant();
        Instant initClosureEnd = futureDate.atTime(9, 0).atZone(zoneId).toInstant();

        CenterClosure closure = closureService.createClosure(center1.getId(), initClosureStart, initClosureEnd, "Initial");

        // Attempt to update closure to 09:00 to 11:00 (overlaps session starting at 10:00)
        Instant updatedStart = futureDate.atTime(9, 0).atZone(zoneId).toInstant();
        Instant updatedEnd = futureDate.atTime(11, 0).atZone(zoneId).toInstant();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> closureService.updateFutureClosure(closure.getId(), updatedStart, updatedEnd, "Extended Maintenance")
        );
        assertTrue(ex.getMessage().contains("Active center closure overlaps an active or scheduled session"));
    }

    @Test
    @DisplayName("createClosure allows exact boundary-touching closure")
    void createClosure_allows_exactBoundaryTouch() {
        LocalDate today = LocalDate.now(clock);
        LocalDate futureDate = today.plusDays(5);
        ZoneId zoneId = ZoneId.of(center1.getTimezone());

        Instant sessionStart = futureDate.atTime(10, 0).atZone(zoneId).toInstant();
        Instant sessionEnd = futureDate.atTime(14, 0).atZone(zoneId).toInstant();

        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.SCHEDULED);

        // Closure ending exactly at sessionStart: 08:00 to 10:00
        Instant closure1Start = futureDate.atTime(8, 0).atZone(zoneId).toInstant();
        Instant closure1End = futureDate.atTime(10, 0).atZone(zoneId).toInstant();

        assertDoesNotThrow(() -> closureService.createClosure(center1.getId(), closure1Start, closure1End, "Morning Maintenance"));

        // Closure starting exactly at sessionEnd: 14:00 to 16:00
        Instant closure2Start = futureDate.atTime(14, 0).atZone(zoneId).toInstant();
        Instant closure2End = futureDate.atTime(16, 0).atZone(zoneId).toInstant();

        assertDoesNotThrow(() -> closureService.createClosure(center1.getId(), closure2Start, closure2End, "Afternoon Maintenance"));
    }

    @Test
    @DisplayName("createClosure rejects overlap with IN_PROGRESS session")
    void createClosure_rejects_overlapWithInProgressSession() {
        Instant now = clock.instant();
        Instant sessionStart = now.minusSeconds(1800);
        Instant sessionEnd = now.plusSeconds(1800);

        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.IN_PROGRESS);

        Instant closureStart = now.minusSeconds(900);
        Instant closureEnd = now.plusSeconds(900);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> closureService.createClosure(center1.getId(), closureStart, closureEnd, "Mid-session closure")
        );
        assertTrue(ex.getMessage().contains("Active center closure overlaps an active or scheduled session"));
    }

    @Test
    @DisplayName("createClosure ignores COMPLETED, CANCELLED, or unrelated center sessions")
    void createClosure_ignores_terminalAndUnrelatedCenterSessions() {
        LocalDate today = LocalDate.now(clock);
        LocalDate futureDate = today.plusDays(5);
        ZoneId zoneId = ZoneId.of(center1.getTimezone());

        Instant sessionStart = futureDate.atTime(10, 0).atZone(zoneId).toInstant();
        Instant sessionEnd = futureDate.atTime(14, 0).atZone(zoneId).toInstant();

        // Cancelled session on center1
        createSession(center1, sessionStart, sessionEnd, SessionLifecycleStatus.CANCELLED);

        // Active session on unrelated center2
        createSession(center2, sessionStart, sessionEnd, SessionLifecycleStatus.SCHEDULED);

        // Closure on center1 overlapping session time
        Instant closureStart = futureDate.atTime(11, 0).atZone(zoneId).toInstant();
        Instant closureEnd = futureDate.atTime(13, 0).atZone(zoneId).toInstant();

        CenterClosure created = assertDoesNotThrow(() ->
                closureService.createClosure(center1.getId(), closureStart, closureEnd, "Valid Closure")
        );
        assertNotNull(created);
    }
}
