package com.se2030.vaccinationportal.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.List;
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
 * Production-equivalent MySQL Testcontainers concurrency tests verifying deterministic serialization,
 * atomic first-user bootstrap, zero-row registration concurrency, multi-user lock ordering,
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
    @DisplayName("MySQL Test A: Zero-row double registration for brand-new user converges on single identity anchor without race")
    void zeroRowDoubleRegistration_convergesOnSingleIdentityAnchor() throws Exception {
        Long centerAId = centerA.getId();
        Long centerBId = centerB.getId();
        Long newUserId = 9001L;

        // Verify initial state: zero identity rows, zero roster rows for new user
        assertTrue(identityRepository.findByUserId(newUserId).isEmpty(), "Must have zero identity rows initially");
        assertTrue(staffRosterRepository.findByUserIdOrderByCenterIdAsc(newUserId).isEmpty(), "Must have zero roster rows initially");

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicBoolean regASucceeded = new AtomicBoolean(false);
        AtomicBoolean regBSucceeded = new AtomicBoolean(false);

        Future<?> fA = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                rosterService.registerStaffMember(centerAId, newUserId, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE);
                regASucceeded.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Future<?> fB = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                rosterService.registerStaffMember(centerBId, newUserId, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE);
                regBSucceeded.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        fA.get(15, TimeUnit.SECONDS);
        fB.get(15, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertTrue(regASucceeded.get(), "Registration at center A must succeed");
        assertTrue(regBSucceeded.get(), "Registration at center B must succeed");

        // Exactly one identity anchor must exist for the user
        assertEquals(1, identityRepository.findAll().stream().filter(i -> i.getUserId().equals(newUserId)).count(),
                "Must create exactly one identity anchor row for the user");

        // Exactly two roster entries must exist across the two distinct centers
        List<OperationalStaffRoster> rosters = staffRosterRepository.findByUserIdOrderByCenterIdAsc(newUserId);
        assertEquals(2, rosters.size(), "User should have exactly 2 roster entries across center A and center B");
    }

    @Test
    @Timeout(30)
    @DisplayName("MySQL Test B: Concurrent staff assignment mutation vs new cross-center roster registration serializes under identity anchor")
    void concurrentAssignmentMutationVsCrossCenterRosterRegistration() throws Exception {
        Long sessionId = session.getId();
        Long centerBId = centerB.getId();
        Long userId = 8888L;

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicBoolean assignmentDeleteSucceeded = new AtomicBoolean(false);
        AtomicBoolean registerBSucceeded = new AtomicBoolean(false);

        Long assignmentId = staffAssignmentRepository.findBySessionIdOrderByStartAtAscIdAsc(sessionId).get(0).getId();

        Future<?> fDelete = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                staffAssignmentService.deleteAssignment(sessionId, assignmentId);
                assignmentDeleteSucceeded.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Future<?> fRegister = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                rosterService.registerStaffMember(centerBId, userId, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE);
                registerBSucceeded.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        fDelete.get(15, TimeUnit.SECONDS);
        fRegister.get(15, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertTrue(assignmentDeleteSucceeded.get(), "Assignment deletion must succeed under identity lock");
        assertTrue(registerBSucceeded.get(), "Cross-center registration must succeed under identity lock");

        assertEquals(2, staffRosterRepository.findByUserIdOrderByCenterIdAsc(userId).size());
        assertTrue(staffAssignmentRepository.findBySessionIdOrderByStartAtAscIdAsc(sessionId).isEmpty());
    }

    @Test
    @Timeout(30)
    @DisplayName("MySQL Test C: Concurrent scheduleSession vs cross-center roster registration serializes deterministically")
    void concurrentScheduleSessionVsCrossCenterRosterRegistration() throws Exception {
        Long sessionId = session.getId();
        Long centerBId = centerB.getId();
        Long userId = 8888L;

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicBoolean scheduleSucceeded = new AtomicBoolean(false);
        AtomicBoolean registerSucceeded = new AtomicBoolean(false);

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

        Future<?> fRegister = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                rosterService.registerStaffMember(centerBId, userId, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE);
                registerSucceeded.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        fSchedule.get(15, TimeUnit.SECONDS);
        fRegister.get(15, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertTrue(scheduleSucceeded.get(), "Session schedule must succeed");
        assertTrue(registerSucceeded.get(), "Roster registration must succeed");

        VaccinationSession reloadedSession = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(SessionLifecycleStatus.SCHEDULED, reloadedSession.getLifecycleStatus());
        assertEquals(2, staffRosterRepository.findByUserIdOrderByCenterIdAsc(userId).size());
    }

    @Test
    @Timeout(30)
    @DisplayName("MySQL Test D: Reverse multi-user staff ordering acquires identity locks in deterministic ascending order without deadlock")
    void reverseMultiUserStaffOrdering_preventsDeadlock() throws Exception {
        Long u1 = 9101L;
        Long u2 = 9102L;

        // Setup 2 staff members at Center A
        OperationalStaffRoster r1 = transactionTemplate.execute(status ->
                rosterService.registerStaffMember(centerA.getId(), u1, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE)
        );
        OperationalStaffRoster r2 = transactionTemplate.execute(status ->
                rosterService.registerStaffMember(centerA.getId(), u2, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE)
        );

        // Session 1 operating window: 08:00 - 12:00
        Long session1Id = session.getId();
        // Session 2 operating window on day 2: 08:00 - 12:00
        Instant s2Start = sessionStart.plus(java.time.Duration.ofDays(1));
        Instant s2End = sessionEnd.plus(java.time.Duration.ofDays(1));

        // Setup Session 1 with staff in natural order [U1, U2] and valid 2-vaccinator capacity plan
        transactionTemplate.executeWithoutResult(status -> {
            staffAssignmentRepository.deleteAllInBatch();
            capacityPlanRepository.deleteAllInBatch();

            staffAssignmentRepository.saveAndFlush(
                    new StaffAssignment(session, r1, StaffRole.VACCINATOR, sessionStart, sessionEnd)
            );
            staffAssignmentRepository.saveAndFlush(
                    new StaffAssignment(session, r2, StaffRole.VACCINATOR, sessionStart, sessionEnd)
            );
            capacityPlanRepository.saveAndFlush(
                    new SessionCapacityPlan(
                            session, 1, 240, 10, 2, 1, 1, 1, 24, 24, 50, 15, 0.0, 100, null, "Planner"
                    )
            );
        });

        // Setup Session 2 with staff in reverse order [U2, U1] and valid 2-vaccinator capacity plan
        Long session2Id = transactionTemplate.execute(status -> {
            VaccinationSession s2 = sessionRepository.saveAndFlush(
                    new VaccinationSession(campaign, centerA, s2Start, s2End, s2Start.minusSeconds(3600), "UTC")
            );
            resourceAssignmentRepository.saveAndFlush(
                    new SessionResourceAssignment(s2, stationResource, s2Start, s2End)
            );
            capacityPlanRepository.saveAndFlush(
                    new SessionCapacityPlan(
                            s2, 1, 240, 10, 2, 1, 1, 1, 24, 24, 50, 15, 0.0, 100, null, "Planner"
                    )
            );
            staffAssignmentRepository.saveAndFlush(
                    new StaffAssignment(s2, r2, StaffRole.VACCINATOR, s2Start, s2End)
            );
            staffAssignmentRepository.saveAndFlush(
                    new StaffAssignment(s2, r1, StaffRole.VACCINATOR, s2Start, s2End)
            );
            return s2.getId();
        });

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicBoolean s1Scheduled = new AtomicBoolean(false);
        AtomicBoolean s2Scheduled = new AtomicBoolean(false);

        Future<?> f1 = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                sessionService.scheduleSession(session1Id);
                s1Scheduled.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Future<?> f2 = executor.submit(() -> {
            readyLatch.countDown();
            try {
                assertTrue(startLatch.await(10, TimeUnit.SECONDS));
                sessionService.scheduleSession(session2Id);
                s2Scheduled.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        f1.get(15, TimeUnit.SECONDS);
        f2.get(15, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertTrue(s1Scheduled.get(), "Session 1 must schedule successfully");
        assertTrue(s2Scheduled.get(), "Session 2 must schedule successfully");

        assertEquals(SessionLifecycleStatus.SCHEDULED, sessionRepository.findById(session1Id).orElseThrow().getLifecycleStatus());
        assertEquals(SessionLifecycleStatus.SCHEDULED, sessionRepository.findById(session2Id).orElseThrow().getLifecycleStatus());
    }

    @Test
    @Timeout(30)
    @DisplayName("Scenario E: Concurrent scheduling cannot race assigned resource deactivation")
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
                resourceService.updateResource(centerId, resourceId, "Station 1", OperationalResourceStatus.INACTIVE);
                resourceUpdateSucceeded.set(true);
            } catch (IllegalStateException e) {
                // Expected: the DRAFT or SCHEDULED session remains a blocking dependency.
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

        assertTrue(scheduleSucceeded.get(), "Scheduling should remain valid with the assigned ACTIVE resource");
        assertFalse(resourceUpdateSucceeded.get(), "Assigned resource deactivation must be rejected");

        OperationalResource reloadedResource = resourceRepository.findById(resourceId).orElseThrow();
        assertEquals(OperationalResourceStatus.ACTIVE, reloadedResource.getStatus());

        VaccinationSession reloadedSession = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(SessionLifecycleStatus.SCHEDULED, reloadedSession.getLifecycleStatus());
    }

    @Test
    @Timeout(30)
    @DisplayName("Scenario F: Concurrent scheduling cannot race assigned staff deactivation")
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
            } catch (IllegalStateException e) {
                // Expected: the DRAFT or SCHEDULED session remains a blocking dependency.
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

        assertTrue(scheduleSucceeded.get(), "Scheduling should remain valid with the assigned ACTIVE staff member");
        assertFalse(rosterUpdateSucceeded.get(), "Assigned staff deactivation must be rejected");

        OperationalStaffRoster reloadedRoster = staffRosterRepository.findById(rosterId).orElseThrow();
        assertEquals(OperationalStaffStatus.ACTIVE, reloadedRoster.getStatus());

        VaccinationSession reloadedSession = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(SessionLifecycleStatus.SCHEDULED, reloadedSession.getLifecycleStatus());
    }

    @Test
    @Timeout(30)
    @DisplayName("Scenario G: Concurrent duplicate schedule commands for same DRAFT session execute exactly one state transition")
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
