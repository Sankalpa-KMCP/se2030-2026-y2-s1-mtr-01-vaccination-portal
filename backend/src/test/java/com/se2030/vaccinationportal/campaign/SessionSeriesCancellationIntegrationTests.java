package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SeriesOccurrenceCategory;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.RecurrenceGenerationResult;
import com.se2030.vaccinationportal.campaign.service.SeriesChangePreview;
import com.se2030.vaccinationportal.campaign.service.SeriesChangeResult;
import com.se2030.vaccinationportal.campaign.service.SeriesOccurrencePreviewItem;
import com.se2030.vaccinationportal.campaign.service.SessionChangePreview;
import com.se2030.vaccinationportal.campaign.service.SessionImpactReader;
import com.se2030.vaccinationportal.campaign.service.SessionImpactSummary;
import com.se2030.vaccinationportal.campaign.service.SessionOccurrenceGenerationService;
import com.se2030.vaccinationportal.campaign.service.SessionSeriesChangeService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import com.se2030.vaccinationportal.campaign.validation.VaccinationSessionValidator;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(SessionSeriesCancellationIntegrationTests.TestConfig.class)
class SessionSeriesCancellationIntegrationTests {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-01T00:00:00Z");

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }

    @Autowired private SessionSeriesChangeService seriesChangeService;
    @Autowired private SessionOccurrenceGenerationService generationService;
    @Autowired private VaccinationSessionService sessionService;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private VaccinationSessionValidator validator;
    @Autowired private Clock clock;

    private VaccinationCampaign campaign;
    private VaccinationCenter centerA;

    private static final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 12, 31);
    private static final Instant CAMPAIGN_REG_START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant CAMPAIGN_REG_END = Instant.parse("2026-12-30T00:00:00Z");
    private static final String TIMEZONE = "UTC";

    @BeforeEach
    void setUp() {
        sessionChangeEventRepository.deleteAllInBatch();
        sessionRepository.deleteAllInBatch();
        ruleRepository.deleteAllInBatch();
        closureRepository.deleteAllInBatch();
        operatingHoursRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
        centerRepository.deleteAllInBatch();
        vaccineTypeRepository.deleteAllInBatch();
        targetGroupRepository.deleteAllInBatch();

        VaccineType vt = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-CANCEL-TEST", "Series Cancel Vaccine"));
        TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-CANCEL-TEST", "Series Cancel Target Group"));

        campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-CANCEL-TEST", "Cancel Campaign", "Testing series cancel",
                vt, tg,
                CAMPAIGN_START, CAMPAIGN_END,
                CAMPAIGN_REG_START, CAMPAIGN_REG_END,
                CampaignStatus.DRAFT));

        centerA = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-CANCEL-A", "Center Alpha", "100 Alpha St", "+94 11 111 1111",
                CenterStatus.ACTIVE, 200, 100, TIMEZONE));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, centerA, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(centerA, day, LocalTime.of(8, 0), LocalTime.of(18, 0), CAMPAIGN_START.minusMonths(1), null));
        }
    }

    private SessionRecurrenceRule createAndMaterializeRule(
            LocalDate startDate,
            LocalDate endDate,
            Set<DayOfWeek> weekdays
    ) {
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign,
                centerA,
                weekdays,
                startDate,
                endDate,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                TIMEZONE
        ));
        generationService.generateOccurrences(rule.getId());
        return rule;
    }

    @Test
    @DisplayName("previewSeriesCancellation validates input parameters and rule state")
    void testPreviewSeriesCancellationValidation() {
        assertThatThrownBy(() -> seriesChangeService.previewSeriesCancellation(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session recurrence rule ID is required");

        assertThatThrownBy(() -> seriesChangeService.previewSeriesCancellation(999999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session recurrence rule not found with ID: 999999");

        SessionRecurrenceRule rule = createAndMaterializeRule(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 16),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));

        rule.cancel();
        ruleRepository.saveAndFlush(rule);

        assertThatThrownBy(() -> seriesChangeService.previewSeriesCancellation(rule.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session recurrence rule must be ACTIVE to preview cancellation");
    }

    @Test
    @DisplayName("previewSeriesCancellation classifies all eligible future DRAFT sessions as REMOVED")
    void testPreviewSeriesCancellationAllDraft() {
        SessionRecurrenceRule rule = createAndMaterializeRule(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 16),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));

        SeriesChangePreview preview = seriesChangeService.previewSeriesCancellation(rule.getId());

        assertThat(preview).isNotNull();
        assertThat(preview.ruleId()).isEqualTo(rule.getId());
        assertThat(preview.totalAffectedAppointmentsCount()).isEqualTo(0);
        assertThat(preview.totalAffectedWaitlistCount()).isEqualTo(0);
        assertThat(preview.previewToken()).isNotBlank();
        assertThat(preview.occurrenceEntries()).hasSize(4);

        for (SeriesOccurrencePreviewItem item : preview.occurrenceEntries()) {
            assertThat(item.category()).isEqualTo(SeriesOccurrenceCategory.REMOVED);
            assertThat(item.sessionId()).isNotNull();
            assertThat(item.currentSnapshot().getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.DRAFT);
            assertThat(item.proposedSnapshot().getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.CANCELLED);
            assertThat(item.affectedAppointmentCount()).isEqualTo(0);
            assertThat(item.affectedWaitlistCount()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("previewSeriesCancellation queries SessionImpactReader for SCHEDULED sessions")
    void testPreviewSeriesCancellationWithScheduledImpact() {
        SessionRecurrenceRule rule = createAndMaterializeRule(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 16),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));

        List<VaccinationSession> sessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        assertThat(sessions).hasSize(4);

        // Transition 2 sessions to SCHEDULED
        sessions.get(0).transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessions.get(1).transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAllAndFlush(sessions);

        Map<Long, SessionImpactSummary> impactMap = new HashMap<>();
        impactMap.put(sessions.get(0).getId(), new SessionImpactSummary(5, 2));
        impactMap.put(sessions.get(1).getId(), new SessionImpactSummary(8, 3));

        SessionImpactReader impactReader = sessionId -> impactMap.getOrDefault(sessionId, new SessionImpactSummary(0, 0));

        SessionSeriesChangeService customService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, impactReader, clock);

        SeriesChangePreview preview = customService.previewSeriesCancellation(rule.getId());

        assertThat(preview.totalAffectedAppointmentsCount()).isEqualTo(13);
        assertThat(preview.totalAffectedWaitlistCount()).isEqualTo(5);

        List<SeriesOccurrencePreviewItem> items = preview.occurrenceEntries();
        assertThat(items.get(0).affectedAppointmentCount()).isEqualTo(5);
        assertThat(items.get(0).affectedWaitlistCount()).isEqualTo(2);
        assertThat(items.get(1).affectedAppointmentCount()).isEqualTo(8);
        assertThat(items.get(1).affectedWaitlistCount()).isEqualTo(3);
        assertThat(items.get(2).affectedAppointmentCount()).isEqualTo(0);
        assertThat(items.get(2).affectedWaitlistCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("previewSeriesCancellation throws when SCHEDULED session exists but SessionImpactReader is missing")
    void testPreviewSeriesCancellationMissingImpactReaderForScheduled() {
        SessionRecurrenceRule rule = createAndMaterializeRule(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 16),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));

        List<VaccinationSession> sessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        sessions.get(0).transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAllAndFlush(sessions);

        SessionSeriesChangeService noReaderService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, null, clock);

        assertThatThrownBy(() -> noReaderService.previewSeriesCancellation(rule.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Session impact reader is not available to assess SCHEDULED session changes");
    }

    @Test
    @DisplayName("previewSeriesCancellation preserves individual exceptions and elapsed occurrences")
    void testPreviewSeriesCancellationPreservedAndIneligible() {
        SessionRecurrenceRule rule = createAndMaterializeRule(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 16),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));

        List<VaccinationSession> sessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        assertThat(sessions).hasSize(6);

        // Occurrence 0 (Oct 5): Individually rescheduled -> PRESERVED_EXCEPTION
        VaccinationSession s0 = sessions.get(0);
        SessionChangePreview indivPreview = sessionService.previewRescheduleOrTransfer(
                s0.getId(), centerA.getId(),
                Instant.parse("2026-10-05T10:00:00Z"), Instant.parse("2026-10-05T13:00:00Z"),
                Instant.parse("2026-10-05T10:00:00Z"));
        sessionService.confirmRescheduleOrTransfer(
                s0.getId(), centerA.getId(),
                Instant.parse("2026-10-05T10:00:00Z"), Instant.parse("2026-10-05T13:00:00Z"),
                Instant.parse("2026-10-05T10:00:00Z"),
                indivPreview.previewToken(), "Individual adjustment", "Dr. Jane");

        // Occurrence 1 (Oct 7): Ineligible due to elapsed past time
        VaccinationSession s1 = sessions.get(1);

        // Advance clock past Oct 7 session
        Clock customClock = Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC);
        SessionSeriesChangeService customService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, null, customClock);

        SeriesChangePreview preview = customService.previewSeriesCancellation(rule.getId());

        List<SeriesOccurrencePreviewItem> items = preview.occurrenceEntries();
        assertThat(items).hasSize(6);

        // Oct 5 is PRESERVED_EXCEPTION
        assertThat(items.get(0).category()).isEqualTo(SeriesOccurrenceCategory.PRESERVED_EXCEPTION);
        assertThat(items.get(0).sessionId()).isEqualTo(s0.getId());

        // Oct 7 is INELIGIBLE (elapsed)
        assertThat(items.get(1).category()).isEqualTo(SeriesOccurrenceCategory.INELIGIBLE);
        assertThat(items.get(1).sessionId()).isEqualTo(s1.getId());

        // Remaining 4 future sessions are REMOVED
        for (int i = 2; i < 6; i++) {
            assertThat(items.get(i).category()).isEqualTo(SeriesOccurrenceCategory.REMOVED);
        }
    }

    @Test
    @DisplayName("confirmSeriesCancellation successfully cancels rule and future sessions with audit trail")
    void testConfirmSeriesCancellationSuccess() {
        SessionRecurrenceRule rule = createAndMaterializeRule(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 16),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));

        List<VaccinationSession> initialSessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        assertThat(initialSessions).hasSize(4);

        SeriesChangePreview preview = seriesChangeService.previewSeriesCancellation(rule.getId());

        SeriesChangeResult result = seriesChangeService.confirmSeriesCancellation(
                rule.getId(), preview.previewToken(), "Series cancelled due to center closure", "Admin Joe");

        assertThat(result).isNotNull();
        assertThat(result.rule().getStatus()).isEqualTo(SessionRecurrenceRuleStatus.CANCELLED);
        assertThat(result.rescheduledCount()).isEqualTo(0);
        assertThat(result.cancelledCount()).isEqualTo(4);
        assertThat(result.newCount()).isEqualTo(0);
        assertThat(result.preservedCount()).isEqualTo(0);
        assertThat(result.ineligibleCount()).isEqualTo(0);
        assertThat(result.eventsCreatedCount()).isEqualTo(4);

        // Verify DB persistence of rule
        SessionRecurrenceRule updatedRule = ruleRepository.findById(rule.getId()).orElseThrow();
        assertThat(updatedRule.getStatus()).isEqualTo(SessionRecurrenceRuleStatus.CANCELLED);

        // Verify DB persistence of sessions
        List<VaccinationSession> updatedSessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        assertThat(updatedSessions).hasSize(4);
        for (VaccinationSession s : updatedSessions) {
            assertThat(s.getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.CANCELLED);
            assertThat(s.getRecurrenceRule()).isNotNull();
            assertThat(s.getRecurrenceRule().getId()).isEqualTo(rule.getId());
        }

        // Verify DB persistence of audit events
        List<SessionChangeEvent> events = sessionChangeEventRepository.findAll();
        assertThat(events).hasSize(4);
        for (SessionChangeEvent e : events) {
            assertThat(e.getChangeType()).isEqualTo(SessionChangeType.SERIES_CANCELLED);
            assertThat(e.getReason()).isEqualTo("Series cancelled due to center closure");
            assertThat(e.getActorId()).isEqualTo("Admin Joe");
            assertThat(e.getOldSnapshot().getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.DRAFT);
            assertThat(e.getNewSnapshot().getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.CANCELLED);
        }
    }

    @Test
    @DisplayName("confirmSeriesCancellation rejects stale preview token")
    void testConfirmSeriesCancellationStaleToken() {
        SessionRecurrenceRule rule = createAndMaterializeRule(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 16),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));

        SeriesChangePreview preview = seriesChangeService.previewSeriesCancellation(rule.getId());

        // Modify one session behind the preview's back
        List<VaccinationSession> sessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        sessions.get(0).transitionLifecycleStatus(SessionLifecycleStatus.CANCELLED);
        sessionRepository.saveAndFlush(sessions.get(0));

        assertThatThrownBy(() -> seriesChangeService.confirmSeriesCancellation(
                rule.getId(), preview.previewToken(), "Reason", "Admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Preview is stale or session state has changed");
    }

    @Test
    @DisplayName("confirmSeriesCancellation validates mandatory parameters")
    void testConfirmSeriesCancellationParameterValidation() {
        SessionRecurrenceRule rule = createAndMaterializeRule(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 16),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));

        SeriesChangePreview preview = seriesChangeService.previewSeriesCancellation(rule.getId());
        String token = preview.previewToken();

        assertThatThrownBy(() -> seriesChangeService.confirmSeriesCancellation(null, token, "Reason", "Admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session recurrence rule ID is required");

        assertThatThrownBy(() -> seriesChangeService.confirmSeriesCancellation(rule.getId(), null, "Reason", "Admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Preview token is required");

        assertThatThrownBy(() -> seriesChangeService.confirmSeriesCancellation(rule.getId(), "  ", "Reason", "Admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Preview token is required");

        assertThatThrownBy(() -> seriesChangeService.confirmSeriesCancellation(rule.getId(), token, null, "Admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reason is required");

        assertThatThrownBy(() -> seriesChangeService.confirmSeriesCancellation(rule.getId(), token, "a".repeat(501), "Admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reason cannot exceed 500 characters");

        assertThatThrownBy(() -> seriesChangeService.confirmSeriesCancellation(rule.getId(), token, "Reason", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Actor ID is required");

        assertThatThrownBy(() -> seriesChangeService.confirmSeriesCancellation(rule.getId(), token, "Reason", "a".repeat(65)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Actor ID cannot exceed 64 characters");
    }

    @Test
    @DisplayName("confirmSeriesCancellation handles rule with zero generated sessions cleanly")
    void testConfirmSeriesCancellationZeroSessions() {
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, Set.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 16),
                LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE
        ));

        SeriesChangePreview preview = seriesChangeService.previewSeriesCancellation(rule.getId());
        assertThat(preview.occurrenceEntries()).isEmpty();

        SeriesChangeResult result = seriesChangeService.confirmSeriesCancellation(
                rule.getId(), preview.previewToken(), "Cancelled before generation", "Admin");

        assertThat(result.rule().getStatus()).isEqualTo(SessionRecurrenceRuleStatus.CANCELLED);
        assertThat(result.cancelledCount()).isEqualTo(0);
        assertThat(result.eventsCreatedCount()).isEqualTo(0);

        SessionRecurrenceRule updatedRule = ruleRepository.findById(rule.getId()).orElseThrow();
        assertThat(updatedRule.getStatus()).isEqualTo(SessionRecurrenceRuleStatus.CANCELLED);
    }

    @Test
    @DisplayName("generateOccurrences rejects generation on a cancelled recurrence rule")
    void testGenerateOccurrencesRejectsCancelledRule() {
        SessionRecurrenceRule rule = createAndMaterializeRule(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 16),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));

        SeriesChangePreview preview = seriesChangeService.previewSeriesCancellation(rule.getId());
        seriesChangeService.confirmSeriesCancellation(
                rule.getId(), preview.previewToken(), "Cancellation", "Admin");

        assertThatThrownBy(() -> generationService.generateOccurrences(rule.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session recurrence rule must be ACTIVE to generate occurrences");
    }
}
