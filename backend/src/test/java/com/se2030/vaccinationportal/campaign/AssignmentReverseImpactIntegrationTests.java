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
import com.se2030.vaccinationportal.campaign.service.CampaignCenterAssignmentService;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying reverse configuration-impact rules for CampaignCenterAssignment mutations.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AssignmentReverseImpactIntegrationTests {

    @Autowired private CampaignCenterAssignmentService assignmentService;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private Clock clock;

    private VaccineType vaccineType;
    private TargetGroup targetGroup;
    private VaccinationCenter centerA;
    private VaccinationCenter centerB;

    @BeforeEach
    void setUp() {
        vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-REV-ASSIGN", "Rev Assignment Vaccine", "dose", true)
        );
        targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-REV-ASSIGN", "Rev Assignment Target", true)
        );
        centerA = centerRepository.saveAndFlush(
                new VaccinationCenter(
                        "CTR-REV-ASGN-A",
                        "Rev Assignment Center A",
                        "123 Main St",
                        "555-0199",
                        CenterStatus.ACTIVE,
                        50,
                        20,
                        "UTC"
                )
        );
        centerB = centerRepository.saveAndFlush(
                new VaccinationCenter(
                        "CTR-REV-ASGN-B",
                        "Rev Assignment Center B",
                        "456 Oak St",
                        "555-0299",
                        CenterStatus.ACTIVE,
                        50,
                        20,
                        "UTC"
                )
        );

        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(
                    new OperatingHours(
                            centerA,
                            day,
                            LocalTime.of(8, 0),
                            LocalTime.of(20, 0),
                            LocalDate.of(2026, 1, 1),
                            LocalDate.of(2026, 12, 31)
                    )
            );
            operatingHoursRepository.saveAndFlush(
                    new OperatingHours(
                            centerB,
                            day,
                            LocalTime.of(8, 0),
                            LocalTime.of(20, 0),
                            LocalDate.of(2026, 1, 1),
                            LocalDate.of(2026, 12, 31)
                    )
            );
        }
    }

    private VaccinationCampaign createCampaign(String code, LocalDate start, LocalDate end) {
        return campaignRepository.saveAndFlush(
                new VaccinationCampaign(
                        code,
                        "Campaign " + code,
                        "Desc",
                        vaccineType,
                        targetGroup,
                        start,
                        end,
                        Instant.parse("2026-05-01T08:00:00Z"),
                        Instant.parse("2026-12-31T18:00:00Z"),
                        CampaignStatus.ACTIVE
                )
        );
    }

    @Test
    @DisplayName("Shortening assignment active range excluding a dependent session is rejected")
    void shortenAssignmentExcludingDependentSessionThrowsException() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(10);
        LocalDate end = today.plusDays(60);

        VaccinationCampaign campaign = createCampaign("CMP-REV-ASG-01", today, today.plusDays(100));
        CampaignCenterAssignment assignment = assignmentService.createAssignment(campaign.getId(), centerA.getId(), start, end);

        // Session on start + 5 days
        Instant sessionStart = start.plusDays(5).atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant sessionEnd = start.plusDays(5).atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession session = new VaccinationSession(
                campaign, centerA, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Attempting to move assignment start to start + 10 days (past session date) must fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                assignmentService.updateFutureAssignment(assignment.getId(), start.plusDays(10), end)
        );
        assertTrue(ex.getMessage().contains("Cannot modify campaign-center assignment"));
    }

    @Test
    @DisplayName("Ending a currently effective assignment before dependent session date is rejected")
    void endAssignmentBeforeDependentSessionThrowsException() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(5);
        LocalDate end = today.plusDays(60);

        VaccinationCampaign campaign = createCampaign("CMP-REV-ASG-02", start, today.plusDays(100));
        CampaignCenterAssignment assignment = assignmentService.createAssignment(campaign.getId(), centerA.getId(), start, end);

        // Session on today + 10 days
        Instant sessionStart = today.plusDays(10).atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant sessionEnd = today.plusDays(10).atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession session = new VaccinationSession(
                campaign, centerA, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Attempting to end assignment on today + 5 days (before session on today + 10) must fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                assignmentService.endAssignment(assignment.getId(), today.plusDays(5))
        );
        assertTrue(ex.getMessage().contains("Cannot modify campaign-center assignment"));
    }

    @Test
    @DisplayName("Cancelling a future assignment with dependent sessions is rejected")
    void cancelFutureAssignmentWithDependentSessionThrowsException() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(10);
        LocalDate end = today.plusDays(60);

        VaccinationCampaign campaign = createCampaign("CMP-REV-ASG-03", today, today.plusDays(100));
        CampaignCenterAssignment assignment = assignmentService.createAssignment(campaign.getId(), centerA.getId(), start, end);

        // Session on start + 5 days
        Instant sessionStart = start.plusDays(5).atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant sessionEnd = start.plusDays(5).atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession session = new VaccinationSession(
                campaign, centerA, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Attempting to cancel assignment must fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                assignmentService.cancelFutureAssignment(assignment.getId())
        );
        assertTrue(ex.getMessage().contains("Cannot cancel campaign-center assignment"));
    }

    @Test
    @DisplayName("Safe assignment extension or update preserving session coverage is allowed")
    void safeAssignmentExtensionAllowed() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(10);
        LocalDate end = today.plusDays(60);

        VaccinationCampaign campaign = createCampaign("CMP-REV-ASG-04", today, today.plusDays(100));
        CampaignCenterAssignment assignment = assignmentService.createAssignment(campaign.getId(), centerA.getId(), start, end);

        Instant sessionStart = start.plusDays(5).atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant sessionEnd = start.plusDays(5).atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession session = new VaccinationSession(
                campaign, centerA, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Extending start to start-2 and end to end+10 is allowed
        assertDoesNotThrow(() ->
                assignmentService.updateFutureAssignment(assignment.getId(), start.plusDays(1), end.plusDays(10))
        );
        CampaignCenterAssignment updated = assignmentRepository.findById(assignment.getId()).orElseThrow();
        assertEquals(start.plusDays(1), updated.getActiveFrom());
        assertEquals(end.plusDays(10), updated.getActiveTo());
    }

    @Test
    @DisplayName("Exact half-open boundary assignment coverage [activeFrom, activeTo) is respected")
    void exactHalfOpenBoundaryRespected() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(10);
        LocalDate sessionDate = start.plusDays(5);
        // activeTo = sessionDate + 1 day
        LocalDate end = sessionDate.plusDays(1);

        VaccinationCampaign campaign = createCampaign("CMP-REV-ASG-05", today, today.plusDays(100));
        CampaignCenterAssignment assignment = assignmentService.createAssignment(campaign.getId(), centerA.getId(), start, end);

        Instant sessionStart = sessionDate.atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant sessionEnd = sessionDate.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession session = new VaccinationSession(
                campaign, centerA, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Session is on sessionDate. requiredToExclusive = sessionDate + 1. activeTo = sessionDate + 1.
        // sessionDate < activeTo (sessionDate < sessionDate + 1) -> valid!
        assertDoesNotThrow(() ->
                assignmentService.updateFutureAssignment(assignment.getId(), start, end)
        );

        // Ending assignment on sessionDate (activeTo = sessionDate) makes requiredToExclusive (sessionDate+1) > activeTo (sessionDate) -> fails!
        assertThrows(IllegalArgumentException.class, () ->
                assignmentService.updateFutureAssignment(assignment.getId(), start, sessionDate)
        );
    }

    @Test
    @DisplayName("IN_PROGRESS session blocks assignment shortening/cancellation")
    void inProgressSessionBlocksAssignmentMutation() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(5);
        LocalDate end = today.plusDays(30);

        VaccinationCampaign campaign = createCampaign("CMP-REV-ASG-06", start, today.plusDays(100));
        CampaignCenterAssignment assignment = assignmentService.createAssignment(campaign.getId(), centerA.getId(), start, end);

        Instant now = clock.instant();
        Instant sessionStart = now.minusSeconds(1800);
        Instant sessionEnd = now.plusSeconds(1800);
        VaccinationSession session = new VaccinationSession(
                campaign, centerA, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.IN_PROGRESS);
        sessionRepository.saveAndFlush(session);

        assertThrows(IllegalArgumentException.class, () ->
                assignmentService.updateFutureAssignment(assignment.getId(), today.plusDays(1), end)
        );

    }

    @Test
    @DisplayName("Past COMPLETED and CANCELLED sessions do not block assignment changes")
    void completedAndCancelledSessionsDoNotBlockAssignmentChanges() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(10);
        LocalDate end = today.plusDays(60);

        VaccinationCampaign campaign = createCampaign("CMP-REV-ASG-07", today, today.plusDays(100));
        CampaignCenterAssignment assignment = assignmentService.createAssignment(campaign.getId(), centerA.getId(), start, end);

        // Past COMPLETED session
        Instant pastStart = Instant.parse("2026-06-02T09:00:00Z");
        Instant pastEnd = Instant.parse("2026-06-02T12:00:00Z");
        VaccinationSession completedSession = new VaccinationSession(
                campaign, centerA, pastStart, pastEnd, pastStart.minusSeconds(3600), "UTC"
        );
        completedSession.transitionLifecycleStatus(SessionLifecycleStatus.COMPLETED);
        sessionRepository.saveAndFlush(completedSession);

        // Future CANCELLED session
        Instant futureStart = start.plusDays(5).atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant futureEnd = start.plusDays(5).atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession cancelledSession = new VaccinationSession(
                campaign, centerA, futureStart, futureEnd, futureStart.minusSeconds(3600), "UTC"
        );
        cancelledSession.transitionLifecycleStatus(SessionLifecycleStatus.CANCELLED);
        sessionRepository.saveAndFlush(cancelledSession);

        // Cancelling future assignment should succeed since no DRAFT/SCHEDULED/IN_PROGRESS session exists
        assertDoesNotThrow(() ->
                assignmentService.cancelFutureAssignment(assignment.getId())
        );
    }

    @Test
    @DisplayName("Session on unrelated center or campaign does not block assignment mutation")
    void sessionOnUnrelatedCenterOrCampaignDoesNotBlock() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(10);
        LocalDate end = today.plusDays(60);

        VaccinationCampaign campaign1 = createCampaign("CMP-REV-ASG-08A", today, today.plusDays(100));
        VaccinationCampaign campaign2 = createCampaign("CMP-REV-ASG-08B", today, today.plusDays(100));

        CampaignCenterAssignment assignment1A = assignmentService.createAssignment(campaign1.getId(), centerA.getId(), start, end);
        assignmentService.createAssignment(campaign2.getId(), centerB.getId(), start, end);

        // Session on (campaign2, centerB)
        Instant sessionStart = start.plusDays(5).atTime(9, 0).toInstant(java.time.ZoneOffset.UTC);
        Instant sessionEnd = start.plusDays(5).atTime(12, 0).toInstant(java.time.ZoneOffset.UTC);
        VaccinationSession session = new VaccinationSession(
                campaign2, centerB, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Cancelling assignment1A for (campaign1, centerA) should succeed
        assertDoesNotThrow(() ->
                assignmentService.cancelFutureAssignment(assignment1A.getId())
        );
    }
}
