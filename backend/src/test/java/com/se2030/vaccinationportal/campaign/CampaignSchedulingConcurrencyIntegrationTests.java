package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignQuestionnaireAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.se2030.vaccinationportal.campaign.service.QuestionnaireVersionPublicationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CampaignSchedulingConcurrencyIntegrationTests {

    @TestConfiguration
    static class ConcurrencySchedulingTestConfig {
        @Bean
        @Primary
        public QuestionnaireVersionPublicationPort testPublicationPort() {
            return versionId -> versionId != null && versionId > 0;
        }
    }

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private CampaignQuestionnaireAssignmentRepository questionnaireAssignmentRepository;
    @Autowired private CampaignCenterAssignmentRepository centerAssignmentRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private Clock clock;

    @Test
    @Timeout(30)
    @DisplayName("Simultaneous scheduleCampaign calls serialize deterministically: exactly 1 succeeds and 1 fails with IllegalStateException")
    void simultaneousSchedulingSerializesToOneTransition() throws Exception {
        VaccinationCampaign campaign = persistValidDraftCampaign();
        Long campaignId = campaign.getId();

        List<Outcome> outcomes = runConcurrently(
                () -> campaignService.scheduleCampaign(campaignId),
                () -> campaignService.scheduleCampaign(campaignId)
        );

        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejected = outcomes.stream().filter(outcome -> !outcome.success()).findFirst().orElseThrow();
        assertInstanceOf(IllegalStateException.class, rejected.error(), outcomes.toString());
        assertTrue(rejected.error().getMessage().contains("Cannot schedule campaign in SCHEDULED status"));

        VaccinationCampaign finalCampaign = campaignRepository.findById(campaignId).orElseThrow();
        assertEquals(CampaignStatus.SCHEDULED, finalCampaign.getStatus());
    }

    private List<Outcome> runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome> firstFuture = executor.submit(contender(first, ready, start));
            Future<Outcome> secondFuture = executor.submit(contender(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Scheduling contenders did not become ready");
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

    private VaccinationCampaign persistValidDraftCampaign() {
        String suffix = Long.toUnsignedString(System.nanoTime());
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-CON-SCHED-" + suffix, "Concurrent Scheduling Vaccine", "dose", true));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-CON-SCHED-" + suffix, "Concurrent Scheduling Group", true));

        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-CON-SCHED-" + suffix, "Concurrent Scheduling Campaign", null,
                vaccine, group,
                start, end,
                start.atStartOfDay(ZoneId.of("UTC")).toInstant(),
                end.atStartOfDay(ZoneId.of("UTC")).toInstant(),
                CampaignStatus.DRAFT
        ));

        doseProgrammeRepository.saveAndFlush(new CampaignDoseProgramme(
                campaign, 1, DoseCategory.PRIMARY, "1,2", 28, false, 10, start, null
        ));

        questionnaireAssignmentRepository.saveAndFlush(new CampaignQuestionnaireAssignment(
                campaign, 501L, start, null
        ));

        VaccinationCenter center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-CON-SCHED-" + suffix, "Center " + suffix, "123 Main St", "contact@test.org",
                CenterStatus.ACTIVE, 100, null, "UTC"
        ));

        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, start, null
        ));

        return campaign;
    }

    private record Outcome(boolean success, Throwable error) {
    }
}
