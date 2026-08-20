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
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CenterClosureService;
import com.se2030.vaccinationportal.campaign.service.VaccinationCenterService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency integration tests for VaccinationSession creation.
 * Worker threads invoke the transactional service proxy and therefore own separate transactions.
 * Demonstrates deterministic locking order and authoritative post-lock refresh behavior.
 */
@SpringBootTest
@ActiveProfiles("test")
class VaccinationSessionConcurrencyIntegrationTests {

    @Autowired private VaccinationSessionService sessionService;
    @Autowired private VaccinationCenterService centerService;
    @Autowired private CenterClosureService closureService;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    private static final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);

    @BeforeEach
    void clearPersistedState() {
        sessionRepository.deleteAllInBatch();
        closureRepository.deleteAllInBatch();
        operatingHoursRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
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
    void creationRejectsWhenCenterConcurrentlyDeactivated() throws Exception {
        VaccinationCampaign campaign = createCampaign("CONC-DEACT");
        VaccinationCenter center = createCenter("CONC-DEACT", CenterStatus.ACTIVE);
        setupPrerequisites(campaign, center);

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");

        // Concurrent action 1: deactivates center
        // Concurrent action 2: creates session
        List<Outcome> outcomes = runConcurrently(
                () -> {
                    centerService.deactivateCenter(center.getId());
                    return null;
                },
                () -> sessionService.createOneOffDraftSession(
                        campaign.getId(), center.getId(), startAt, endAt, startAt)
        );

        // One of the outcomes must have completed the deactivation.
        // If session creation ran after deactivation committed, it should be rejected.
        // If session creation ran before deactivation, it succeeds.
        // In all cases, no invalid session can be created after deactivation is committed.
        boolean anyError = outcomes.stream().anyMatch(o -> !o.success());
        if (anyError) {
            Outcome failed = outcomes.stream().filter(o -> !o.success()).findFirst().orElseThrow();
            assertInstanceOf(IllegalArgumentException.class, failed.error());
        }

        // Verify that if center is now INACTIVE, subsequent creation attempt definitely fails
        VaccinationCenter reloaded = centerRepository.findById(center.getId()).orElseThrow();
        if (reloaded.getCenterStatus() == CenterStatus.INACTIVE) {
            try {
                sessionService.createOneOffDraftSession(
                        campaign.getId(), center.getId(), startAt, endAt, startAt);
                org.junit.jupiter.api.Assertions.fail("Expected exception when creating session for INACTIVE center");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("ACTIVE"));
            }
        }
    }

    @Test
    @Timeout(30)
    void concurrentSessionCreationsForSameCenterSucceedWithoutDeadlock() throws Exception {
        VaccinationCampaign campaign = createCampaign("CONC-DEADLOCK");
        VaccinationCenter center = createCenter("CONC-DEADLOCK", CenterStatus.ACTIVE);
        setupPrerequisites(campaign, center);

        Instant start1 = Instant.parse("2026-10-14T09:00:00Z");
        Instant end1 = Instant.parse("2026-10-14T12:00:00Z");
        Instant start2 = Instant.parse("2026-10-14T13:00:00Z");
        Instant end2 = Instant.parse("2026-10-14T16:00:00Z");

        List<Outcome> outcomes = runConcurrently(
                () -> sessionService.createOneOffDraftSession(
                        campaign.getId(), center.getId(), start1, end1, start1),
                () -> sessionService.createOneOffDraftSession(
                        campaign.getId(), center.getId(), start2, end2, start2)
        );

        assertEquals(2, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        List<VaccinationSession> committed = sessionService.getSessionsForCenter(center.getId());
        assertEquals(2, committed.size());
    }

    @Test
    @Timeout(30)
    void concurrentScheduleAttemptsFailClosedWithoutLifecycleOrEventMutation() throws Exception {
        VaccinationCampaign campaign = createCampaign("CONC-SCHEDULE-GUARD");
        VaccinationCenter center = createCenter("CONC-SCHEDULE-GUARD", CenterStatus.ACTIVE);
        setupPrerequisites(campaign, center);

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        VaccinationSession draft = sessionService.createOneOffDraftSession(
                campaign.getId(),
                center.getId(),
                startAt,
                Instant.parse("2026-10-14T12:00:00Z"),
                startAt.minusSeconds(3600)
        );

        List<Outcome> outcomes = runConcurrently(
                () -> sessionService.scheduleSession(draft.getId()),
                () -> sessionService.scheduleSession(draft.getId())
        );

        assertEquals(0, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        assertTrue(outcomes.stream().allMatch(outcome -> outcome.error() instanceof IllegalStateException));
        assertTrue(outcomes.stream().allMatch(outcome -> outcome.error().getMessage().equals(
                "Session scheduling is unavailable until operational resource and capacity readiness can be validated"
        )));
        assertEquals(
                SessionLifecycleStatus.DRAFT,
                sessionRepository.findById(draft.getId()).orElseThrow().getLifecycleStatus()
        );
        assertTrue(sessionService.getSessionChangeEvents(draft.getId()).isEmpty());
    }

    @Test
    @Timeout(30)
    void creationRacingWithOverlappingClosureRejectsWhenClosureCommitsFirst() throws Exception {
        VaccinationCampaign campaign = createCampaign("CONC-CLOSURE");
        VaccinationCenter center = createCenter("CONC-CLOSURE", CenterStatus.ACTIVE);
        setupPrerequisites(campaign, center);

        Instant startAt = Instant.parse("2026-10-14T09:00:00Z");
        Instant endAt = Instant.parse("2026-10-14T12:00:00Z");

        // Concurrent action 1: creates closure overlapping the session
        // Concurrent action 2: creates session
        List<Outcome> outcomes = runConcurrently(
                () -> closureService.createClosure(
                        center.getId(), startAt, endAt, "Emergency sanitation"),
                () -> sessionService.createOneOffDraftSession(
                        campaign.getId(), center.getId(), startAt, endAt, startAt)
        );

        // At least one operation succeeded
        assertTrue(outcomes.stream().anyMatch(Outcome::success));
        boolean closureCreated = !closureService.getClosuresForCenter(center.getId()).isEmpty();
        if (closureCreated) {
            // Once closure exists, subsequent session creation must definitely be rejected
            try {
                sessionService.createOneOffDraftSession(
                        campaign.getId(), center.getId(), startAt, endAt, startAt);
                org.junit.jupiter.api.Assertions.fail("Expected exception when creating session overlapping active closure");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("overlaps an active center closure"));
            }
        }
    }

    private void setupPrerequisites(VaccinationCampaign campaign, VaccinationCenter center) {
        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                center, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0),
                CAMPAIGN_START, CAMPAIGN_END.plusDays(1)));
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

    private record Outcome(boolean success, Throwable error) {
    }
}
