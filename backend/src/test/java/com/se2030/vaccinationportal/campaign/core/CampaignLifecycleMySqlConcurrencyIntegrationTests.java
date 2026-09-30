package com.se2030.vaccinationportal.campaign.core;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleChangeType;
import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.core.repository.CampaignLifecycleEventRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.core.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Production-equivalent MySQL Testcontainers concurrency tests verifying single-row
 * command locking and deterministic serialization for campaign lifecycle operations.
 */
@Import(CampaignLifecycleMySqlConcurrencyIntegrationTests.FixedClockConfig.class)
class CampaignLifecycleMySqlConcurrencyIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    private static final Instant LIFECYCLE_INSTANT = Instant.parse("2026-09-15T10:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock mysqlTestClock() {
            return Clock.fixed(LIFECYCLE_INSTANT, ZoneOffset.UTC);
        }
    }

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private CampaignLifecycleEventRepository campaignLifecycleEventRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    @Test
    @Timeout(30)
    @DisplayName("Simultaneous resumeCampaign calls serialize deterministically on MySQL: exactly 1 succeeds and 1 fails with IllegalStateException")
    void simultaneousResumptionSerializesToOneTransitionOnMySql() throws Exception {
        assertConnectedToRealMySql();

        VaccinationCampaign campaign = persistSuspendedCampaign("CMP-MYSQL-RES-01");
        Long campaignId = campaign.getId();

        List<Outcome> outcomes = runConcurrently(
                () -> campaignService.resumeCampaign(campaignId, "Concurrent hold lift A on MySQL", "mysql-actor-A"),
                () -> campaignService.resumeCampaign(campaignId, "Concurrent hold lift B on MySQL", "mysql-actor-B")
        );

        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(outcome -> !outcome.success()).findFirst().orElseThrow();
        assertInstanceOf(IllegalStateException.class, rejected.error(), outcomes.toString());
        assertTrue(
                rejected.error().getMessage().contains("Cannot resume campaign in ACTIVE status")
                        || rejected.error().getMessage().contains("Cannot resume campaign in PUBLISHED status"),
                "Expected rejection message to indicate campaign is no longer SUSPENDED: " + rejected.error().getMessage()
        );

        VaccinationCampaign finalCampaign = campaignRepository.findById(campaignId).orElseThrow();
        assertEquals(CampaignStatus.ACTIVE, finalCampaign.getStatus());

        List<CampaignLifecycleEvent> events = campaignLifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaignId);
        assertEquals(1, events.size(), "Expected exactly one RESUMED event to be persisted on MySQL");
        CampaignLifecycleEvent event = events.get(0);
        assertEquals(CampaignLifecycleChangeType.RESUMED, event.getChangeType());
        assertEquals(CampaignStatus.SUSPENDED, event.getPreviousStatus());
        assertEquals(CampaignStatus.ACTIVE, event.getNewStatus());
    }

    @Test
    @Timeout(30)
    @DisplayName("Simultaneous activateCampaign calls serialize deterministically on MySQL: exactly 1 succeeds and 1 fails with IllegalStateException")
    void simultaneousActivationSerializesToOneTransitionOnMySql() throws Exception {
        assertConnectedToRealMySql();

        VaccinationCampaign campaign = persistPublishedCampaign("CMP-MYSQL-ACT-01");
        Long campaignId = campaign.getId();

        List<Outcome> outcomes = runConcurrently(
                () -> campaignService.activateCampaign(campaignId),
                () -> campaignService.activateCampaign(campaignId)
        );

        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(outcome -> !outcome.success()).findFirst().orElseThrow();
        assertInstanceOf(IllegalStateException.class, rejected.error(), outcomes.toString());
        assertTrue(
                rejected.error().getMessage().contains("Cannot activate campaign in ACTIVE status")
                        && rejected.error().getMessage().contains("Only PUBLISHED campaigns can be activated"),
                "Expected rejection message indicating campaign was already transitioned to ACTIVE by competing worker: " + rejected.error().getMessage()
        );

        VaccinationCampaign finalCampaign = campaignRepository.findById(campaignId).orElseThrow();
        assertEquals(CampaignStatus.ACTIVE, finalCampaign.getStatus());
    }

    private List<Outcome> runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome> firstFuture = executor.submit(contender(first, ready, start));
            Future<Outcome> secondFuture = executor.submit(contender(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Contenders did not become ready");
            start.countDown();
            return List.of(
                    firstFuture.get(10, TimeUnit.SECONDS),
                    secondFuture.get(10, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Outcome> contender(Callable<?> action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Contender timed out waiting for start");
            }
            try {
                Object result = action.call();
                return new Outcome(true, result, null);
            } catch (Exception ex) {
                return new Outcome(false, null, ex);
            }
        };
    }

    private record Outcome(boolean success, Object result, Throwable error) {}

    private VaccinationCampaign persistSuspendedCampaign(String code) {
        VaccineType vaccineType = vaccineTypeRepository.save(
                new VaccineType("VAX-" + code, "Vaccine " + code, "dose", true)
        );
        TargetGroup targetGroup = targetGroupRepository.save(
                new TargetGroup("TG-" + code, "Target Group " + code, true)
        );

        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                code,
                "Campaign " + code,
                "Description " + code,
                vaccineType,
                targetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-30T23:59:59Z"),
                CampaignStatus.SUSPENDED
        );
        return campaignRepository.save(campaign);
    }

    private VaccinationCampaign persistPublishedCampaign(String code) {
        VaccineType vaccineType = vaccineTypeRepository.save(
                new VaccineType("VAX-" + code, "Vaccine " + code, "dose", true)
        );
        TargetGroup targetGroup = targetGroupRepository.save(
                new TargetGroup("TG-" + code, "Target Group " + code, true)
        );

        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                code,
                "Campaign " + code,
                "Description " + code,
                vaccineType,
                targetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-30T23:59:59Z"),
                CampaignStatus.PUBLISHED
        );
        return campaignRepository.save(campaign);
    }
}
