package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests verifying deterministic serialization of simultaneous campaign activation attempts.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(CampaignActivationConcurrencyIntegrationTests.FixedClockConfig.class)
class CampaignActivationConcurrencyIntegrationTests {

    private static final Instant ACTIVE_DATE_INSTANT = Instant.parse("2026-09-15T10:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock activationClock() {
            return Clock.fixed(ACTIVE_DATE_INSTANT, ZoneOffset.UTC);
        }
    }

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    @Test
    @Timeout(30)
    @DisplayName("Simultaneous activateCampaign calls serialize deterministically: exactly 1 succeeds and 1 fails with IllegalStateException")
    void simultaneousActivationSerializesToOneTransition() throws Exception {
        VaccinationCampaign campaign = persistPublishedCampaign();
        Long campaignId = campaign.getId();

        List<Outcome> outcomes = runConcurrently(
                () -> campaignService.activateCampaign(campaignId),
                () -> campaignService.activateCampaign(campaignId)
        );

        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(outcome -> !outcome.success()).findFirst().orElseThrow();
        assertInstanceOf(IllegalStateException.class, rejected.error(), outcomes.toString());
        assertTrue(rejected.error().getMessage().contains("Cannot activate campaign in ACTIVE status"));

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
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Activation contenders did not become ready");
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

    private VaccinationCampaign persistPublishedCampaign() {
        VaccineType vaccineType = vaccineTypeRepository.save(
                new VaccineType("VAX-CONC-ACT", "Vaccine Concurrency Act", "dose", true)
        );
        TargetGroup targetGroup = targetGroupRepository.save(
                new TargetGroup("TG-CONC-ACT", "Target Group Concurrency Act", true)
        );

        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-CONC-ACT-01",
                "Campaign Concurrency Act",
                "Description",
                vaccineType,
                targetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-01T08:00:00Z"),
                Instant.parse("2026-12-30T18:00:00Z"),
                CampaignStatus.PUBLISHED
        );
        return campaignRepository.save(campaign);
    }
}
