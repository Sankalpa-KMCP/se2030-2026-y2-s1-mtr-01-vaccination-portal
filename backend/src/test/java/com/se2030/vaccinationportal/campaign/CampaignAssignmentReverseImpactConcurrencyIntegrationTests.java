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
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CampaignCenterAssignmentService;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency integration tests verifying parent VaccinationCampaign write serialization
 * between concurrent session creation and campaign service date / assignment changes.
 */
@SpringBootTest
@ActiveProfiles("test")
class CampaignAssignmentReverseImpactConcurrencyIntegrationTests {

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

    @Test
    @DisplayName("Concurrent session creation and campaign service date shortening serialize deterministically")
    void concurrentSessionCreationAndCampaignDateShorteningSerialize() throws Exception {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        Long campaignId = transactionTemplate.execute(status -> {
            VaccinationCampaign campaign = campaignRepository.saveAndFlush(
                    new VaccinationCampaign(
                            "CMP-CONCUR-01",
                            "Concur Campaign 01",
                            "Desc",
                            vaccineType,
                            targetGroup,
                            start,
                            end,
                            Instant.parse("2026-05-01T08:00:00Z"),
                            Instant.parse("2026-12-31T18:00:00Z"),
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

        AtomicBoolean shortenSuccess = new AtomicBoolean(false);
        AtomicBoolean createSuccess = new AtomicBoolean(false);

        // Thread 1: Shortens campaign dates to 2026-06-15 to 2026-08-31
        Future<?> tx1 = executor.submit(() -> {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    startLatch.await();
                    // Lock campaign first
                    VaccinationCampaign c = campaignRepository.findByIdForUpdate(campaignId).orElseThrow();
                    lockAcquiredLatch.countDown();
                    // Hold lock briefly to simulate processing
                    Thread.sleep(300);
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
                startLatch.await();
                // Wait until Thread 1 has acquired campaign lock or started
                lockAcquiredLatch.await();
                transactionTemplate.executeWithoutResult(status -> {
                    Instant sessionStart = Instant.parse("2026-06-05T09:00:00Z");
                    Instant sessionEnd = Instant.parse("2026-06-05T12:00:00Z");
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

        // Exactly one of the two concurrent operations must succeed and one must fail
        assertTrue(shortenSuccess.get() ^ createSuccess.get(),
                "Exactly one concurrent operation must succeed (shortenSuccess=" + shortenSuccess.get() + ", createSuccess=" + createSuccess.get() + ")");
    }
}
