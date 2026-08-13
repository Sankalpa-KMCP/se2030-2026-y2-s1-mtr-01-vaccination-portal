package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.SessionRecurrenceRuleService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Set;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** H2/MySQL mode verifies transaction structure, not production MySQL lock behavior. */
@SpringBootTest
@ActiveProfiles("test")
@Import(SessionRecurrenceRuleConcurrencyIntegrationTests.FixedClockConfig.class)
class SessionRecurrenceRuleConcurrencyIntegrationTests {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-09-15T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired private SessionRecurrenceRuleService service;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @PersistenceContext private EntityManager entityManager;

    @BeforeEach
    void clearPersistedState() {
        ruleRepository.deleteAllInBatch();
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
    void preloadedRuleIsAuthoritativelyRefreshedBeforeMutation() {
        VaccinationCampaign campaign = campaign("AUTHORITATIVE-REFRESH");
        VaccinationCenter center = center("AUTHORITATIVE-REFRESH");
        SessionRecurrenceRule rule = service.createRule(
                campaign.getId(), center.getId(), Set.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 5),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "UTC");
        String generationKey = rule.getGenerationKey();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            assertThrows(UnexpectedRollbackException.class,
                    () -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        SessionRecurrenceRule preloaded = ruleRepository.findById(rule.getId())
                                .orElseThrow();
                        assertTrue(entityManager.contains(preloaded));
                        assertEquals(SessionRecurrenceRuleStatus.ACTIVE, preloaded.getStatus());

                        Future<?> committedCancellation = executor.submit(
                                () -> service.cancelRule(rule.getId()));
                        awaitWorker(committedCancellation);

                        assertEquals(SessionRecurrenceRuleStatus.ACTIVE, preloaded.getStatus(),
                                "Transaction A must retain its stale pre-lock state");
                        assertThrows(IllegalArgumentException.class,
                                () -> service.updateFutureRule(
                                        rule.getId(), Set.of(DayOfWeek.MONDAY),
                                        LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 12),
                                        LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC"));
                        assertEquals(SessionRecurrenceRuleStatus.CANCELLED, preloaded.getStatus(),
                                "The locked refresh must replace stale managed state");
                    }));
        } finally {
            executor.shutdownNow();
            awaitExecutorTermination(executor);
        }

        SessionRecurrenceRule committed = service.getRuleById(rule.getId()).orElseThrow();
        assertEquals(SessionRecurrenceRuleStatus.CANCELLED, committed.getStatus());
        assertEquals(LocalDate.of(2026, 9, 21), committed.getStartDate());
        assertEquals(LocalDate.of(2026, 10, 5), committed.getEndDate());
        assertEquals(generationKey, committed.getGenerationKey());
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

    private VaccinationCampaign campaign(String suffix) {
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-SRR-" + suffix, "Vaccine " + suffix));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-SRR-" + suffix, "Group " + suffix));
        return campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-SRR-" + suffix, "Campaign " + suffix, null, vaccine, group,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.DRAFT));
    }

    private VaccinationCenter center(String suffix) {
        return centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-SRR-" + suffix, "Center " + suffix, "1 Test Road", "+94 11 000 0000",
                CenterStatus.ACTIVE, 100, 50, "UTC"));
    }
}
