package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignmentStatus;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CampaignCenterAssignmentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Worker calls enter separate transactions through the service proxy. H2/MySQL mode provides
 * structural locking evidence and does not prove production MySQL behavior.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(CampaignCenterAssignmentConcurrencyIntegrationTests.FixedClockConfig.class)
class CampaignCenterAssignmentConcurrencyIntegrationTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-09-15T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired private CampaignCenterAssignmentService service;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @PersistenceContext private EntityManager entityManager;

    @BeforeEach
    void clearPersistedState() {
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
    void overlappingCreatesForSamePairCommitExactlyOneAssignment() throws Exception {
        VaccinationCampaign campaign = campaign("SAME-CREATE");
        VaccinationCenter center = center("SAME-CREATE");

        List<Outcome> outcomes = runConcurrently(
                () -> service.createAssignment(campaign.getId(), center.getId(), day(2), day(6)),
                () -> service.createAssignment(campaign.getId(), center.getId(), day(4), day(8))
        );

        assertOneSuccessAndOneDomainRejection(outcomes);
        List<CampaignCenterAssignment> committed = service.getAssignmentsForCampaign(campaign.getId());
        assertEquals(1, committed.size());
        assertEquals(CampaignCenterAssignmentStatus.ACTIVE, committed.get(0).getStatus());
    }

    @Test
    @Timeout(30)
    void createRacingWithFutureUpdateCannotCommitOverlap() throws Exception {
        VaccinationCampaign campaign = campaign("CREATE-UPDATE");
        VaccinationCenter center = center("CREATE-UPDATE");
        CampaignCenterAssignment existing = service.createAssignment(
                campaign.getId(), center.getId(), day(6), day(8));

        List<Outcome> outcomes = runConcurrently(
                () -> service.createAssignment(campaign.getId(), center.getId(), day(2), day(5)),
                () -> service.updateFutureAssignment(existing.getId(), day(4), day(7))
        );

        assertOneSuccessAndOneDomainRejection(outcomes);
        assertNoActiveOverlap(service.getAssignmentsForCampaign(campaign.getId()));
    }

    @Test
    @Timeout(30)
    void writesForDifferentCampaignsRemainIndependent() throws Exception {
        VaccinationCampaign first = campaign("INDEPENDENT-A");
        VaccinationCampaign second = campaign("INDEPENDENT-B");
        VaccinationCenter center = center("INDEPENDENT");

        List<Outcome> outcomes = runConcurrently(
                () -> service.createAssignment(first.getId(), center.getId(), day(2), day(6)),
                () -> service.createAssignment(second.getId(), center.getId(), day(2), day(6))
        );

        assertEquals(2, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        assertEquals(1, service.getAssignmentsForCampaign(first.getId()).size());
        assertEquals(1, service.getAssignmentsForCampaign(second.getId()).size());
    }

    @Test
    @Timeout(30)
    void preloadedAssignmentIsRefreshedAfterCampaignLockBeforeMutation() {
        VaccinationCampaign campaign = campaign("AUTHORITATIVE-REFRESH");
        VaccinationCenter center = center("AUTHORITATIVE-REFRESH");
        CampaignCenterAssignment assignment = service.createAssignment(
                campaign.getId(), center.getId(), day(2), day(6));
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            assertThrows(UnexpectedRollbackException.class,
                    () -> new TransactionTemplate(transactionManager).executeWithoutResult(transactionStatus -> {
                        CampaignCenterAssignment preloaded = assignmentRepository.findById(assignment.getId())
                                .orElseThrow();
                        assertTrue(entityManager.contains(preloaded));
                        assertEquals(CampaignCenterAssignmentStatus.ACTIVE, preloaded.getStatus());

                        Future<?> committedCancellation = executor.submit(
                                () -> service.cancelFutureAssignment(assignment.getId()));
                        awaitWorker(committedCancellation);

                        assertEquals(CampaignCenterAssignmentStatus.ACTIVE, preloaded.getStatus(),
                                "Transaction A must still hold the stale pre-lock state");
                        assertThrows(IllegalArgumentException.class,
                                () -> service.updateFutureAssignment(assignment.getId(), day(3), day(7)));
                        assertEquals(CampaignCenterAssignmentStatus.CANCELLED, preloaded.getStatus(),
                                "Post-lock refresh must replace stale managed state");
                    }));
        } finally {
            executor.shutdownNow();
            awaitExecutorTermination(executor);
        }

        CampaignCenterAssignment committed = service.getAssignmentById(assignment.getId()).orElseThrow();
        assertEquals(CampaignCenterAssignmentStatus.CANCELLED, committed.getStatus());
        assertEquals(day(2), committed.getActiveFrom());
        assertEquals(day(6), committed.getActiveTo());
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
            return List.of(
                    firstFuture.get(10, TimeUnit.SECONDS),
                    secondFuture.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate");
        }
    }

    private void awaitWorker(Future<?> future) {
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for worker", error);
        } catch (ExecutionException | TimeoutException error) {
            throw new AssertionError("Worker did not complete successfully", error);
        }
    }

    private void awaitExecutorTermination(ExecutorService executor) {
        try {
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while stopping executor", error);
        }
    }

    private Callable<Outcome> contender(
            Callable<?> action,
            CountDownLatch ready,
            CountDownLatch start
    ) {
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

    private void assertOneSuccessAndOneDomainRejection(List<Outcome> outcomes) {
        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(outcome -> !outcome.success()).findFirst().orElseThrow();
        assertInstanceOf(IllegalArgumentException.class, rejected.error(), outcomes.toString());
    }

    private void assertNoActiveOverlap(List<CampaignCenterAssignment> assignments) {
        List<CampaignCenterAssignment> active = assignments.stream()
                .filter(a -> a.getStatus() == CampaignCenterAssignmentStatus.ACTIVE)
                .toList();
        for (int left = 0; left < active.size(); left++) {
            for (int right = left + 1; right < active.size(); right++) {
                assertFalse(overlaps(active.get(left), active.get(right)), active.toString());
            }
        }
    }

    private boolean overlaps(CampaignCenterAssignment first, CampaignCenterAssignment second) {
        return (first.getActiveTo() == null || second.getActiveFrom().isBefore(first.getActiveTo()))
                && (second.getActiveTo() == null || first.getActiveFrom().isBefore(second.getActiveTo()));
    }

    private VaccinationCampaign campaign(String suffix) {
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-CCA-" + suffix, "Vaccine " + suffix));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-CCA-" + suffix, "Group " + suffix));
        return campaignRepository.saveAndFlush(new VaccinationCampaign(
                "CMP-CCA-" + suffix, "Campaign " + suffix, null, vaccine, group,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.DRAFT));
    }

    private VaccinationCenter center(String suffix) {
        return centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-CCA-" + suffix, "Center " + suffix, "1 Test Road", "+94 11 000 0000",
                CenterStatus.ACTIVE, 100, 50, "UTC"));
    }

    private static LocalDate day(long offset) {
        return TODAY.plusDays(offset);
    }

    private record Outcome(boolean success, Throwable error) {
    }
}
