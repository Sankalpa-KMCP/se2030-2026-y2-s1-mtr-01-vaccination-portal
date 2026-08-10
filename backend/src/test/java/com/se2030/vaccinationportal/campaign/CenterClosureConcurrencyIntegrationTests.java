package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.service.CenterClosureService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
 * Worker threads invoke the transactional service proxy and therefore own separate transactions.
 * H2 in MySQL mode provides structural locking evidence, not proof of production-MySQL equivalence.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(CenterClosureConcurrencyIntegrationTests.FixedClockConfig.class)
class CenterClosureConcurrencyIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired private CenterClosureService service;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private VaccinationCenterRepository centerRepository;

    @BeforeEach
    void clearPersistedState() {
        closureRepository.deleteAllInBatch();
        centerRepository.deleteAllInBatch();
    }

    @AfterEach
    void clearPersistedStateAfterTest() {
        clearPersistedState();
    }

    @Test
    @Timeout(30)
    void overlappingCreatesForSameCenterRetainOneActiveClosure() throws Exception {
        VaccinationCenter center = center("SAME-CREATE");

        List<Outcome> outcomes = runConcurrently(
                () -> service.createClosure(center.getId(), future(2), future(6), "First"),
                () -> service.createClosure(center.getId(), future(4), future(8), "Second")
        );

        assertOneSuccessAndOneDomainRejection(outcomes);
        List<CenterClosure> committed = service.getClosuresForCenter(center.getId());
        assertEquals(1, committed.size());
        assertEquals(CenterClosureStatus.ACTIVE, committed.get(0).getStatus());
    }

    @Test
    @Timeout(30)
    void createRacingOverlappingUpdateCannotCommitAConflict() throws Exception {
        VaccinationCenter center = center("CREATE-UPDATE");
        CenterClosure existing = service.createClosure(
                center.getId(), future(6), future(8), "Existing");

        List<Outcome> outcomes = runConcurrently(
                () -> service.createClosure(center.getId(), future(2), future(5), "Created"),
                () -> service.updateFutureClosure(
                        existing.getId(), future(4), future(7), "Updated")
        );

        assertOneSuccessAndOneDomainRejection(outcomes);
        assertNoActiveOverlap(service.getClosuresForCenter(center.getId()));
    }

    @Test
    @Timeout(30)
    void writesForDifferentCentersRemainIndependent() throws Exception {
        VaccinationCenter first = center("INDEPENDENT-A");
        VaccinationCenter second = center("INDEPENDENT-B");

        List<Outcome> outcomes = runConcurrently(
                () -> service.createClosure(first.getId(), future(2), future(6), "First center"),
                () -> service.createClosure(second.getId(), future(2), future(6), "Second center")
        );

        assertEquals(2, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        assertEquals(1, service.getClosuresForCenter(first.getId()).size());
        assertEquals(1, service.getClosuresForCenter(second.getId()).size());
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

    private void assertOneSuccessAndOneDomainRejection(List<Outcome> outcomes) {
        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(outcome -> !outcome.success()).findFirst().orElseThrow();
        assertInstanceOf(IllegalArgumentException.class, rejected.error(), outcomes.toString());
    }

    private void assertNoActiveOverlap(List<CenterClosure> closures) {
        List<CenterClosure> active = closures.stream()
                .filter(closure -> closure.getStatus() == CenterClosureStatus.ACTIVE)
                .toList();
        for (int left = 0; left < active.size(); left++) {
            for (int right = left + 1; right < active.size(); right++) {
                assertFalse(overlaps(active.get(left), active.get(right)), active.toString());
            }
        }
    }

    private boolean overlaps(CenterClosure first, CenterClosure second) {
        return first.getStartAt().isBefore(second.getEndAt())
                && second.getStartAt().isBefore(first.getEndAt());
    }

    private VaccinationCenter center(String suffix) {
        return centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-" + suffix,
                "Center " + suffix,
                "1 Test Road",
                "+94 11 000 0000",
                CenterStatus.ACTIVE,
                100,
                50
        ));
    }

    private static Instant future(long hours) {
        return NOW.plusSeconds(hours * 3600);
    }

    private record Outcome(boolean success, Throwable error) {
    }
}
