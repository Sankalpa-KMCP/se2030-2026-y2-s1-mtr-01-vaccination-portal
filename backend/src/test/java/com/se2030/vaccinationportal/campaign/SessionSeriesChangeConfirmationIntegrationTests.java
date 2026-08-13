package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.RecurrenceRuleDefinitionSnapshot;
import com.se2030.vaccinationportal.campaign.model.SeriesOccurrenceCategory;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
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
import java.util.List;
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
@Import(SessionSeriesChangeConfirmationIntegrationTests.TestConfig.class)
class SessionSeriesChangeConfirmationIntegrationTests {

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
    private VaccinationCenter centerB;

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

        VaccineType vt = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-CONFIRM-TEST", "Series Confirm Vaccine"));
        TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-CONFIRM-TEST", "Series Confirm Target Group"));

        campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-CONFIRM-TEST", "Confirm Campaign", "Testing series confirm",
                vt, tg,
                CAMPAIGN_START, CAMPAIGN_END,
                CAMPAIGN_REG_START, CAMPAIGN_REG_END,
                CampaignStatus.DRAFT));

        centerA = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-CONFIRM-A", "Center Alpha", "100 Alpha St", "+94 11 111 1111",
                CenterStatus.ACTIVE, 200, 100, TIMEZONE));

        centerB = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-CONFIRM-B", "Center Beta", "200 Beta St", "+94 11 222 2222",
                CenterStatus.ACTIVE, 300, 150, TIMEZONE));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, centerA, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, centerB, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(centerA, day, LocalTime.of(8, 0), LocalTime.of(18, 0), CAMPAIGN_START.minusMonths(1), null));
            operatingHoursRepository.saveAndFlush(new OperatingHours(centerB, day, LocalTime.of(8, 0), LocalTime.of(18, 0), CAMPAIGN_START.minusMonths(1), null));
        }
    }

    @Test
    @DisplayName("confirmSeriesChange: successful weekday change reschedules retained, cancels removed, and creates new sessions")
    void confirmSeriesChange_successfulWeekdayChange() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        Set<DayOfWeek> newWeekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), newWeekdays, startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE);

        SeriesChangeResult result = seriesChangeService.confirmSeriesChange(
                rule.getId(), newWeekdays, startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE,
                preview.previewToken(), "Updating weekdays to MON/FRI", "admin-user-1");

        assertThat(result.rule().getWeekdays()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        assertThat(result.rescheduledCount()).isEqualTo(0);
        assertThat(result.cancelledCount()).isGreaterThan(0);
        assertThat(result.newCount()).isGreaterThan(0);

        List<VaccinationSession> allSessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        List<VaccinationSession> wedSessions = allSessions.stream()
                .filter(s -> s.getOriginOccurrenceDate().getDayOfWeek() == DayOfWeek.WEDNESDAY)
                .toList();
        assertThat(wedSessions).allMatch(s -> s.getLifecycleStatus() == SessionLifecycleStatus.CANCELLED);

        List<VaccinationSession> friSessions = allSessions.stream()
                .filter(s -> s.getOriginOccurrenceDate().getDayOfWeek() == DayOfWeek.FRIDAY)
                .toList();
        assertThat(friSessions).isNotEmpty().allMatch(s -> s.getLifecycleStatus() == SessionLifecycleStatus.DRAFT);

        List<SessionChangeEvent> events = sessionChangeEventRepository.findAll();
        assertThat(events).isNotEmpty().allMatch(e -> e.getChangeType() == SessionChangeType.SERIES_CANCELLED);
    }

    @Test
    @DisplayName("confirmSeriesChange: successful time change reschedules all retained sessions and emits SERIES_RESCHEDULED events")
    void confirmSeriesChange_successfulTimeChange() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 19);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        LocalTime newStart = LocalTime.of(10, 0);
        LocalTime newEnd = LocalTime.of(13, 0);

        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, endDate, newStart, newEnd, TIMEZONE);

        SeriesChangeResult result = seriesChangeService.confirmSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, endDate, newStart, newEnd, TIMEZONE,
                preview.previewToken(), "Pushing start time back by 1 hour", "ops-lead");

        assertThat(result.rescheduledCount()).isEqualTo(3);
        assertThat(result.eventsCreatedCount()).isEqualTo(3);

        List<VaccinationSession> sessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        for (VaccinationSession s : sessions) {
            assertThat(s.getStartAt()).isEqualTo(Instant.parse(s.getOriginOccurrenceDate() + "T10:00:00Z"));
            assertThat(s.getEndAt()).isEqualTo(Instant.parse(s.getOriginOccurrenceDate() + "T13:00:00Z"));
            assertThat(s.getBookingCloseAt()).isEqualTo(s.getStartAt());
        }

        List<SessionChangeEvent> events = sessionChangeEventRepository.findAll();
        assertThat(events).hasSize(3).allMatch(e -> e.getChangeType() == SessionChangeType.SERIES_RESCHEDULED);
        assertThat(events.get(0).getReason()).isEqualTo("Pushing start time back by 1 hour");
        assertThat(events.get(0).getActorId()).isEqualTo("ops-lead");
    }

    @Test
    @DisplayName("confirmSeriesChange: range extension creates NEW DRAFT occurrences without touching existing ones")
    void confirmSeriesChange_rangeExtensionCreatesNewOccurrences() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 12);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        LocalDate extendedEndDate = LocalDate.of(2026, 10, 26);

        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, extendedEndDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE);

        SeriesChangeResult result = seriesChangeService.confirmSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, extendedEndDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE,
                preview.previewToken(), "Extending campaign recurrence range", "planner-1");

        assertThat(result.newCount()).isEqualTo(2);
        assertThat(result.eventsCreatedCount()).isEqualTo(0);

        List<VaccinationSession> sessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        assertThat(sessions).hasSize(4);
    }

    @Test
    @DisplayName("confirmSeriesChange: range shortening cancels REMOVED occurrences and emits SERIES_CANCELLED events")
    void confirmSeriesChange_rangeShorteningCancelsRemovedOccurrences() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 26);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        LocalDate shortenedEndDate = LocalDate.of(2026, 10, 12);

        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, shortenedEndDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE);

        SeriesChangeResult result = seriesChangeService.confirmSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, shortenedEndDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE,
                preview.previewToken(), "Shortening series duration", "planner-1");

        assertThat(result.cancelledCount()).isEqualTo(2);
        assertThat(result.eventsCreatedCount()).isEqualTo(2);

        List<SessionChangeEvent> events = sessionChangeEventRepository.findAll();
        assertThat(events).hasSize(2).allMatch(e -> e.getChangeType() == SessionChangeType.SERIES_CANCELLED);
    }

    @Test
    @DisplayName("confirmSeriesChange: retained occurrence identity and provenance are strictly preserved")
    void confirmSeriesChange_retainedIdentityPreserved() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 12);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        VaccinationSession original = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId()).get(0);
        String originalKey = original.getOccurrenceKey();
        LocalDate originalOriginDate = original.getOriginOccurrenceDate();

        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, endDate, LocalTime.of(10, 0), LocalTime.of(13, 0), TIMEZONE);

        seriesChangeService.confirmSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, endDate, LocalTime.of(10, 0), LocalTime.of(13, 0), TIMEZONE,
                preview.previewToken(), "Time shift", "admin-1");

        VaccinationSession updated = sessionRepository.findById(original.getId()).orElseThrow();
        assertThat(updated.getRecurrenceRule().getId()).isEqualTo(rule.getId());
        assertThat(updated.getOccurrenceKey()).isEqualTo(originalKey);
        assertThat(updated.getOriginOccurrenceDate()).isEqualTo(originalOriginDate);
    }

    @Test
    @DisplayName("confirmSeriesChange: preserved individual exception is ignored and untouched by series change")
    void confirmSeriesChange_preservedIndividualException() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 19);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        List<VaccinationSession> sessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        VaccinationSession targetSession = sessions.get(1); // Oct 12 session

        Instant newStart = Instant.parse("2026-10-12T14:00:00Z");
        Instant newEnd = Instant.parse("2026-10-12T17:00:00Z");
        SessionChangePreview p = sessionService.previewReschedule(targetSession.getId(), newStart, newEnd, newStart);
        sessionService.confirmReschedule(targetSession.getId(), newStart, newEnd, newStart, p.previewToken(), "Individual conflict", "doctor-1");

        LocalTime seriesNewStart = LocalTime.of(10, 0);
        LocalTime seriesNewEnd = LocalTime.of(13, 0);

        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, endDate, seriesNewStart, seriesNewEnd, TIMEZONE);

        SeriesChangeResult result = seriesChangeService.confirmSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, endDate, seriesNewStart, seriesNewEnd, TIMEZONE,
                preview.previewToken(), "Series shift", "admin-1");

        assertThat(result.preservedCount()).isEqualTo(1);

        VaccinationSession overriddenSession = sessionRepository.findById(targetSession.getId()).orElseThrow();
        assertThat(overriddenSession.getStartAt()).isEqualTo(newStart);
        assertThat(overriddenSession.getEndAt()).isEqualTo(newEnd);
    }

    @Test
    @DisplayName("confirmSeriesChange: rejects when preview token is stale or invalid")
    void confirmSeriesChange_staleTokenRejection() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 12);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        assertThatThrownBy(() -> seriesChangeService.confirmSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, endDate, LocalTime.of(10, 0), LocalTime.of(13, 0), TIMEZONE,
                "stale-invalid-token", "Testing stale token", "admin-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Preview is stale");
    }

    @Test
    @DisplayName("confirmSeriesChange: validator failure causes complete atomic transaction rollback")
    void confirmSeriesChange_validatorFailureRollback() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 12);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        closureRepository.saveAndFlush(new CenterClosure(centerA, Instant.parse("2026-10-12T00:00:00Z"), Instant.parse("2026-10-12T23:59:59Z"), "Maintenance"));

        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY), startDate, endDate, LocalTime.of(10, 0), LocalTime.of(13, 0), TIMEZONE);

        assertThatThrownBy(() -> seriesChangeService.previewSeriesChange(rule.getId(), proposedDef))
                .isInstanceOf(IllegalArgumentException.class);

        SessionRecurrenceRule untouchedRule = ruleRepository.findById(rule.getId()).orElseThrow();
        assertThat(untouchedRule.getStartTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    @DisplayName("confirmSeriesChange: SCHEDULED session records real impact counts in SessionChangeEvent")
    void confirmSeriesChange_scheduledRealImpactCounts() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 5);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        VaccinationSession session = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId()).get(0);
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        SessionImpactReader mockImpactReader = sid -> new SessionImpactSummary(5, 2);
        SessionSeriesChangeService customService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, mockImpactReader, clock);

        LocalTime newStart = LocalTime.of(10, 0);
        LocalTime newEnd = LocalTime.of(13, 0);

        SeriesChangePreview preview = customService.previewSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, endDate, newStart, newEnd, TIMEZONE);

        SeriesChangeResult result = customService.confirmSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY), startDate, endDate, newStart, newEnd, TIMEZONE,
                preview.previewToken(), "Rescheduling scheduled session", "admin-1");

        assertThat(result.eventsCreatedCount()).isEqualTo(1);
        SessionChangeEvent event = sessionChangeEventRepository.findAll().get(0);
        assertThat(event.getAffectedAppointmentCount()).isEqualTo(5);
        assertThat(event.getAffectedWaitlistCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("confirmSeriesChange: recurrence generator retry after confirmation creates 0 duplicate sessions")
    void confirmSeriesChange_generatorCompatibilityAfterConfirmation() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 19);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        Set<DayOfWeek> newWeekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), newWeekdays, startDate, endDate, LocalTime.of(10, 0), LocalTime.of(13, 0), TIMEZONE);

        seriesChangeService.confirmSeriesChange(
                rule.getId(), newWeekdays, startDate, endDate, LocalTime.of(10, 0), LocalTime.of(13, 0), TIMEZONE,
                preview.previewToken(), "Shifting time and adding Friday", "admin-1");

        int sessionCountAfterConfirm = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId()).size();

        RecurrenceGenerationResult genResult = generationService.generateOccurrences(rule.getId());
        assertThat(genResult.createdCount()).isEqualTo(0);
        assertThat(genResult.existingCount()).isEqualTo(sessionCountAfterConfirm);
    }

    @Test
    @DisplayName("Regression: confirmSeriesChange with unchanged SCHEDULED retained occurrences creates 0 reschedule events and allows missing impact reader")
    void confirmSeriesChange_unchangedScheduledRetainedOccurrences_zeroImpactAndAllowedWithoutImpactReader() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 5);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        VaccinationSession s1 = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId()).get(0);
        s1.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(s1);

        // Service with null impact reader
        SessionSeriesChangeService noReaderService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, null, clock);

        // Propose adding Friday Oct 9 with same time (09:00 - 12:00)
        Set<DayOfWeek> newWeekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        LocalDate newEndDate = LocalDate.of(2026, 10, 9);

        SeriesChangePreview preview = noReaderService.previewSeriesChange(
                rule.getId(), newWeekdays, startDate, newEndDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE);

        assertThat(preview.totalAffectedAppointmentsCount()).isEqualTo(0);
        assertThat(preview.totalAffectedWaitlistCount()).isEqualTo(0);

        SeriesChangeResult result = noReaderService.confirmSeriesChange(
                rule.getId(), newWeekdays, startDate, newEndDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE,
                preview.previewToken(), "Adding Friday without modifying Monday", "admin-1");

        assertThat(result.rescheduledCount()).isEqualTo(0);
        assertThat(result.newCount()).isEqualTo(1);
        assertThat(result.eventsCreatedCount()).isEqualTo(0);

        // Verify s1 is untouched SCHEDULED and zero events exist
        VaccinationSession reloadedS1 = sessionRepository.findById(s1.getId()).orElseThrow();
        assertThat(reloadedS1.getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.SCHEDULED);
        assertThat(sessionChangeEventRepository.findBySessionIdIn(List.of(s1.getId()))).isEmpty();
    }

    @Test
    @DisplayName("Regression: confirmSeriesChange rejects backward expansion introducing past occurrence before mutating rule")
    void confirmSeriesChange_backwardSeriesExpansionPastOccurrence_rejectedBeforeMutation() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 12);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, centerA, EnumSet.of(DayOfWeek.MONDAY),
                startDate, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE));

        generationService.generateOccurrences(rule.getId());

        // Attempt confirm introducing Sep 21 (past date before FIXED_NOW)
        LocalDate pastStart = LocalDate.of(2026, 9, 21);
        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY), pastStart, endDate, LocalTime.of(9, 0), LocalTime.of(12, 0), TIMEZONE);

        assertThatThrownBy(() -> seriesChangeService.confirmSeriesChange(
                rule.getId(), proposedDef, "any-token", "Attempting past expansion", "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot introduce past occurrence");

        // Rule must remain untouched in DB
        SessionRecurrenceRule untouchedRule = ruleRepository.findById(rule.getId()).orElseThrow();
        assertThat(untouchedRule.getStartDate()).isEqualTo(startDate);
    }
}
