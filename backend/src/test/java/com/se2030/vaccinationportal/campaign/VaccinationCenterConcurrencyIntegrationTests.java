package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.service.VaccinationCenterService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.concurrent.ExecutionException;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** H2/MySQL mode verifies authoritative center refresh across genuine separate transactions. */
@SpringBootTest
@ActiveProfiles("test")
class VaccinationCenterConcurrencyIntegrationTests {

    @Autowired private VaccinationCenterService service;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @PersistenceContext private EntityManager entityManager;

    @BeforeEach
    void clearPersistedState() {
        centerRepository.deleteAllInBatch();
    }

    @AfterEach
    void clearPersistedStateAfterTest() {
        clearPersistedState();
    }

    @Test
    @Timeout(30)
    void preloadedActiveCenterIsRefreshedAfterCommittedDeactivationBeforeUpdate() {
        VaccinationCenter center = createCenter("DEACTIVATE");
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                VaccinationCenter preloaded = centerRepository.findById(center.getId()).orElseThrow();
                assertTrue(entityManager.contains(preloaded));
                assertEquals(CenterStatus.ACTIVE, preloaded.getCenterStatus());

                Future<?> committedDeactivation = executor.submit(
                        () -> service.deactivateCenter(center.getId()));
                awaitWorker(committedDeactivation);

                assertEquals(CenterStatus.ACTIVE, preloaded.getCenterStatus(),
                        "The outer transaction must still hold stale pre-lock state");

                VaccinationCenter updated = service.updateCenter(
                        center.getId(), "Updated center", "2 Test Road", "+94 11 111 1111", 120, 60);

                assertEquals(CenterStatus.INACTIVE, updated.getCenterStatus(),
                        "Post-lock refresh must preserve the committed deactivation");
                assertEquals("Asia/Colombo", updated.getTimezone());
            });
        } finally {
            executor.shutdownNow();
            awaitExecutorTermination(executor);
        }

        VaccinationCenter committed = service.getCenterById(center.getId()).orElseThrow();
        assertEquals(CenterStatus.INACTIVE, committed.getCenterStatus());
        assertEquals("Updated center", committed.getName());
        assertEquals("Asia/Colombo", committed.getTimezone());
    }

    @Test
    @Timeout(30)
    void preloadedInactiveCenterIsRefreshedAfterCommittedActivationBeforeUpdate() {
        VaccinationCenter center = createCenter("ACTIVATE");
        service.deactivateCenter(center.getId());
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                VaccinationCenter preloaded = centerRepository.findById(center.getId()).orElseThrow();
                assertTrue(entityManager.contains(preloaded));
                assertEquals(CenterStatus.INACTIVE, preloaded.getCenterStatus());

                Future<?> committedActivation = executor.submit(
                        () -> service.activateCenter(center.getId()));
                awaitWorker(committedActivation);

                assertEquals(CenterStatus.INACTIVE, preloaded.getCenterStatus(),
                        "The outer transaction must still hold stale pre-lock state");

                VaccinationCenter updated = service.updateCenter(
                        center.getId(), "Reactivated center", "3 Test Road", "+94 11 222 2222", 130, 70);

                assertEquals(CenterStatus.ACTIVE, updated.getCenterStatus(),
                        "Post-lock refresh must preserve the committed activation");
                assertEquals("Asia/Colombo", updated.getTimezone());
            });
        } finally {
            executor.shutdownNow();
            awaitExecutorTermination(executor);
        }

        VaccinationCenter committed = service.getCenterById(center.getId()).orElseThrow();
        assertEquals(CenterStatus.ACTIVE, committed.getCenterStatus());
        assertEquals("Reactivated center", committed.getName());
        assertEquals("Asia/Colombo", committed.getTimezone());
    }

    private VaccinationCenter createCenter(String suffix) {
        return service.createCenter(
                "CTR-CENTER-" + suffix,
                "Center " + suffix,
                "1 Test Road",
                "+94 11 000 0000",
                100,
                50,
                "Asia/Colombo");
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
}
