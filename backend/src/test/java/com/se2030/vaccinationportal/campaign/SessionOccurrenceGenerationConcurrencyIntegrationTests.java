package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignLifecycleEventRepository;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignLocationTargetRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignQuestionnaireAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.RecurrenceGenerationResult;
import com.se2030.vaccinationportal.campaign.service.SessionOccurrenceGenerationService;
import com.se2030.vaccinationportal.campaign.service.SessionRecurrenceRuleService;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.campaign.service.VaccinationCenterService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency integration tests for SessionOccurrenceGenerationService.
 * Worker threads invoke transactional service methods across separate connections and transactions.
 * Proves pessimistic serialization and duplicate-prevention on concurrent occurrence generation.
 */
@SpringBootTest
@ActiveProfiles("test")
class SessionOccurrenceGenerationConcurrencyIntegrationTests {

    @Autowired private SessionOccurrenceGenerationService generationService;
    @Autowired private SessionRecurrenceRuleService ruleService;
    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCenterService centerService;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private CampaignQuestionnaireAssignmentRepository questionnaireAssignmentRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private CampaignLocationTargetRepository locationTargetRepository;
    @Autowired private CampaignLifecycleEventRepository eventRepository;

    private static final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);
    private static final LocalTime MORNING_START = LocalTime.of(9, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);

    @BeforeEach
    void clearPersistedState() {
        sessionChangeEventRepository.deleteAllInBatch();
        sessionRepository.deleteAllInBatch();
        ruleRepository.deleteAllInBatch();
        closureRepository.deleteAllInBatch();
        operatingHoursRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        questionnaireAssignmentRepository.deleteAllInBatch();
        doseProgrammeRepository.deleteAllInBatch();
        locationTargetRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
        centerRepository.deleteAllInBatch();
        targetGroupRepository.deleteAllInBatch();
        vaccineTypeRepository.deleteAllInBatch();
    }

    @AfterEach
    void clearPersistedStateAfterTest() {
        clearPersistedState();
    }

    @Test
    @Timeout(30)
    void concurrentGenerationOnSameRule_producesExactSingleOccurrenceSetWithoutDuplicates() throws Exception {
        VaccinationCampaign campaign = createCampaign("CONC-GEN-1");
        VaccinationCenter center = createCenter("CONC-GEN-1", CenterStatus.ACTIVE);
        setupPrerequisites(campaign, center);

        // 4 Wednesdays in October 2026 (Oct 7, 14, 21, 28)
        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        // Two worker threads simultaneously invoke generateOccurrences for the same rule
        List<Outcome> outcomes = runConcurrently(
                () -> generationService.generateOccurrences(rule.getId()),
                () -> generationService.generateOccurrences(rule.getId())
        );

        // Both calls should complete successfully (one creates, the other observes existing or creates disjoint)
        assertTrue(outcomes.get(0).success(), "First generation call failed: " + outcomes.get(0).error());
        assertTrue(outcomes.get(1).success(), "Second generation call failed: " + outcomes.get(1).error());

        RecurrenceGenerationResult res1 = (RecurrenceGenerationResult) outcomes.get(0).result();
        RecurrenceGenerationResult res2 = (RecurrenceGenerationResult) outcomes.get(1).result();

        // Total created across both calls must equal 4
        assertEquals(4, res1.createdCount() + res2.createdCount());
        assertEquals(4, sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId()).size());
    }

    @Test
    @Timeout(30)
    void concurrentGenerationAndRuleCancellation_maintainsConsistentState() throws Exception {
        VaccinationCampaign campaign = createCampaign("CONC-GEN-CANCEL");
        VaccinationCenter center = createCenter("CONC-GEN-CANCEL", CenterStatus.ACTIVE);
        setupPrerequisites(campaign, center);

        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        // Thread 1: generates occurrences
        // Thread 2: cancels rule
        List<Outcome> outcomes = runConcurrently(
                () -> generationService.generateOccurrences(rule.getId()),
                () -> ruleService.cancelRule(rule.getId())
        );

        boolean genSuccess = outcomes.get(0).success();
        boolean cancelSuccess = outcomes.get(1).success();

        // Cancellation should always succeed
        assertTrue(cancelSuccess, "Cancellation failed: " + outcomes.get(1).error());

        if (genSuccess) {
            // Generator won rule lock first: occurrences materialized before cancellation
            List<VaccinationSession> sessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
            assertEquals(4, sessions.size());
        } else {
            // Cancellation won first: generation rejected because rule was CANCELLED
            assertTrue(outcomes.get(0).error().getMessage().contains("must be ACTIVE"));
        }
    }

    @Test
    @Timeout(30)
    void concurrentGenerationAndCampaignSuspension_suspensionBlocksGeneration() throws Exception {
        VaccinationCampaign campaign = createCampaignInStatus("CONC-GEN-SUSP", CampaignStatus.PUBLISHED);
        VaccinationCenter center = createCenter("CONC-GEN-SUSP", CenterStatus.ACTIVE);
        setupPrerequisites(campaign, center);

        SessionRecurrenceRule rule = ruleService.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.WEDNESDAY),
                CAMPAIGN_START, CAMPAIGN_END, MORNING_START, MORNING_END, "UTC");

        // Thread 1: generates occurrences
        // Thread 2: suspends campaign
        List<Outcome> outcomes = runConcurrently(
                () -> generationService.generateOccurrences(rule.getId()),
                () -> campaignService.suspendCampaign(campaign.getId(), "Safety hold for investigation", "ACTOR-TEST-001")
        );

        boolean genSuccess = outcomes.get(0).success();
        boolean suspendSuccess = outcomes.get(1).success();

        // Suspension should always succeed
        assertTrue(suspendSuccess, "Suspension failed: " + outcomes.get(1).error());

        if (genSuccess) {
            // Generator won campaign lock first: occurrences materialized before suspension committed
            List<VaccinationSession> sessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
            assertEquals(4, sessions.size());
        } else {
            // Suspension won campaign lock first: generation rejected because campaign was SUSPENDED
            assertTrue(outcomes.get(0).error().getMessage().contains("Cannot generate session occurrences for suspended campaign"));
        }
    }

    private VaccinationCampaign createCampaignInStatus(String suffix, CampaignStatus status) {
        VaccineType vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-" + suffix, "Vaccine " + suffix));
        TargetGroup targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-" + suffix, "Target Group " + suffix));

        return campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-" + suffix, "Campaign " + suffix, "Desc",
                vaccineType, targetGroup,
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"),
                status));
    }

    private VaccinationCampaign createCampaign(String suffix) {
        VaccineType vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-" + suffix, "Vaccine " + suffix));
        TargetGroup targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-" + suffix, "Target Group " + suffix));

        return campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-" + suffix, "Campaign " + suffix, "Desc",
                vaccineType, targetGroup,
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"),
                CampaignStatus.DRAFT));
    }

    private VaccinationCenter createCenter(String suffix, CenterStatus status) {
        return centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-" + suffix, "Center " + suffix, "123 Concurrency Ave", "+94 11 000 1111",
                status, 100, 50, "UTC"));
    }

    private void setupPrerequisites(VaccinationCampaign campaign, VaccinationCenter center) {
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));

        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center, day, LocalTime.of(8, 0), LocalTime.of(17, 0),
                    CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        }
    }

    private List<Outcome> runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<Outcome> firstFuture = executor.submit(contender(first, ready, start));
            Future<Outcome> secondFuture = executor.submit(contender(second, ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS), "Contenders did not become ready in time");
            start.countDown();
            return List.of(firstFuture.get(10, TimeUnit.SECONDS), secondFuture.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate");
        }
    }

    private Callable<Outcome> contender(Callable<?> action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new Outcome(false, null, new TimeoutException("Start barrier timed out"));
            }
            try {
                Object result = action.call();
                return new Outcome(true, result, null);
            } catch (Throwable error) {
                return new Outcome(false, null, error);
            }
        };
    }

    private record Outcome(boolean success, Object result, Throwable error) {
    }
}
