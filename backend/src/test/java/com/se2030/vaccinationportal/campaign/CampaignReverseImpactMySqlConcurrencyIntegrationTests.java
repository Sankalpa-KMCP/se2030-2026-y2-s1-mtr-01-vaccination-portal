package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffIdentityRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Production-equivalent MySQL Testcontainers concurrency tests verifying cross-entity
 * parent/child reverse-impact locking between campaign service date modification and session creation.
 */
class CampaignReverseImpactMySqlConcurrencyIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Autowired private VaccinationSessionService sessionService;
    @Autowired private SessionResourceAssignmentRepository resourceAssignmentRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private SessionCapacityPlanRepository capacityPlanRepository;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private OperationalStaffRosterRepository staffRosterRepository;
    @Autowired private OperationalStaffIdentityRepository identityRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private VaccineType vaccineType;
    private TargetGroup targetGroup;
    private VaccinationCenter center;

    @BeforeEach
    void setUp() throws Exception {
        assertConnectedToRealMySql();

        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            resourceAssignmentRepository.deleteAllInBatch();
            staffAssignmentRepository.deleteAllInBatch();
            capacityPlanRepository.deleteAllInBatch();
            sessionChangeEventRepository.deleteAllInBatch();
            sessionRepository.deleteAllInBatch();
            ruleRepository.deleteAllInBatch();
            resourceRepository.deleteAllInBatch();
            staffRosterRepository.deleteAllInBatch();
            identityRepository.deleteAllInBatch();
            assignmentRepository.deleteAllInBatch();
            closureRepository.deleteAllInBatch();
            operatingHoursRepository.deleteAllInBatch();
            doseProgrammeRepository.deleteAllInBatch();
            campaignRepository.deleteAllInBatch();
            centerRepository.deleteAllInBatch();
            targetGroupRepository.deleteAllInBatch();
            vaccineTypeRepository.deleteAllInBatch();

            vaccineType = vaccineTypeRepository.saveAndFlush(
                    new VaccineType("VAX-MYSQL-REV", "MySQL Rev Vaccine", "dose", true)
            );
            targetGroup = targetGroupRepository.saveAndFlush(
                    new TargetGroup("TG-MYSQL-REV", "MySQL Rev Target", true)
            );
            center = centerRepository.saveAndFlush(
                    new VaccinationCenter(
                            "CTR-MYSQL-REV",
                            "MySQL Rev Center",
                            "123 MySQL Ave",
                            "555-0188",
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
    @Timeout(30)
    @DisplayName("Concurrent session creation and campaign service date shortening serialize deterministically on MySQL")
    void concurrentSessionCreationAndCampaignDateShorteningSerializeOnMySql() throws Exception {
        assertConnectedToRealMySql();

        LocalDate initialStart = LocalDate.of(2026, 6, 1);
        LocalDate initialEnd = LocalDate.of(2026, 8, 31);

        Long campaignId = transactionTemplate.execute(status -> {
            VaccinationCampaign campaign = campaignRepository.saveAndFlush(
                    CampaignTestFixtures.campaign(
                            "CMP-MYSQL-REV-01",
                            "MySQL Concur Campaign",
                            "Desc",
                            vaccineType,
                            targetGroup,
                            initialStart,
                            initialEnd,
                            Instant.parse("2026-05-01T08:00:00Z"),
                            Instant.parse("2026-12-31T18:00:00Z"),
                            CampaignStatus.ACTIVE
                    )
            );
            assignmentRepository.saveAndFlush(
                    new CampaignCenterAssignment(campaign, center, initialStart, initialEnd)
            );
            return campaign.getId();
        });

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch competingAttemptLatch = new CountDownLatch(1);

        AtomicBoolean shortenSuccess = new AtomicBoolean(false);
        AtomicBoolean createSuccess = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Task A: Shortens campaign serviceStartDate from 2026-06-01 to 2026-06-15 while holding campaign lock
            Future<?> shortenFuture = executor.submit(() -> {
                try {
                    assertTrue(startLatch.await(5, TimeUnit.SECONDS), "Task A timed out waiting for startLatch");
                    transactionTemplate.executeWithoutResult(status -> {
                        VaccinationCampaign lockedCampaign = campaignRepository.findByIdForUpdate(campaignId).orElseThrow();
                        lockAcquiredLatch.countDown();

                        try {
                            assertTrue(
                                    competingAttemptLatch.await(5, TimeUnit.SECONDS),
                                    "Task A timed out waiting for competing session creation attempt"
                            );
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted while holding campaign lock", ex);
                        }

                        lockedCampaign.setServiceStartDate(LocalDate.of(2026, 6, 15));
                        campaignRepository.saveAndFlush(lockedCampaign);
                    });
                    shortenSuccess.set(true);
                } catch (Exception e) {
                    shortenSuccess.set(false);
                }
            });

            // Task B: Attempts to create a session on 2026-06-10 (valid under original dates, invalid under shortened dates)
            Future<?> createFuture = executor.submit(() -> {
                try {
                    assertTrue(startLatch.await(5, TimeUnit.SECONDS), "Task B timed out waiting for startLatch");
                    assertTrue(lockAcquiredLatch.await(5, TimeUnit.SECONDS), "Task B timed out waiting for lockAcquiredLatch");

                    transactionTemplate.executeWithoutResult(status -> {
                        competingAttemptLatch.countDown();
                        Instant sessionStart = Instant.parse("2026-06-05T09:00:00Z");
                        Instant sessionEnd = Instant.parse("2026-06-05T12:00:00Z");
                        Instant bookingOpen = sessionStart.minusSeconds(3600);

                        sessionService.createOneOffDraftSession(
                                campaignId,
                                center.getId(),
                                sessionStart,
                                sessionEnd,
                                bookingOpen
                        );
                    });
                    createSuccess.set(true);
                } catch (Exception e) {
                    createSuccess.set(false);
                }
            });

            startLatch.countDown();
            shortenFuture.get(10, TimeUnit.SECONDS);
            createFuture.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertTrue(shortenSuccess.get(), "Campaign service date shortening must succeed on MySQL");
        assertFalse(createSuccess.get(), "Conflicting session creation outside shortened range must fail on MySQL");

        VaccinationCampaign persistedCampaign = campaignRepository.findById(campaignId).orElseThrow();
        assertEquals(LocalDate.of(2026, 6, 15), persistedCampaign.getServiceStartDate());
        assertEquals(0, sessionRepository.count(), "No invalid session outside shortened campaign dates should be persisted in MySQL");
    }
}
