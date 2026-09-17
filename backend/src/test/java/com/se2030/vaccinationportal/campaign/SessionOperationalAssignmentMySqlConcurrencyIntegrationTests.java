package com.se2030.vaccinationportal.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignLifecycleEventRepository;
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
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.SessionResourceAssignmentService;
import com.se2030.vaccinationportal.campaign.service.StaffAssignmentService;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
 * Production-equivalent MySQL Testcontainers concurrency tests verifying deterministic serialization
 * for session operational resource assignments and cross-center staff assignments.
 */
class SessionOperationalAssignmentMySqlConcurrencyIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Autowired private SessionResourceAssignmentService resourceAssignmentService;
    @Autowired private StaffAssignmentService staffAssignmentService;
    @Autowired private SessionResourceAssignmentRepository resourceAssignmentRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private SessionCapacityPlanRepository capacityPlanRepository;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private TimeSlotRepository timeSlotRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private OperationalStaffRosterRepository staffRosterRepository;
    @Autowired private OperationalStaffIdentityRepository identityRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private CampaignCenterAssignmentRepository campaignCenterAssignmentRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private CampaignLifecycleEventRepository lifecycleEventRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private VaccineType vaccineType;
    private TargetGroup targetGroup;
    private VaccinationCenter centerA;
    private VaccinationCenter centerB;
    private VaccinationCampaign campaign;

    @BeforeEach
    void setUp() throws Exception {
        assertConnectedToRealMySql();

        transactionTemplate = new TransactionTemplate(transactionManager);
        cleanupDatabase();

        transactionTemplate.executeWithoutResult(status -> {
            vaccineType = vaccineTypeRepository.saveAndFlush(
                    new VaccineType("VAX-CONCUR-01", "Concurrency Vaccine", "dose", true)
            );
            targetGroup = targetGroupRepository.saveAndFlush(
                    new TargetGroup("TG-CONCUR-01", "Concurrency Target Group", true)
            );
            centerA = centerRepository.saveAndFlush(
                    new VaccinationCenter("CTR-CONCUR-A", "Concurrency Center A", "100 Ave A", "555-0101", CenterStatus.ACTIVE, 50, 20, "UTC")
            );
            centerB = centerRepository.saveAndFlush(
                    new VaccinationCenter("CTR-CONCUR-B", "Concurrency Center B", "200 Ave B", "555-0102", CenterStatus.ACTIVE, 50, 20, "UTC")
            );

            for (DayOfWeek day : DayOfWeek.values()) {
                operatingHoursRepository.saveAndFlush(new OperatingHours(centerA, day, LocalTime.of(8, 0), LocalTime.of(20, 0), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
                operatingHoursRepository.saveAndFlush(new OperatingHours(centerB, day, LocalTime.of(8, 0), LocalTime.of(20, 0), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
            }

            campaign = campaignRepository.saveAndFlush(
                    CampaignTestFixtures.campaign(
                            "CMP-CONCUR-01",
                            "Concurrency Campaign",
                            "Desc",
                            vaccineType,
                            targetGroup,
                            LocalDate.of(2026, 6, 1),
                            LocalDate.of(2026, 8, 31),
                            Instant.parse("2026-05-01T08:00:00Z"),
                            Instant.parse("2026-08-31T18:00:00Z"),
                            CampaignStatus.ACTIVE
                    )
            );
            campaignCenterAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, centerA, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31)));
            campaignCenterAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, centerB, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31)));
        });
    }

    @AfterEach
    void tearDown() {
        if (transactionTemplate != null) {
            cleanupDatabase();
        }
    }

    private void cleanupDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            timeSlotRepository.deleteAllInBatch();
            resourceAssignmentRepository.deleteAllInBatch();
            staffAssignmentRepository.deleteAllInBatch();
            capacityPlanRepository.deleteAllInBatch();
            sessionChangeEventRepository.deleteAllInBatch();
            sessionRepository.deleteAllInBatch();
            ruleRepository.deleteAllInBatch();
            resourceRepository.deleteAllInBatch();
            staffRosterRepository.deleteAllInBatch();
            identityRepository.deleteAllInBatch();
            campaignCenterAssignmentRepository.deleteAllInBatch();
            closureRepository.deleteAllInBatch();
            operatingHoursRepository.deleteAllInBatch();
            doseProgrammeRepository.deleteAllInBatch();
            lifecycleEventRepository.deleteAllInBatch();
            campaignRepository.deleteAllInBatch();
            centerRepository.deleteAllInBatch();
            targetGroupRepository.deleteAllInBatch();
            vaccineTypeRepository.deleteAllInBatch();
        });
    }

    @Test
    @Timeout(30)
    @DisplayName("Concurrent overlapping resource assignments serialize deterministically on MySQL")
    void concurrentSameResourceOverlappingAssignmentsSerializeOnMySql() throws Exception {
        assertConnectedToRealMySql();

        Long resourceId = transactionTemplate.execute(status -> {
            OperationalResource res = resourceRepository.saveAndFlush(
                    new OperationalResource(centerA, "STATION-CONCUR", ResourceType.VACCINATION_STATION, "Concur Station", OperationalResourceStatus.ACTIVE)
            );
            return res.getId();
        });

        Long session1Id = transactionTemplate.execute(status -> {
            VaccinationSession s1 = sessionRepository.saveAndFlush(
                    new VaccinationSession(campaign, centerA, Instant.parse("2026-07-01T08:00:00Z"), Instant.parse("2026-07-01T16:00:00Z"), Instant.parse("2026-07-01T07:00:00Z"), "UTC")
            );
            return s1.getId();
        });

        Long session2Id = transactionTemplate.execute(status -> {
            VaccinationSession s2 = sessionRepository.saveAndFlush(
                    new VaccinationSession(campaign, centerA, Instant.parse("2026-07-01T08:00:00Z"), Instant.parse("2026-07-01T16:00:00Z"), Instant.parse("2026-07-01T07:00:00Z"), "UTC")
            );
            return s2.getId();
        });

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch competingAttemptLatch = new CountDownLatch(1);

        AtomicBoolean thread1Success = new AtomicBoolean(false);
        AtomicBoolean thread2Success = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Thread 1: Locks resource and creates assignment [08:00, 12:00)
            Future<?> future1 = executor.submit(() -> {
                try {
                    assertTrue(startLatch.await(5, TimeUnit.SECONDS));
                    transactionTemplate.executeWithoutResult(status -> {
                        resourceRepository.findByIdForUpdate(resourceId).orElseThrow();
                        lockAcquiredLatch.countDown();

                        try {
                            assertTrue(competingAttemptLatch.await(5, TimeUnit.SECONDS));
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(ex);
                        }

                        resourceAssignmentService.createAssignment(
                                session1Id,
                                resourceId,
                                Instant.parse("2026-07-01T08:00:00Z"),
                                Instant.parse("2026-07-01T12:00:00Z")
                        );
                    });
                    thread1Success.set(true);
                } catch (Exception e) {
                    thread1Success.set(false);
                }
            });

            // Thread 2: Attempts overlapping assignment [10:00, 14:00)
            Future<?> future2 = executor.submit(() -> {
                try {
                    assertTrue(startLatch.await(5, TimeUnit.SECONDS));
                    assertTrue(lockAcquiredLatch.await(5, TimeUnit.SECONDS));

                    transactionTemplate.executeWithoutResult(status -> {
                        competingAttemptLatch.countDown();
                        resourceAssignmentService.createAssignment(
                                session2Id,
                                resourceId,
                                Instant.parse("2026-07-01T10:00:00Z"),
                                Instant.parse("2026-07-01T14:00:00Z")
                        );
                    });
                    thread2Success.set(true);
                } catch (Exception e) {
                    thread2Success.set(false);
                }
            });

            startLatch.countDown();
            future1.get(10, TimeUnit.SECONDS);
            future2.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertTrue(thread1Success.get(), "First resource assignment transaction must succeed");
        assertFalse(thread2Success.get(), "Competing overlapping resource assignment must fail due to overlap");
        assertEquals(1, resourceAssignmentRepository.count(), "Exactly one resource assignment should be persisted");
    }

    @Test
    @Timeout(30)
    @DisplayName("Concurrent cross-center staff assignments for same user serialize deterministically on MySQL")
    void concurrentSameStaffUserCrossCenterOverlappingAssignmentsSerializeOnMySql() throws Exception {
        assertConnectedToRealMySql();

        Long sharedUserId = 9991L;

        Long rosterAId = transactionTemplate.execute(status -> {
            OperationalStaffRoster rA = staffRosterRepository.saveAndFlush(
                    new OperationalStaffRoster(centerA, sharedUserId, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE)
            );
            return rA.getId();
        });

        Long rosterBId = transactionTemplate.execute(status -> {
            OperationalStaffRoster rB = staffRosterRepository.saveAndFlush(
                    new OperationalStaffRoster(centerB, sharedUserId, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE)
            );
            return rB.getId();
        });

        Long sessionAId = transactionTemplate.execute(status -> {
            VaccinationSession sA = sessionRepository.saveAndFlush(
                    new VaccinationSession(campaign, centerA, Instant.parse("2026-07-01T08:00:00Z"), Instant.parse("2026-07-01T16:00:00Z"), Instant.parse("2026-07-01T07:00:00Z"), "UTC")
            );
            return sA.getId();
        });

        Long sessionBId = transactionTemplate.execute(status -> {
            VaccinationSession sB = sessionRepository.saveAndFlush(
                    new VaccinationSession(campaign, centerB, Instant.parse("2026-07-01T08:00:00Z"), Instant.parse("2026-07-01T16:00:00Z"), Instant.parse("2026-07-01T07:00:00Z"), "UTC")
            );
            return sB.getId();
        });

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch competingAttemptLatch = new CountDownLatch(1);

        AtomicBoolean thread1Success = new AtomicBoolean(false);
        AtomicBoolean thread2Success = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Thread 1: Locks user rosters globally and creates staff assignment at Center A for [08:00, 12:00)
            Future<?> future1 = executor.submit(() -> {
                try {
                    assertTrue(startLatch.await(5, TimeUnit.SECONDS));
                    transactionTemplate.executeWithoutResult(status -> {
                        staffRosterRepository.findByUserIdForUpdateOrderByIdAsc(sharedUserId);
                        lockAcquiredLatch.countDown();

                        try {
                            assertTrue(competingAttemptLatch.await(5, TimeUnit.SECONDS));
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(ex);
                        }

                        staffAssignmentService.createAssignment(
                                sessionAId,
                                rosterAId,
                                StaffRole.VACCINATOR,
                                Instant.parse("2026-07-01T08:00:00Z"),
                                Instant.parse("2026-07-01T12:00:00Z")
                        );
                    });
                    thread1Success.set(true);
                } catch (Exception e) {
                    thread1Success.set(false);
                }
            });

            // Thread 2: Concurrently attempts cross-center staff assignment for same user at Center B for [10:00, 14:00)
            Future<?> future2 = executor.submit(() -> {
                try {
                    assertTrue(startLatch.await(5, TimeUnit.SECONDS));
                    assertTrue(lockAcquiredLatch.await(5, TimeUnit.SECONDS));

                    transactionTemplate.executeWithoutResult(status -> {
                        competingAttemptLatch.countDown();
                        staffAssignmentService.createAssignment(
                                sessionBId,
                                rosterBId,
                                StaffRole.VACCINATOR,
                                Instant.parse("2026-07-01T10:00:00Z"),
                                Instant.parse("2026-07-01T14:00:00Z")
                        );
                    });
                    thread2Success.set(true);
                } catch (Exception e) {
                    thread2Success.set(false);
                }
            });

            startLatch.countDown();
            future1.get(10, TimeUnit.SECONDS);
            future2.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertTrue(thread1Success.get(), "First staff assignment transaction must succeed");
        assertFalse(thread2Success.get(), "Competing cross-center staff assignment for same user must fail due to overlap");
        assertEquals(1, staffAssignmentRepository.count(), "Exactly one staff assignment should be persisted");
    }
}
