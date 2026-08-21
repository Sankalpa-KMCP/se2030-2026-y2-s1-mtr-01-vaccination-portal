package com.se2030.vaccinationportal.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
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
import com.se2030.vaccinationportal.campaign.service.OperationalResourceService;
import com.se2030.vaccinationportal.campaign.service.OperationalStaffRosterService;
import com.se2030.vaccinationportal.campaign.service.SessionCapacityPlanService;
import com.se2030.vaccinationportal.campaign.service.SessionResourceAssignmentService;
import com.se2030.vaccinationportal.campaign.service.StaffAssignmentService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Production-equivalent MySQL Testcontainers concurrency tests verifying deterministic serialization
 * and operational readiness enforcement against real MySQL/InnoDB.
 */
class SessionOperationalReadinessMySqlConcurrencyIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Autowired private VaccinationSessionService sessionService;
    @Autowired private SessionCapacityPlanService capacityPlanService;
    @Autowired private SessionResourceAssignmentService resourceAssignmentService;
    @Autowired private StaffAssignmentService staffAssignmentService;
    @Autowired private OperationalResourceService resourceService;
    @Autowired private OperationalStaffRosterService rosterService;

    @Autowired private SessionCapacityPlanRepository capacityPlanRepository;
    @Autowired private SessionResourceAssignmentRepository resourceAssignmentRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private OperationalStaffRosterRepository staffRosterRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private CampaignCenterAssignmentRepository campaignCenterAssignmentRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private VaccinationCenter centerA;
    private VaccinationCenter centerB;
    private VaccinationCampaign campaign;
    private VaccinationSession session;
    private OperationalResource stationResource;
    private OperationalStaffRoster staffRoster;

    private final Instant sessionStart = Instant.parse("2026-10-01T08:00:00Z");
    private final Instant sessionEnd = Instant.parse("2026-10-01T12:00:00Z"); // 240 min

    @BeforeEach
    void setUp() throws Exception {
        assertConnectedToRealMySql();

        transactionTemplate = new TransactionTemplate(transactionManager);
        cleanupDatabase();

        transactionTemplate.executeWithoutResult(status -> {
            VaccineType vaccineType = vaccineTypeRepository.saveAndFlush(
                    new VaccineType("VAX-CONCUR-RDY", "Concurrency Vaccine", "dose", true)
            );
            TargetGroup targetGroup = targetGroupRepository.saveAndFlush(
                    new TargetGroup("TG-CONCUR-RDY", "Concurrency Target Group", true)
            );
            centerA = centerRepository.saveAndFlush(
                    new VaccinationCenter("CTR-CONCUR-RDY-A", "Readiness Center A", "100 Ave A", "555-0101", CenterStatus.ACTIVE, 50, 100, "UTC")
            );
            centerB = centerRepository.saveAndFlush(
                    new VaccinationCenter("CTR-CONCUR-RDY-B", "Readiness Center B", "200 Ave B", "555-0102", CenterStatus.ACTIVE, 50, 100, "UTC")
            );

            for (DayOfWeek day : DayOfWeek.values()) {
                operatingHoursRepository.saveAndFlush(new OperatingHours(centerA, day, LocalTime.of(8, 0), LocalTime.of(20, 0), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
                operatingHoursRepository.saveAndFlush(new OperatingHours(centerB, day, LocalTime.of(8, 0), LocalTime.of(20, 0), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
            }

            campaign = campaignRepository.saveAndFlush(
                    CampaignTestFixtures.campaign(
                            "CMP-CONCUR-RDY",
                            "Readiness Concurrency Campaign",
                            "Desc",
                            vaccineType,
                            targetGroup,
                            LocalDate.of(2026, 6, 1),
                            LocalDate.of(2026, 12, 31),
                            Instant.parse("2026-05-01T08:00:00Z"),
                            Instant.parse("2026-12-31T18:00:00Z"),
                            CampaignStatus.ACTIVE
                    )
            );

            doseProgrammeRepository.saveAndFlush(new CampaignDoseProgramme(
                    campaign, 1, DoseCategory.PRIMARY, "1", 0, false, 10, LocalDate.of(2026, 6, 1), null
            ));

            campaignCenterAssignmentRepository.saveAndFlush(
                    new CampaignCenterAssignment(campaign, centerA, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31))
            );
            campaignCenterAssignmentRepository.saveAndFlush(
                    new CampaignCenterAssignment(campaign, centerB, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31))
            );

            session = sessionRepository.saveAndFlush(
                    new VaccinationSession(campaign, centerA, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC")
            );

            stationResource = resourceRepository.saveAndFlush(
                    new OperationalResource(centerA, "STATION-MYSQL-01", ResourceType.VACCINATION_STATION, "Station 1", OperationalResourceStatus.ACTIVE)
            );

            staffRoster = staffRosterRepository.saveAndFlush(
                    new OperationalStaffRoster(centerA, 8888L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE)
            );

            resourceAssignmentRepository.saveAndFlush(
                    new SessionResourceAssignment(session, stationResource, sessionStart, sessionEnd)
            );

            staffAssignmentRepository.saveAndFlush(
                    new StaffAssignment(session, staffRoster, StaffRole.VACCINATOR, sessionStart, sessionEnd)
            );

            capacityPlanRepository.saveAndFlush(
                    new SessionCapacityPlan(
                            session, 1, 240, 10, 1, 1, 1, 1, 24, 24, 50, 15, 0.0, 100, null, "Planner"
                    )
            );
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
            resourceAssignmentRepository.deleteAllInBatch();
            staffAssignmentRepository.deleteAllInBatch();
            capacityPlanRepository.deleteAllInBatch();
            sessionChangeEventRepository.deleteAllInBatch();
            sessionRepository.deleteAllInBatch();
            ruleRepository.deleteAllInBatch();
            resourceRepository.deleteAllInBatch();
            staffRosterRepository.deleteAllInBatch();
            campaignCenterAssignmentRepository.deleteAllInBatch();
            closureRepository.deleteAllInBatch();
            operatingHoursRepository.deleteAllInBatch();
            doseProgrammeRepository.deleteAllInBatch();
            campaignRepository.deleteAllInBatch();
            centerRepository.deleteAllInBatch();
            targetGroupRepository.deleteAllInBatch();
            vaccineTypeRepository.deleteAllInBatch();
        });
    }

    @Test
    @Timeout(30)
    @DisplayName("Scenario A: Concurrent scheduleSession vs resource status change serializes deterministically")
    void concurrentScheduleVsResourceStatusChange() throws Exception {
        Long sessionId = session.getId();
        Long centerId = centerA.getId();
        Long resourceId = stationResource.getId();

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicBoolean scheduleSucceeded = new AtomicBoolean(false);
        AtomicBoolean resourceUpdateSucceeded = new AtomicBoolean(false);

        Future<?> fSchedule = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                sessionService.scheduleSession(sessionId);
                scheduleSucceeded.set(true);
            } catch (IllegalStateException e) {
                // Expected if resource update committed first
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Future<?> fResource = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                resourceService.updateResource(centerId, resourceId, "Station 1", OperationalResourceStatus.MAINTENANCE);
                resourceUpdateSucceeded.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        fSchedule.get(15, TimeUnit.SECONDS);
        fResource.get(15, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertTrue(resourceUpdateSucceeded.get(), "Resource update should succeed");

        OperationalResource reloadedResource = resourceRepository.findById(resourceId).orElseThrow();
        assertEquals(OperationalResourceStatus.MAINTENANCE, reloadedResource.getStatus());

        VaccinationSession reloadedSession = sessionRepository.findById(sessionId).orElseThrow();
        if (scheduleSucceeded.get()) {
            assertEquals(SessionLifecycleStatus.SCHEDULED, reloadedSession.getLifecycleStatus());
        } else {
            assertEquals(SessionLifecycleStatus.DRAFT, reloadedSession.getLifecycleStatus());
        }
    }

    @Test
    @Timeout(30)
    @DisplayName("Scenario B: Concurrent scheduleSession vs staff roster status change serializes deterministically")
    void concurrentScheduleVsStaffRosterStatusChange() throws Exception {
        Long sessionId = session.getId();
        Long centerId = centerA.getId();
        Long rosterId = staffRoster.getId();

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicBoolean scheduleSucceeded = new AtomicBoolean(false);
        AtomicBoolean rosterUpdateSucceeded = new AtomicBoolean(false);

        Future<?> fSchedule = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                sessionService.scheduleSession(sessionId);
                scheduleSucceeded.set(true);
            } catch (IllegalStateException e) {
                // Expected if roster update committed first
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Future<?> fRoster = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                rosterService.updateStaffRoster(centerId, rosterId, StaffRole.VACCINATOR, OperationalStaffStatus.INACTIVE);
                rosterUpdateSucceeded.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        fSchedule.get(15, TimeUnit.SECONDS);
        fRoster.get(15, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertTrue(rosterUpdateSucceeded.get(), "Roster update should succeed");

        OperationalStaffRoster reloadedRoster = staffRosterRepository.findById(rosterId).orElseThrow();
        assertEquals(OperationalStaffStatus.INACTIVE, reloadedRoster.getStatus());

        VaccinationSession reloadedSession = sessionRepository.findById(sessionId).orElseThrow();
        if (scheduleSucceeded.get()) {
            assertEquals(SessionLifecycleStatus.SCHEDULED, reloadedSession.getLifecycleStatus());
        } else {
            assertEquals(SessionLifecycleStatus.DRAFT, reloadedSession.getLifecycleStatus());
        }
    }

    @Test
    @Timeout(30)
    @DisplayName("Scenario C: Concurrent cross-center roster registration vs staff assignment/scheduling serializes under global user lock")
    void concurrentRosterRegistrationVsAssignmentAndScheduling() throws Exception {
        Long sessionId = session.getId();
        Long centerBId = centerB.getId();
        Long userId = 8888L;

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicBoolean registrationSucceeded = new AtomicBoolean(false);
        AtomicBoolean scheduleSucceeded = new AtomicBoolean(false);

        Future<?> fRegister = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                rosterService.registerStaffMember(centerBId, userId, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE);
                registrationSucceeded.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Future<?> fSchedule = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                sessionService.scheduleSession(sessionId);
                scheduleSucceeded.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        fRegister.get(15, TimeUnit.SECONDS);
        fSchedule.get(15, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertTrue(registrationSucceeded.get(), "Cross-center registration should succeed under deterministic user lock");
        assertTrue(scheduleSucceeded.get(), "Session schedule should succeed");

        assertEquals(2, staffRosterRepository.findByUserIdOrderByCenterIdAsc(userId).size(),
                "User should now have 2 roster entries across center A and center B");

        VaccinationSession reloadedSession = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(SessionLifecycleStatus.SCHEDULED, reloadedSession.getLifecycleStatus());
    }

    @Test
    @Timeout(30)
    @DisplayName("Scenario D: Concurrent duplicate schedule commands for same DRAFT session execute exactly one state transition")
    void concurrentDuplicateScheduleCommands() throws Exception {
        Long sessionId = session.getId();

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Runnable scheduleTask = () -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                sessionService.scheduleSession(sessionId);
                successCount.incrementAndGet();
            } catch (IllegalStateException e) {
                if (e.getMessage().contains("SCHEDULED") || e.getMessage().contains("already")) {
                    conflictCount.incrementAndGet();
                } else {
                    throw new RuntimeException("Unexpected exception: " + e.getMessage(), e);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        Future<?> f1 = executor.submit(scheduleTask);
        Future<?> f2 = executor.submit(scheduleTask);

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        f1.get(15, TimeUnit.SECONDS);
        f2.get(15, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, successCount.get(), "Exactly one schedule command must succeed");
        assertEquals(1, conflictCount.get(), "Competing schedule command must receive 409 Conflict");

        VaccinationSession reloadedSession = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(SessionLifecycleStatus.SCHEDULED, reloadedSession.getLifecycleStatus());
    }
}
