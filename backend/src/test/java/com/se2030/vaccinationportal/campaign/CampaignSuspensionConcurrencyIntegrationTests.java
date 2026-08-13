package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignLifecycleEventRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class CampaignSuspensionConcurrencyIntegrationTests {

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private CampaignLifecycleEventRepository eventRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    @Test
    @Timeout(30)
    void simultaneousSuspensionsSerializeToOneTransitionAndOneEvent() throws Exception {
        VaccinationCampaign campaign = persistPublishedCampaign();
        Long campaignId = campaign.getId();

        List<Outcome> outcomes = runConcurrently(
                () -> campaignService.suspendCampaign(campaignId, "First safety hold", "officer-one"),
                () -> campaignService.suspendCampaign(campaignId, "Second safety hold", "officer-two")
        );

        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(outcome -> !outcome.success()).findFirst().orElseThrow();
        assertInstanceOf(IllegalStateException.class, rejected.error(), outcomes.toString());
        assertEquals(CampaignStatus.SUSPENDED,
                campaignRepository.findById(campaignId).orElseThrow().getStatus());
        assertEquals(1, eventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaignId).size());
    }

    private List<Outcome> runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome> firstFuture = executor.submit(contender(first, ready, start));
            Future<Outcome> secondFuture = executor.submit(contender(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Suspension contenders did not become ready");
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

    private VaccinationCampaign persistPublishedCampaign() {
        String suffix = Long.toUnsignedString(System.nanoTime());
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-CON-SUSP-" + suffix, "Concurrent Suspension Vaccine"));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-CON-SUSP-" + suffix, "Concurrent Suspension Group"));
        return campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-CON-SUSP-" + suffix, "Concurrent Suspension Campaign", null,
                vaccine, group,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.PUBLISHED
        ));
    }

    private record Outcome(boolean success, Throwable error) {
    }
}
