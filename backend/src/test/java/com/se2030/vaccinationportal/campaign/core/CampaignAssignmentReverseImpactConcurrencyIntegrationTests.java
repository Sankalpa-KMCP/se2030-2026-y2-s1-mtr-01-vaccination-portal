package com.se2030.vaccinationportal.campaign.core;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.center.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.center.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.center.service.CampaignCenterAssignmentService;
import com.se2030.vaccinationportal.campaign.core.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.campaign.session.planning.service.VaccinationSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency integration tests verifying parent VaccinationCampaign write serialization
 * between concurrent session creation and campaign service date / assignment changes.
 */
@SpringBootTest
@ActiveProfiles("test")
class CampaignAssignmentReverseImpactConcurrencyIntegrationTests {

    @org.springframework.boot.test.context.TestConfiguration
    static class DateClockConfig {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        Clock campaignDateClock() {
            return Clock.fixed(java.time.Instant.parse("2026-05-01T12:00:00Z"), java.time.ZoneOffset.UTC);
        }
    }

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private CampaignCenterAssignmentService assignmentService;
    @Autowired private VaccinationSessionService sessionService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private Clock clock;

    private TransactionTemplate transactionTemplate;
    private VaccineType vaccineType;
    private TargetGroup targetGroup;
    private VaccinationCenter center;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            sessionRepository.deleteAllInBatch();
            assignmentRepository.deleteAllInBatch();
            operatingHoursRepository.deleteAllInBatch();
            campaignRepository.deleteAllInBatch();
            centerRepository.deleteAllInBatch();
            targetGroupRepository.deleteAllInBatch();
            vaccineTypeRepository.deleteAllInBatch();

            vaccineType = vaccineTypeRepository.saveAndFlush(
                    new VaccineType("VAX-CONCUR-REV", "Concur Rev Vaccine", "dose", true)
            );
            targetGroup = targetGroupRepository.saveAndFlush(
                    new TargetGroup("TG-CONCUR-REV", "Concur Rev Target", true)
            );
            center = centerRepository.saveAndFlush(
                    new VaccinationCenter(
                            "CTR-CONCUR-REV",
                            "Concur Rev Center",
                            "123 Main St",
                            "555-0199",
                            CenterStatus.ACTIVE,
                            50,
                            20,
                            "UTC"
                    )
            );

            for (DayOfWeek day : DayOfWeek.values()) {
                operatingHoursRepository.saveAndFlush(
                        new OperatingHours(
                                center,
                                day,
                                LocalTime.of(8, 0),
                                LocalTime.of(20, 0),
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 12, 31)
                        )
                );
            }
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            sessionRepository.deleteAllInBatch();
            assignmentRepository.deleteAllInBatch();
            operatingHoursRepository.deleteAllInBatch();
            campaignRepository.deleteAllInBatch();
            centerRepository.deleteAllInBatch();
            targetGroupRepository.deleteAllInBatch();
            vaccineTypeRepository.deleteAllInBatch();
        });
    }

    @Test
    @DisplayName("Concurrent session creation and campaign service date shortening serialize deterministically")
    void concurrentSessionCreationAndCampaignDateShorteningSerialize() throws Exception {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        Long campaignId = transactionTemplate.execute(status -> {
            VaccinationCampaign campaign = campaignRepository.saveAndFlush(
                    CampaignTestFixtures.campaign(
                            "CMP-CONCUR-01",
                            "Concur Campaign 01",
                            "Desc",
                            vaccineType,
                            targetGroup,
                            start,
                            end,
                            Instant.parse("2026-05-01T08:00:00Z"),
                            end.atTime(23, 59, 59).toInstant(ZoneOffset.UTC),
                            CampaignStatus.ACTIVE
                    )
            );
            assignmentRepository.saveAndFlush(
                    new CampaignCenterAssignment(campaign, center, start, end)
            );
            return campaign.getId();
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch competingAttemptLatch = new CountDownLatch(1);

        AtomicBoolean shortenSuccess = new AtomicBoolean(false);
        AtomicBoolean createSuccess = new AtomicBoolean(false);

        // Thread 1: Shortens campaign dates to 2026-06-15 to 2026-08-31
        Future<?> tx1 = executor.submit(() -> {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    assertTrue(startLatch.await(5, TimeUnit.SECONDS), "startLatch timed out in Thread 1");
                    // Lock campaign first
                    VaccinationCampaign c = campaignRepository.findByIdForUpdate(campaignId).orElseThrow();
                    lockAcquiredLatch.countDown();
                    // Wait while still holding the pessimistic write lock until Thread 2 has reached its competing attempt
                    assertTrue(competingAttemptLatch.await(5, TimeUnit.SECONDS), "competingAttemptLatch timed out in Thread 1");
                    campaignService.updateServiceDates(campaignId, LocalDate.of(2026, 6, 15), end);
                    shortenSuccess.set(true);
                } catch (Exception ex) {
                    // Exception if concurrent session was persisted first
                }
            });
        });

        // Thread 2: Creates a session on 2026-06-05 (which falls outside proposed shortened date 2026-06-15)
        Future<?> tx2 = executor.submit(() -> {
            try {
                assertTrue(startLatch.await(5, TimeUnit.SECONDS), "startLatch timed out in Thread 2");
                // Wait until Thread 1 has acquired campaign lock
                assertTrue(lockAcquiredLatch.await(5, TimeUnit.SECONDS), "lockAcquiredLatch timed out in Thread 2");
                transactionTemplate.executeWithoutResult(status -> {
                    Instant sessionStart = Instant.parse("2026-06-05T09:00:00Z");
                    Instant sessionEnd = Instant.parse("2026-06-05T12:00:00Z");
                    // Signal that Thread 2 is immediately entering the competing session creation call
                    competingAttemptLatch.countDown();
                    VaccinationSession s = sessionService.createOneOffDraftSession(
                            campaignId, center.getId(), sessionStart, sessionEnd, sessionStart.minusSeconds(3600)
                    );
                    s.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
                    sessionRepository.saveAndFlush(s);
                    createSuccess.set(true);
                });
            } catch (Exception ex) {
                // Exception if Thread 1 finished first and shortened campaign start to 2026-06-15
            }
        });

        startLatch.countDown();
        tx1.get(5, TimeUnit.SECONDS);
        tx2.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Enforce deterministic outcome: Thread 1 acquired lock first, so date shortening succeeds and conflicting session creation fails
        assertTrue(shortenSuccess.get(), "Campaign service date shortening must succeed");
        assertFalse(createSuccess.get(), "Conflicting session creation outside shortened range must fail");

        // Verify final persisted database state
        VaccinationCampaign persistedCampaign = campaignRepository.findById(campaignId).orElseThrow();
        assertEquals(LocalDate.of(2026, 6, 15), persistedCampaign.getServiceStartDate());
        assertEquals(0, sessionRepository.count(), "No invalid session outside shortened campaign service dates should be persisted");
    }
}
