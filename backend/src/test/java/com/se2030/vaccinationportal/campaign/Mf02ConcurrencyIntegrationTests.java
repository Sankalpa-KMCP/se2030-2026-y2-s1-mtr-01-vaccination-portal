package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignQuestionnaireAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CampaignDoseProgrammeService;
import com.se2030.vaccinationportal.campaign.service.CampaignQuestionnaireAssignmentService;
import com.se2030.vaccinationportal.campaign.service.OperatingHoursService;
import com.se2030.vaccinationportal.campaign.service.QuestionnaireVersionPublicationPort;
import com.se2030.vaccinationportal.campaign.service.SessionChangePreview;
import com.se2030.vaccinationportal.campaign.service.SessionOccurrenceGenerationService;
import com.se2030.vaccinationportal.campaign.service.SessionSeriesChangeService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Separate worker threads invoke transactional service proxies, so each contender owns an
 * independent transaction. These H2/MySQL-mode tests prove the parent-lock invariants without
 * claiming production-MySQL equivalence.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(Mf02ConcurrencyIntegrationTests.PublishedQuestionnaireConfig.class)
class Mf02ConcurrencyIntegrationTests {

    private static final LocalDate INITIAL_START = LocalDate.of(2029, 1, 1);
    private static final LocalDate REPLACEMENT_START = LocalDate.of(2030, 1, 1);

    @TestConfiguration
    static class PublishedQuestionnaireConfig {
        @Bean
        @Primary
        QuestionnaireVersionPublicationPort questionnaireVersionPublicationPort() {
            Set<Long> published = Set.of(100L, 101L, 102L);
            return published::contains;
        }
    }

    @Autowired private CampaignDoseProgrammeService doseProgrammeService;
    @Autowired private CampaignQuestionnaireAssignmentService questionnaireAssignmentService;
    @Autowired private OperatingHoursService operatingHoursService;
    @Autowired private VaccinationSessionService sessionService;
    @Autowired private SessionSeriesChangeService seriesChangeService;
    @Autowired private SessionOccurrenceGenerationService generationService;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private CampaignQuestionnaireAssignmentRepository questionnaireAssignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    @BeforeEach
    void clearPersistedState() {
        sessionChangeEventRepository.deleteAllInBatch();
        sessionRepository.deleteAllInBatch();
        ruleRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        operatingHoursRepository.deleteAllInBatch();
        questionnaireAssignmentRepository.deleteAllInBatch();
        doseProgrammeRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
        centerRepository.deleteAllInBatch();
        targetGroupRepository.deleteAllInBatch();
        vaccineTypeRepository.deleteAllInBatch();
    }

    @Test
    @Timeout(30)
    void concurrentDoseProgrammeReplacementsRetainOneUnambiguousHistory() throws Exception {
        VaccinationCampaign campaign = createCampaign("CMP-CONCURRENT-DOSE");
        doseProgrammeService.createInitialProgramme(
                campaign.getId(), DoseCategory.PRIMARY, "1,2", 21, false, 10, INITIAL_START
        );

        List<Outcome> outcomes = runConcurrently(
                () -> doseProgrammeService.createNewVersion(
                        campaign.getId(), DoseCategory.BOOSTER, "3", 90, true, 12, REPLACEMENT_START
                ),
                () -> doseProgrammeService.createNewVersion(
                        campaign.getId(), DoseCategory.BOOSTER, "4", 120, true, 15, REPLACEMENT_START
                )
        );

        assertOneSuccessAndOneDomainRejection(outcomes);
        List<CampaignDoseProgramme> history = doseProgrammeService.getProgrammeHistory(campaign.getId());
        assertEquals(2, history.size());
        assertEquals(REPLACEMENT_START, history.get(1).getActiveTo());
        assertEquals(2, doseProgrammeService.getEffectiveProgramme(campaign.getId(), REPLACEMENT_START)
                .orElseThrow().getVersionNumber());
    }

    @Test
    @Timeout(30)
    void concurrentQuestionnaireReplacementsRetainOneUnambiguousHistory() throws Exception {
        VaccinationCampaign campaign = createCampaign("CMP-CONCURRENT-QUESTIONNAIRE");
        questionnaireAssignmentService.assignQuestionnaireVersion(campaign.getId(), 100L, INITIAL_START);

        List<Outcome> outcomes = runConcurrently(
                () -> questionnaireAssignmentService.assignQuestionnaireVersion(
                        campaign.getId(), 101L, REPLACEMENT_START
                ),
                () -> questionnaireAssignmentService.assignQuestionnaireVersion(
                        campaign.getId(), 102L, REPLACEMENT_START
                )
        );

        assertOneSuccessAndOneDomainRejection(outcomes);
        var history = questionnaireAssignmentService.getAssignmentHistory(campaign.getId());
        assertEquals(2, history.size());
        assertEquals(REPLACEMENT_START, history.get(1).getActiveTo());
        assertTrue(Set.of(101L, 102L).contains(
                questionnaireAssignmentService.getEffectiveAssignmentAt(campaign.getId(), REPLACEMENT_START)
                        .orElseThrow().getQuestionnaireVersionId()
        ));
    }

    @Test
    @Timeout(30)
    void concurrentOperatingHoursCreateAndUpdateCannotCommitAConflict() throws Exception {
        VaccinationCenter center = createCenter("CTR-CONCURRENT-OH");
        OperatingHours existing = operatingHoursService.createOperatingHours(
                center.getId(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), INITIAL_START, null
        );

        List<Outcome> outcomes = runConcurrently(
                () -> operatingHoursService.updateOperatingHours(
                        existing.getId(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(14, 0), INITIAL_START, null
                ),
                () -> operatingHoursService.createOperatingHours(
                        center.getId(), DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(17, 0), INITIAL_START, null
                )
        );

        assertOneSuccessAndOneDomainRejection(outcomes);
        List<OperatingHours> committed = operatingHoursService.getEffectiveOperatingHours(center.getId(), INITIAL_START);
        for (int left = 0; left < committed.size(); left++) {
            for (int right = left + 1; right < committed.size(); right++) {
                assertFalse(timesOverlap(committed.get(left), committed.get(right)));
            }
        }
    }

    @Test
    @Timeout(30)
    void concurrentReschedulesOnSameSessionOnlyOneSucceedsDueToStaleToken() throws Exception {
        VaccinationCampaign campaign = createCampaign("CMP-CONC-SESS-1");
        VaccinationCenter center = createCenter("CTR-CONC-SESS-1");
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, LocalDate.of(2029, 1, 1), LocalDate.of(2031, 1, 1)));
        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    LocalDate.of(2029, 1, 1), null));
        }

        Instant initialStart = Instant.parse("2029-06-15T09:00:00Z");
        Instant initialEnd = Instant.parse("2029-06-15T12:00:00Z");
        Instant initialClose = Instant.parse("2029-06-15T08:00:00Z");
        VaccinationSession session = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, initialStart, initialEnd, initialClose, "UTC"));

        Instant newStartA = Instant.parse("2029-06-15T10:00:00Z");
        Instant newEndA = Instant.parse("2029-06-15T13:00:00Z");
        SessionChangePreview previewA = sessionService.previewReschedule(session.getId(), newStartA, newEndA, null);

        Instant newStartB = Instant.parse("2029-06-15T14:00:00Z");
        Instant newEndB = Instant.parse("2029-06-15T17:00:00Z");
        SessionChangePreview previewB = sessionService.previewReschedule(session.getId(), newStartB, newEndB, null);

        List<Outcome> outcomes = runConcurrently(
                () -> sessionService.confirmReschedule(
                        session.getId(), newStartA, newEndA, null, previewA.previewToken(), "Reason A", "ACTOR-A"),
                () -> sessionService.confirmReschedule(
                        session.getId(), newStartB, newEndB, null, previewB.previewToken(), "Reason B", "ACTOR-B")
        );

        // Exactly one must succeed, and the loser must fail with IllegalStateException (stale token)
        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(outcome -> !outcome.success()).findFirst().orElseThrow();
        assertInstanceOf(IllegalStateException.class, rejected.error(), outcomes.toString());

        // Verify exactly 1 SessionChangeEvent created
        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(1, events.size());
    }

    @Test
    @Timeout(30)
    void concurrentOppositeCenterTransfersExecuteWithoutDeadlock() throws Exception {
        VaccinationCampaign campaign = createCampaign("CMP-CONC-SESS-2");
        VaccinationCenter center1 = createCenter("CTR-CONC-T1");
        VaccinationCenter center2 = createCenter("CTR-CONC-T2");

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center1, LocalDate.of(2029, 1, 1), LocalDate.of(2031, 1, 1)));
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center2, LocalDate.of(2029, 1, 1), LocalDate.of(2031, 1, 1)));

        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center1, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    LocalDate.of(2029, 1, 1), null));
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center2, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    LocalDate.of(2029, 1, 1), null));
        }

        Instant start1 = Instant.parse("2029-06-15T09:00:00Z");
        Instant end1 = Instant.parse("2029-06-15T12:00:00Z");
        VaccinationSession session1 = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center1, start1, end1, start1, "UTC"));

        Instant start2 = Instant.parse("2029-06-15T13:00:00Z");
        Instant end2 = Instant.parse("2029-06-15T16:00:00Z");
        VaccinationSession session2 = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center2, start2, end2, start2, "UTC"));

        // Session 1 transfers from center1 to center2
        SessionChangePreview preview1 = sessionService.previewCenterTransfer(session1.getId(), center2.getId(), start1, end1, null);
        // Session 2 transfers from center2 to center1
        SessionChangePreview preview2 = sessionService.previewCenterTransfer(session2.getId(), center1.getId(), start2, end2, null);

        // Run both transfers concurrently (center1 -> center2 and center2 -> center1)
        List<Outcome> outcomes = runConcurrently(
                () -> sessionService.confirmCenterTransfer(
                        session1.getId(), center2.getId(), start1, end1, null, preview1.previewToken(), "Transfer 1->2", "ACTOR-1"),
                () -> sessionService.confirmCenterTransfer(
                        session2.getId(), center1.getId(), start2, end2, null, preview2.previewToken(), "Transfer 2->1", "ACTOR-2")
        );

        // Both must succeed without deadlock because locks are acquired deterministically in min(centerId), max(centerId) order
        assertEquals(2, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        assertTrue(outcomes.get(0).success());
        assertTrue(outcomes.get(1).success());
    }

    @Test
    @Timeout(15)
    void concurrentSeriesConfirmationAndGeneratorDoNotDeadlock() throws Exception {
        VaccinationCampaign campaign = createCampaign("CONC-SERIES-GEN");
        VaccinationCenter center = createCenter("LOC-SERIES-GEN");
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, LocalDate.of(2029, 1, 1), LocalDate.of(2031, 1, 1)));

        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    LocalDate.of(2029, 1, 1), null));
        }

        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, center, EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "UTC"));

        generationService.generateOccurrences(rule.getId());

        com.se2030.vaccinationportal.campaign.service.SeriesChangePreview preview =
                seriesChangeService.previewSeriesChange(
                        rule.getId(), EnumSet.of(DayOfWeek.MONDAY),
                        LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                        LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC");

        // Run series confirmation and occurrence generator concurrently on the same rule
        List<Outcome> outcomes = runConcurrently(
                () -> seriesChangeService.confirmSeriesChange(
                        rule.getId(), EnumSet.of(DayOfWeek.MONDAY),
                        LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                        LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC",
                        preview.previewToken(), "Concurrent series shift", "ACTOR-1"),
                () -> generationService.generateOccurrences(rule.getId())
        );

        // Both complete without deadlock
        assertEquals(2, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
    }

    @Test
    @Timeout(15)
    void concurrentSeriesConfirmationsOnSameRuleSerializeCleanly() throws Exception {
        VaccinationCampaign campaign = createCampaign("CONC-SERIES-SER");
        VaccinationCenter center = createCenter("LOC-SERIES-SER");
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, LocalDate.of(2029, 1, 1), LocalDate.of(2031, 1, 1)));

        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    LocalDate.of(2029, 1, 1), null));
        }

        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, center, EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "UTC"));

        generationService.generateOccurrences(rule.getId());

        com.se2030.vaccinationportal.campaign.service.SeriesChangePreview preview =
                seriesChangeService.previewSeriesChange(
                        rule.getId(), EnumSet.of(DayOfWeek.MONDAY),
                        LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                        LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC");

        // Run two series confirmations with the same preview token concurrently
        List<Outcome> outcomes = runConcurrently(
                () -> seriesChangeService.confirmSeriesChange(
                        rule.getId(), EnumSet.of(DayOfWeek.MONDAY),
                        LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                        LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC",
                        preview.previewToken(), "First confirm", "ACTOR-1"),
                () -> seriesChangeService.confirmSeriesChange(
                        rule.getId(), EnumSet.of(DayOfWeek.MONDAY),
                        LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                        LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC",
                        preview.previewToken(), "Second confirm", "ACTOR-2")
        );

        // One must succeed, and the second must be rejected due to stale token (state mutated by the first)
        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(o -> !o.success()).findFirst().orElseThrow();
        assertInstanceOf(IllegalStateException.class, rejected.error(), outcomes.toString());
    }

    @Test
    @Timeout(15)
    void concurrentSeriesCancellationsOnSameRuleSerializeCleanly() throws Exception {
        VaccinationCampaign campaign = createCampaign("CONC-SERIES-CANC");
        VaccinationCenter center = createCenter("LOC-SERIES-CANC");
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, LocalDate.of(2029, 1, 1), LocalDate.of(2031, 1, 1)));

        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    LocalDate.of(2029, 1, 1), null));
        }

        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, center, EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "UTC"));

        generationService.generateOccurrences(rule.getId());

        com.se2030.vaccinationportal.campaign.service.SeriesChangePreview preview =
                seriesChangeService.previewSeriesCancellation(rule.getId());

        // Run two series cancellations with the same preview token concurrently
        List<Outcome> outcomes = runConcurrently(
                () -> seriesChangeService.confirmSeriesCancellation(
                        rule.getId(), preview.previewToken(), "First cancel", "ACTOR-1"),
                () -> seriesChangeService.confirmSeriesCancellation(
                        rule.getId(), preview.previewToken(), "Second cancel", "ACTOR-2")
        );

        // Exactly one must succeed, the other rejected (rule not active or stale preview)
        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(o -> !o.success()).findFirst().orElseThrow();
        assertTrue(
                rejected.error() instanceof IllegalArgumentException || rejected.error() instanceof IllegalStateException,
                "Expected IllegalArgumentException or IllegalStateException but got: " + rejected.error()
        );
    }

    @Test
    @Timeout(15)
    void concurrentSeriesCancellationAndSeriesChangeSerializeCleanly() throws Exception {
        VaccinationCampaign campaign = createCampaign("CONC-SERIES-CC");
        VaccinationCenter center = createCenter("LOC-SERIES-CC");
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, LocalDate.of(2029, 1, 1), LocalDate.of(2031, 1, 1)));

        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    LocalDate.of(2029, 1, 1), null));
        }

        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, center, EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "UTC"));

        generationService.generateOccurrences(rule.getId());

        com.se2030.vaccinationportal.campaign.service.SeriesChangePreview cancelPreview =
                seriesChangeService.previewSeriesCancellation(rule.getId());

        com.se2030.vaccinationportal.campaign.service.SeriesChangePreview changePreview =
                seriesChangeService.previewSeriesChange(
                        rule.getId(), EnumSet.of(DayOfWeek.MONDAY),
                        LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                        LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC");

        // Run cancel and schedule change concurrently
        List<Outcome> outcomes = runConcurrently(
                () -> seriesChangeService.confirmSeriesCancellation(
                        rule.getId(), cancelPreview.previewToken(), "Concurrent cancel", "ACTOR-1"),
                () -> seriesChangeService.confirmSeriesChange(
                        rule.getId(), EnumSet.of(DayOfWeek.MONDAY),
                        LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                        LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC",
                        changePreview.previewToken(), "Concurrent shift", "ACTOR-2")
        );

        // Exactly one must succeed, the other rejected cleanly
        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(o -> !o.success()).findFirst().orElseThrow();
        assertTrue(
                rejected.error() instanceof IllegalArgumentException || rejected.error() instanceof IllegalStateException,
                "Expected IllegalArgumentException or IllegalStateException but got: " + rejected.error()
        );
    }

    private VaccinationCampaign createCampaign(String code) {
        VaccineType vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-" + code, "Concurrency vaccine")
        );
        TargetGroup targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-" + code, "Concurrency target")
        );
        return campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                code,
                "Concurrency campaign",
                null,
                vaccineType,
                targetGroup,
                LocalDate.of(2029, 1, 1),
                LocalDate.of(2031, 1, 1),
                Instant.parse("2028-12-01T00:00:00Z"),
                Instant.parse("2030-12-31T23:59:59Z"),
                CampaignStatus.DRAFT
        ));
    }

    private VaccinationCenter createCenter(String code) {
        return centerRepository.saveAndFlush(new VaccinationCenter(
                code, "Concurrency center", "1 Test Road", "+94 11 000 0000",
                CenterStatus.ACTIVE, 100, 50, "UTC"
        ));
    }

    private List<Outcome> runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome> firstFuture = executor.submit(contender(first, ready, start));
            Future<Outcome> secondFuture = executor.submit(contender(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Contenders did not become ready in time");
            start.countDown();
            return List.of(getBounded(firstFuture), getBounded(secondFuture));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate");
        }
    }

    private Callable<Outcome> contender(Callable<?> action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new Outcome(false, new TimeoutException("Start barrier timed out"));
            }
            try {
                action.call();
                return new Outcome(true, null);
            } catch (Throwable error) {
                return new Outcome(false, error);
            }
        };
    }

    private Outcome getBounded(Future<Outcome> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

    private void assertOneSuccessAndOneDomainRejection(List<Outcome> outcomes) {
        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(outcome -> !outcome.success()).findFirst().orElseThrow();
        assertInstanceOf(IllegalArgumentException.class, rejected.error(), outcomes.toString());
    }

    private boolean timesOverlap(OperatingHours first, OperatingHours second) {
        return first.getOpening().isBefore(second.getClosing())
                && second.getOpening().isBefore(first.getClosing());
    }

    private record Outcome(boolean success, Throwable error) {
    }
}
