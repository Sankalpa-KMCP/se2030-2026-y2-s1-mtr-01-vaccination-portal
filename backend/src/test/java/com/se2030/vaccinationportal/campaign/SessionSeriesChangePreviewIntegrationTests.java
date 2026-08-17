package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SeriesOccurrenceCategory;
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
@Import(SessionSeriesChangePreviewIntegrationTests.TestConfig.class)
class SessionSeriesChangePreviewIntegrationTests {

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
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);
    private static final Instant CAMPAIGN_REG_START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant CAMPAIGN_REG_END = Instant.parse("2026-10-30T00:00:00Z");

    @BeforeEach
    void setUp() {
        VaccineType vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-SERIES-TEST", "Series Test Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-SERIES-TEST", "Series Test Target Group"));

        campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-SERIES-TEST", "Series Campaign", "Testing series change workflows",
                vaccineType, targetGroup,
                CAMPAIGN_START, CAMPAIGN_END,
                CAMPAIGN_REG_START, CAMPAIGN_REG_END,
                CampaignStatus.DRAFT));

        centerA = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-SERIES-A", "Center Alpha", "100 Alpha Street", "+94 11 111 1111",
                CenterStatus.ACTIVE, 200, 100, "UTC"));

        centerB = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-SERIES-B", "Center Beta", "200 Beta Street", "+94 11 222 2222",
                CenterStatus.ACTIVE, 300, 150, "UTC"));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, centerA, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, centerB, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    centerA, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    CAMPAIGN_START.minusMonths(1), null));
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    centerB, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    CAMPAIGN_START.minusMonths(1), null));
        }
    }

    private SessionRecurrenceRule createActiveRule(
            Set<DayOfWeek> weekdays,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        SessionRecurrenceRule rule = new SessionRecurrenceRule(
                campaign, centerA, weekdays, startDate, endDate, startTime, endTime, centerA.getTimezone());
        return ruleRepository.saveAndFlush(rule);
    }

    @Test
    @DisplayName("Unchanged rule preview classifies all future sessions as RETAINED with zero impact on DRAFT")
    void testUnchangedRulePreview() {
        // Mon, Wed, Fri for Oct 5 (Mon), Oct 7 (Wed), Oct 9 (Fri)
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 9);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);
        RecurrenceGenerationResult genResult = generationService.generateOccurrences(rule.getId());
        assertThat(genResult.createdCount()).isEqualTo(3);

        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), weekdays, start, end, startTime, endTime, centerA.getTimezone());

        assertThat(preview.ruleId()).isEqualTo(rule.getId());
        assertThat(preview.totalAffectedAppointmentsCount()).isEqualTo(0);
        assertThat(preview.totalAffectedWaitlistCount()).isEqualTo(0);
        assertThat(preview.previewToken()).isNotBlank();
        assertThat(preview.occurrenceEntries()).hasSize(3);

        for (SeriesOccurrencePreviewItem item : preview.occurrenceEntries()) {
            assertThat(item.category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
            assertThat(item.sessionId()).isNotNull();
            assertThat(item.currentSnapshot()).isNotNull();
            assertThat(item.proposedSnapshot()).isNotNull();
            assertThat(item.affectedAppointmentCount()).isEqualTo(0);
            assertThat(item.affectedWaitlistCount()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("Weekday add classifies newly introduced date as NEW in DRAFT with bookingCloseAt = startAt")
    void testWeekdayAddPreview() {
        // Initially Mon, Wed: Oct 5 (Mon), Oct 7 (Wed)
        Set<DayOfWeek> originalWeekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 7);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(originalWeekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId());

        // Proposed: Mon, Tue, Wed (adds Tuesday Oct 6)
        Set<DayOfWeek> newWeekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY);
        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), newWeekdays, start, end, startTime, endTime, centerA.getTimezone());

        assertThat(preview.occurrenceEntries()).hasSize(3);

        Map<LocalDate, SeriesOccurrencePreviewItem> map = toDateMap(preview.occurrenceEntries());
        assertThat(map.get(LocalDate.of(2026, 10, 5)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
        assertThat(map.get(LocalDate.of(2026, 10, 7)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);

        SeriesOccurrencePreviewItem newItem = map.get(LocalDate.of(2026, 10, 6));
        assertThat(newItem.category()).isEqualTo(SeriesOccurrenceCategory.NEW);
        assertThat(newItem.sessionId()).isNull();
        assertThat(newItem.currentSnapshot()).isNull();
        assertThat(newItem.proposedSnapshot()).isNotNull();
        assertThat(newItem.proposedSnapshot().getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.DRAFT);
        assertThat(newItem.proposedSnapshot().getBookingCloseAt()).isEqualTo(newItem.proposedSnapshot().getStartAt());
    }

    @Test
    @DisplayName("Weekday remove classifies omitted date as REMOVED with CANCELLED proposed snapshot")
    void testWeekdayRemovePreview() {
        // Initially Mon, Wed, Fri: Oct 5, Oct 7, Oct 9
        Set<DayOfWeek> originalWeekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 9);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(originalWeekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId());

        // Proposed: Mon, Fri only (removes Wednesday Oct 7)
        Set<DayOfWeek> newWeekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), newWeekdays, start, end, startTime, endTime, centerA.getTimezone());

        assertThat(preview.occurrenceEntries()).hasSize(3);
        Map<LocalDate, SeriesOccurrencePreviewItem> map = toDateMap(preview.occurrenceEntries());

        assertThat(map.get(LocalDate.of(2026, 10, 5)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
        assertThat(map.get(LocalDate.of(2026, 10, 9)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);

        SeriesOccurrencePreviewItem removedItem = map.get(LocalDate.of(2026, 10, 7));
        assertThat(removedItem.category()).isEqualTo(SeriesOccurrenceCategory.REMOVED);
        assertThat(removedItem.sessionId()).isNotNull();
        assertThat(removedItem.proposedSnapshot().getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.CANCELLED);
    }

    @Test
    @DisplayName("Date range extend classifies extra future dates as NEW")
    void testDateRangeExtendPreview() {
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 12); // Oct 5, Oct 12
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId());

        // Extend to Oct 19
        LocalDate newEnd = LocalDate.of(2026, 10, 19);
        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), weekdays, start, newEnd, startTime, endTime, centerA.getTimezone());

        assertThat(preview.occurrenceEntries()).hasSize(3);
        Map<LocalDate, SeriesOccurrencePreviewItem> map = toDateMap(preview.occurrenceEntries());

        assertThat(map.get(LocalDate.of(2026, 10, 5)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
        assertThat(map.get(LocalDate.of(2026, 10, 12)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
        assertThat(map.get(LocalDate.of(2026, 10, 19)).category()).isEqualTo(SeriesOccurrenceCategory.NEW);
    }

    @Test
    @DisplayName("Date range shorten classifies dates beyond new end date as REMOVED")
    void testDateRangeShortenPreview() {
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 19); // Oct 5, Oct 12, Oct 19
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId());

        // Shorten to Oct 12
        LocalDate newEnd = LocalDate.of(2026, 10, 12);
        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), weekdays, start, newEnd, startTime, endTime, centerA.getTimezone());

        assertThat(preview.occurrenceEntries()).hasSize(3);
        Map<LocalDate, SeriesOccurrencePreviewItem> map = toDateMap(preview.occurrenceEntries());

        assertThat(map.get(LocalDate.of(2026, 10, 5)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
        assertThat(map.get(LocalDate.of(2026, 10, 12)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
        assertThat(map.get(LocalDate.of(2026, 10, 19)).category()).isEqualTo(SeriesOccurrenceCategory.REMOVED);
    }

    @Test
    @DisplayName("Time change updates startAt, endAt, and sets bookingCloseAt = proposedStartAt for RETAINED sessions")
    void testTimeChangePreview() {
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 5);
        LocalTime originalStart = LocalTime.of(9, 0);
        LocalTime originalEnd = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, originalStart, originalEnd);
        generationService.generateOccurrences(rule.getId());

        // Shift time to 13:00 - 16:00
        LocalTime newStart = LocalTime.of(13, 0);
        LocalTime newEnd = LocalTime.of(16, 0);
        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), weekdays, start, end, newStart, newEnd, centerA.getTimezone());

        assertThat(preview.occurrenceEntries()).hasSize(1);
        SeriesOccurrencePreviewItem item = preview.occurrenceEntries().get(0);
        assertThat(item.category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);

        Instant expectedStart = Instant.parse("2026-10-05T13:00:00Z");
        Instant expectedEnd = Instant.parse("2026-10-05T16:00:00Z");

        assertThat(item.proposedSnapshot().getStartAt()).isEqualTo(expectedStart);
        assertThat(item.proposedSnapshot().getEndAt()).isEqualTo(expectedEnd);
        assertThat(item.proposedSnapshot().getBookingCloseAt()).isEqualTo(expectedStart);
    }

    @Test
    @DisplayName("Timezone mismatch against hosting center is rejected with IllegalArgumentException")
    void testTimezoneMismatchRejection() {
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 5);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);

        assertThatThrownBy(() -> seriesChangeService.previewSeriesChange(
                rule.getId(), weekdays, start, end, startTime, endTime, "Asia/Tokyo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match center scheduling timezone");
    }

    @Test
    @DisplayName("SCHEDULED sessions query real SessionImpactReader and aggregate total counts")
    void testScheduledSessionImpactAggregation() {
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 7);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId());

        List<VaccinationSession> sessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        VaccinationSession s1 = sessions.get(0); // Oct 5
        VaccinationSession s2 = sessions.get(1); // Oct 7

        s1.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        s2.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAllAndFlush(List.of(s1, s2));

        // Create mock impact reader with custom counts
        SessionImpactReader mockReader = sessionId -> {
            if (sessionId.equals(s1.getId())) {
                return new SessionImpactSummary(6, 2);
            }
            if (sessionId.equals(s2.getId())) {
                return new SessionImpactSummary(4, 1);
            }
            return new SessionImpactSummary(0, 0);
        };

        SessionSeriesChangeService customService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, mockReader, clock);

        // Preview removing Wednesday (Oct 7) and keeping Monday (Oct 5) with time change
        Set<DayOfWeek> newWeekdays = EnumSet.of(DayOfWeek.MONDAY);
        LocalTime newStart = LocalTime.of(10, 0);
        LocalTime newEnd = LocalTime.of(13, 0);

        SeriesChangePreview preview = customService.previewSeriesChange(
                rule.getId(), newWeekdays, start, end, newStart, newEnd, centerA.getTimezone());

        // Total impact = s1 (6 appts, 2 wait) + s2 (4 appts, 1 wait) = 10 appts, 3 wait
        assertThat(preview.totalAffectedAppointmentsCount()).isEqualTo(10);
        assertThat(preview.totalAffectedWaitlistCount()).isEqualTo(3);

        Map<LocalDate, SeriesOccurrencePreviewItem> map = toDateMap(preview.occurrenceEntries());
        assertThat(map.get(LocalDate.of(2026, 10, 5)).affectedAppointmentCount()).isEqualTo(6);
        assertThat(map.get(LocalDate.of(2026, 10, 5)).affectedWaitlistCount()).isEqualTo(2);
        assertThat(map.get(LocalDate.of(2026, 10, 7)).affectedAppointmentCount()).isEqualTo(4);
        assertThat(map.get(LocalDate.of(2026, 10, 7)).affectedWaitlistCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("SCHEDULED session preview throws IllegalStateException if SessionImpactReader is unavailable")
    void testMissingSessionImpactReaderRejection() {
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 5);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId());

        VaccinationSession s1 = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId()).get(0);
        s1.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(s1);

        // Service without impact reader
        SessionSeriesChangeService noReaderService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, null, clock);

        assertThatThrownBy(() -> noReaderService.previewSeriesChange(
                rule.getId(), weekdays, start, end, LocalTime.of(10, 0), LocalTime.of(13, 0), centerA.getTimezone()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Session impact reader is not available to assess SCHEDULED session changes");
    }

    @Test
    @DisplayName("Individually modified occurrences are classified as PRESERVED_EXCEPTION and do not contribute to impact totals")
    void testPreservedIndividualExceptions() {
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 9);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId());

        List<VaccinationSession> sessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        VaccinationSession s1 = sessions.get(0); // Oct 5
        VaccinationSession s2 = sessions.get(1); // Oct 7
        VaccinationSession s3 = sessions.get(2); // Oct 9

        // 1. Individually reschedule s1
        SessionChangePreview p1 = sessionService.previewReschedule(
                s1.getId(), Instant.parse("2026-10-05T14:00:00Z"), Instant.parse("2026-10-05T17:00:00Z"), null);
        sessionService.confirmReschedule(
                s1.getId(), Instant.parse("2026-10-05T14:00:00Z"), Instant.parse("2026-10-05T17:00:00Z"), null,
                p1.previewToken(), "Individual shift", "ACTOR-1");

        // 2. Individually transfer s2 to centerB
        SessionChangePreview p2 = sessionService.previewCenterTransfer(
                s2.getId(), centerB.getId(), Instant.parse("2026-10-07T09:00:00Z"), Instant.parse("2026-10-07T12:00:00Z"), null);
        sessionService.confirmCenterTransfer(
                s2.getId(), centerB.getId(), Instant.parse("2026-10-07T09:00:00Z"), Instant.parse("2026-10-07T12:00:00Z"), null,
                p2.previewToken(), "Individual transfer", "ACTOR-2");

        // 3. Individually cancel s3
        SessionChangePreview p3 = sessionService.previewCancellation(s3.getId());
        sessionService.confirmCancellation(s3.getId(), p3.previewToken(), "Individual cancel", "ACTOR-3");

        // Now preview a series change: change times to 10:00 - 13:00
        SeriesChangePreview seriesPreview = seriesChangeService.previewSeriesChange(
                rule.getId(), weekdays, start, end, LocalTime.of(10, 0), LocalTime.of(13, 0), centerA.getTimezone());

        assertThat(seriesPreview.occurrenceEntries()).hasSize(3);
        Map<LocalDate, SeriesOccurrencePreviewItem> map = toDateMap(seriesPreview.occurrenceEntries());

        // All 3 sessions have individual SessionChangeEvent records and must be PRESERVED_EXCEPTION
        SeriesOccurrencePreviewItem item1 = map.get(LocalDate.of(2026, 10, 5));
        assertThat(item1.category()).isEqualTo(SeriesOccurrenceCategory.PRESERVED_EXCEPTION);
        assertThat(item1.currentSnapshot().getStartAt()).isEqualTo(Instant.parse("2026-10-05T14:00:00Z"));
        assertThat(item1.proposedSnapshot().getStartAt()).isEqualTo(Instant.parse("2026-10-05T14:00:00Z")); // untouched

        SeriesOccurrencePreviewItem item2 = map.get(LocalDate.of(2026, 10, 7));
        assertThat(item2.category()).isEqualTo(SeriesOccurrenceCategory.PRESERVED_EXCEPTION);
        assertThat(item2.currentSnapshot().getCenterId()).isEqualTo(centerB.getId());
        assertThat(item2.proposedSnapshot().getCenterId()).isEqualTo(centerB.getId()); // untouched

        SeriesOccurrencePreviewItem item3 = map.get(LocalDate.of(2026, 10, 9));
        assertThat(item3.category()).isEqualTo(SeriesOccurrenceCategory.PRESERVED_EXCEPTION);
        assertThat(item3.currentSnapshot().getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.CANCELLED);
        assertThat(item3.proposedSnapshot().getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.CANCELLED); // untouched

        // Series impact totals must be zero because exceptions are not modified by series change
        assertThat(seriesPreview.totalAffectedAppointmentsCount()).isEqualTo(0);
        assertThat(seriesPreview.totalAffectedWaitlistCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Past elapsed sessions and IN_PROGRESS/COMPLETED sessions are classified as INELIGIBLE")
    void testIneligibleSessionsClassification() {
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 9);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId());

        List<VaccinationSession> sessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        VaccinationSession s1 = sessions.get(0); // Oct 5
        VaccinationSession s2 = sessions.get(1); // Oct 7
        VaccinationSession s3 = sessions.get(2); // Oct 9

        // Set s1 to IN_PROGRESS
        s1.schedule();
        s1.start();
        // Set s2 to COMPLETED
        s2.schedule();
        s2.start();
        s2.complete();
        sessionRepository.saveAllAndFlush(List.of(s1, s2));

        // Use clock set to Oct 8 (making s1 and s2 past, s3 future)
        Clock oct8Clock = Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC);
        SessionSeriesChangeService timeService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, null, oct8Clock);

        SeriesChangePreview preview = timeService.previewSeriesChange(
                rule.getId(), weekdays, start, end, LocalTime.of(10, 0), LocalTime.of(13, 0), centerA.getTimezone());

        Map<LocalDate, SeriesOccurrencePreviewItem> map = toDateMap(preview.occurrenceEntries());

        assertThat(map.get(LocalDate.of(2026, 10, 5)).category()).isEqualTo(SeriesOccurrenceCategory.INELIGIBLE);
        assertThat(map.get(LocalDate.of(2026, 10, 7)).category()).isEqualTo(SeriesOccurrenceCategory.INELIGIBLE);
        assertThat(map.get(LocalDate.of(2026, 10, 9)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
    }

    @Test
    @DisplayName("Validation failure in proposed candidate schedule throws exception during preview")
    void testValidatorFailureInPreview() {
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 5);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);

        // 1. Proposed time outside operating hours (19:00 - 22:00 when hours are 08:00 - 18:00)
        assertThatThrownBy(() -> seriesChangeService.previewSeriesChange(
                rule.getId(), weekdays, start, end, LocalTime.of(19, 0), LocalTime.of(22, 0), centerA.getTimezone()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operating hours");



        // 2. Proposed date during center closure
        closureRepository.saveAndFlush(new CenterClosure(
                centerA,
                Instant.parse("2026-10-05T00:00:00Z"),
                Instant.parse("2026-10-05T23:59:59Z"),
                "Emergency maintenance"));

        assertThatThrownBy(() -> seriesChangeService.previewSeriesChange(
                rule.getId(), weekdays, start, end, startTime, endTime, centerA.getTimezone()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("closure");
    }

    @Test
    @DisplayName("Regression: Series extension/add-weekday with unchanged SCHEDULED retained occurrences contributes ZERO impact")
    void testSeriesExtensionWithUnchangedScheduledRetainedOccurrencesHasZeroImpact() {
        Set<DayOfWeek> originalWeekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 7);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(originalWeekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId());

        List<VaccinationSession> sessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        VaccinationSession s1 = sessions.get(0); // Oct 5
        VaccinationSession s2 = sessions.get(1); // Oct 7

        s1.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        s2.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAllAndFlush(List.of(s1, s2));

        // Mock impact reader configured to return non-zero if called
        SessionImpactReader mockReader = sessionId -> new SessionImpactSummary(10, 5);

        SessionSeriesChangeService customService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, mockReader, clock);

        // Propose adding Friday Oct 9 with exact same time (09:00 - 12:00)
        Set<DayOfWeek> newWeekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        LocalDate newEnd = LocalDate.of(2026, 10, 9);

        SeriesChangePreview preview = customService.previewSeriesChange(
                rule.getId(), newWeekdays, start, newEnd, startTime, endTime, centerA.getTimezone());

        // Retained occurrences must not count impact
        assertThat(preview.totalAffectedAppointmentsCount()).isEqualTo(0);
        assertThat(preview.totalAffectedWaitlistCount()).isEqualTo(0);

        Map<LocalDate, SeriesOccurrencePreviewItem> map = toDateMap(preview.occurrenceEntries());
        assertThat(map.get(LocalDate.of(2026, 10, 5)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
        assertThat(map.get(LocalDate.of(2026, 10, 5)).affectedAppointmentCount()).isEqualTo(0);
        assertThat(map.get(LocalDate.of(2026, 10, 5)).affectedWaitlistCount()).isEqualTo(0);

        assertThat(map.get(LocalDate.of(2026, 10, 7)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
        assertThat(map.get(LocalDate.of(2026, 10, 7)).affectedAppointmentCount()).isEqualTo(0);
        assertThat(map.get(LocalDate.of(2026, 10, 7)).affectedWaitlistCount()).isEqualTo(0);

        assertThat(map.get(LocalDate.of(2026, 10, 9)).category()).isEqualTo(SeriesOccurrenceCategory.NEW);
        assertThat(map.get(LocalDate.of(2026, 10, 9)).affectedAppointmentCount()).isEqualTo(0);
        assertThat(map.get(LocalDate.of(2026, 10, 9)).affectedWaitlistCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Regression: Missing SessionImpactReader does NOT block change when scheduled retained sessions are unchanged")
    void testMissingImpactReaderAllowedWhenScheduledRetainedOccurrencesUnchanged() {
        Set<DayOfWeek> originalWeekdays = EnumSet.of(DayOfWeek.MONDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 5);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(originalWeekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId());

        VaccinationSession s1 = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId()).get(0);
        s1.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(s1);

        // Service with null impact reader
        SessionSeriesChangeService noReaderService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, null, clock);

        // Extend date range to Oct 12 with same time (retained session Oct 5 is unchanged)
        LocalDate newEnd = LocalDate.of(2026, 10, 12);
        SeriesChangePreview preview = noReaderService.previewSeriesChange(
                rule.getId(), originalWeekdays, start, newEnd, startTime, endTime, centerA.getTimezone());

        assertThat(preview.totalAffectedAppointmentsCount()).isEqualTo(0);
        assertThat(preview.totalAffectedWaitlistCount()).isEqualTo(0);
        assertThat(preview.occurrenceEntries()).hasSize(2);
    }

    @Test
    @DisplayName("Regression: Backward series expansion introducing missing past occurrence is rejected")
    void testBackwardSeriesExpansionIntroducingPastOccurrenceIsRejected() {
        // FIXED_NOW is 2026-10-01T00:00:00Z
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 12);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId());

        // Propose expanding start date backward to Sep 21 (before FIXED_NOW, no existing session)
        LocalDate pastStart = LocalDate.of(2026, 9, 21);
        assertThatThrownBy(() -> seriesChangeService.previewSeriesChange(
                rule.getId(), weekdays, pastStart, end, startTime, endTime, centerA.getTimezone()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot introduce past occurrence");
    }

    @Test
    @DisplayName("Regression: Missing occurrence on candidate date earlier today is rejected")
    void testElapsedOccurrenceEarlierTodayIsRejected() {
        // Clock fixed at 2026-10-05T11:00:00Z
        Clock clockAt11Am = Clock.fixed(Instant.parse("2026-10-05T11:00:00Z"), ZoneOffset.UTC);

        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.WEDNESDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 7);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId()); // generates Wed Oct 7 only

        SessionSeriesChangeService customService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, null, clockAt11Am);

        // Propose adding Monday (Oct 5) with time 09:00 - 12:00 (startAt 09:00 <= now 11:00)
        Set<DayOfWeek> newWeekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        assertThatThrownBy(() -> customService.previewSeriesChange(
                rule.getId(), newWeekdays, start, end, startTime, endTime, centerA.getTimezone()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot introduce past occurrence");
    }

    @Test
    @DisplayName("Regression: Missing occurrence on candidate date later today is allowed")
    void testFutureOccurrenceLaterTodayIsAllowed() {
        // Clock fixed at 2026-10-05T11:00:00Z
        Clock clockAt11Am = Clock.fixed(Instant.parse("2026-10-05T11:00:00Z"), ZoneOffset.UTC);

        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.WEDNESDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 7);
        LocalTime originalStart = LocalTime.of(9, 0);
        LocalTime originalEnd = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, originalStart, originalEnd);
        generationService.generateOccurrences(rule.getId()); // generates Wed Oct 7 only

        SessionSeriesChangeService customService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, null, clockAt11Am);

        // Propose adding Monday (Oct 5) with future time today 14:00 - 17:00 (startAt 14:00 > now 11:00)
        Set<DayOfWeek> newWeekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        LocalTime newStart = LocalTime.of(14, 0);
        LocalTime newEnd = LocalTime.of(17, 0);

        SeriesChangePreview preview = customService.previewSeriesChange(
                rule.getId(), newWeekdays, start, end, newStart, newEnd, centerA.getTimezone());

        Map<LocalDate, SeriesOccurrencePreviewItem> map = toDateMap(preview.occurrenceEntries());
        assertThat(map.get(LocalDate.of(2026, 10, 5)).category()).isEqualTo(SeriesOccurrenceCategory.NEW);
        assertThat(map.get(LocalDate.of(2026, 10, 7)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
    }

    @Test
    @DisplayName("Regression: Existing historical occurrences are preserved as INELIGIBLE and not rejected")
    void testExistingHistoricalOccurrenceIsPreservedWithoutRejection() {
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 7);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);

        SessionRecurrenceRule rule = createActiveRule(weekdays, start, end, startTime, endTime);
        generationService.generateOccurrences(rule.getId()); // generates Oct 5 and Oct 7

        // Clock fixed at Oct 6 (Oct 5 session is now in the past)
        Clock clockAtOct6 = Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"), ZoneOffset.UTC);
        SessionSeriesChangeService customService = new SessionSeriesChangeService(
                ruleRepository, campaignRepository, centerRepository, sessionRepository,
                sessionChangeEventRepository, validator, null, clockAtOct6);

        // Proposed series change: shift future sessions to 10:00 - 13:00
        SeriesChangePreview preview = customService.previewSeriesChange(
                rule.getId(), weekdays, start, end, LocalTime.of(10, 0), LocalTime.of(13, 0), centerA.getTimezone());

        Map<LocalDate, SeriesOccurrencePreviewItem> map = toDateMap(preview.occurrenceEntries());
        assertThat(map.get(LocalDate.of(2026, 10, 5)).category()).isEqualTo(SeriesOccurrenceCategory.INELIGIBLE);
        assertThat(map.get(LocalDate.of(2026, 10, 7)).category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
    }

    @Test
    @DisplayName("previewSeriesChange with non-existent ruleId throws ResourceNotFoundException")
    void previewSeriesChange_missingRuleId_throwsResourceNotFoundException() {
        Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY);
        assertThatThrownBy(() -> seriesChangeService.previewSeriesChange(
                999999L, weekdays, CAMPAIGN_START, CAMPAIGN_END, LocalTime.of(9, 0), LocalTime.of(12, 0), "UTC"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session recurrence rule not found with ID: 999999");
    }

    private static Map<LocalDate, SeriesOccurrencePreviewItem> toDateMap(List<SeriesOccurrencePreviewItem> items) {
        Map<LocalDate, SeriesOccurrencePreviewItem> map = new HashMap<>();
        for (SeriesOccurrencePreviewItem item : items) {
            map.put(item.originOccurrenceDate(), item);
        }
        return map;
    }
}
