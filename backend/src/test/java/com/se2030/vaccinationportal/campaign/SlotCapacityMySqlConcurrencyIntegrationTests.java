package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.api.capacity.SlotReservationResult;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionBookingStatus;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.TimeSlot;
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
import com.se2030.vaccinationportal.campaign.service.OccupancyEvaluationResult;
import com.se2030.vaccinationportal.campaign.service.OccupancyInterval;
import com.se2030.vaccinationportal.campaign.service.SessionCapacityCalculationService;
import com.se2030.vaccinationportal.campaign.service.TimeSlotCapacityService;
import com.se2030.vaccinationportal.campaign.service.TimeSlotService;
import com.se2030.vaccinationportal.campaign.service.VaccinationCenterService;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Production-equivalent MySQL Testcontainers concurrency tests verifying deterministic pessimistic locking,
 * atomic reservation counters, capacity floor preservation, and overbooking prevention on real MySQL/InnoDB.
 */
class SlotCapacityMySqlConcurrencyIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Autowired
    private TimeSlotCapacityService slotCapacityService;

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private SessionCapacityCalculationService calculationService;

    @Autowired
    private VaccinationCenterService centerService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private VaccinationSessionRepository sessionRepository;

    @Autowired
    private SessionCapacityPlanRepository capacityPlanRepository;

    @Autowired
    private SessionResourceAssignmentRepository resourceAssignmentRepository;

    @Autowired
    private StaffAssignmentRepository staffAssignmentRepository;

    @Autowired
    private SessionChangeEventRepository sessionChangeEventRepository;

    @Autowired
    private OperationalResourceRepository resourceRepository;

    @Autowired
    private OperationalStaffRosterRepository staffRosterRepository;

    @Autowired
    private OperationalStaffIdentityRepository identityRepository;

    @Autowired
    private SessionRecurrenceRuleRepository ruleRepository;

    @Autowired
    private CampaignCenterAssignmentRepository campaignCenterAssignmentRepository;

    @Autowired
    private CenterClosureRepository closureRepository;

    @Autowired
    private OperatingHoursRepository operatingHoursRepository;

    @Autowired
    private CampaignDoseProgrammeRepository doseProgrammeRepository;

    @Autowired
    private CampaignLifecycleEventRepository lifecycleEventRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private VaccinationSession session;
    private Instant baseStart;
    private Instant baseEnd;

    @BeforeEach
    void setUp() throws Exception {
        assertConnectedToRealMySql();
        transactionTemplate = new TransactionTemplate(transactionManager);

        cleanupDatabase();

        VaccineType vax = vaccineTypeRepository.save(new VaccineType("VAX-CONC-01", "Concurrency Vaccine"));
        TargetGroup tg = targetGroupRepository.save(new TargetGroup("TG-CONC-01", "Concurrency Target Group"));

        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-CONC-01", "M4 Concurrency Campaign", "Testing M4 Concurrency",
                vax, tg,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                CampaignStatus.ACTIVE
        ));

        center = centerRepository.save(new VaccinationCenter(
                "CTR-CONC-01", "M4 Concurrency Center", "100 Health Way", "+12345",
                CenterStatus.ACTIVE, 60, 100, "UTC"
        ));

        baseStart = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        baseEnd = baseStart.plus(8, ChronoUnit.HOURS);

        session = new VaccinationSession(
                campaign,
                center,
                baseStart,
                baseEnd,
                baseStart.minus(2, ChronoUnit.HOURS),
                "UTC"
        );
        session.schedule();
        session = sessionRepository.save(session);
    }

    @Test
    @Timeout(30)
    @DisplayName("Task 27: Last-capacity race - Exactly 1 of 2 concurrent attempts succeeds on capacity=1 slot")
    void lastCapacityRace_exactlyOneWins_andOneFull() throws Exception {
        TimeSlot slot = new TimeSlot(session, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 1);
        slot = timeSlotRepository.save(slot);
        Long slotId = slot.getId();

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<SlotReservationResult>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await(10, TimeUnit.SECONDS);
                return slotCapacityService.reserveCapacity(slotId);
            }));
        }

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        int successCount = 0;
        int fullCount = 0;
        for (Future<SlotReservationResult> f : futures) {
            SlotReservationResult result = f.get(10, TimeUnit.SECONDS);
            if (result == SlotReservationResult.SUCCESS) {
                successCount++;
            } else if (result == SlotReservationResult.SLOT_FULL) {
                fullCount++;
            }
        }
        executor.shutdown();

        assertEquals(1, successCount, "Exactly one thread must successfully reserve the final capacity place");
        assertEquals(1, fullCount, "Exactly one thread must receive SLOT_FULL");

        TimeSlot updated = timeSlotRepository.findById(slotId).orElseThrow();
        assertEquals(1, updated.getReservedCount());
        assertEquals(0, updated.getAvailableCapacity());
    }

    @Test
    @Timeout(30)
    @DisplayName("Task 28: Capacity N race - Exactly N of N+K concurrent attempts succeed without overbooking")
    void capacityNRace_boundedReservations_neverExceedsCapacity() throws Exception {
        int capacity = 3;
        int threads = 7; // 3 success, 4 rejected
        TimeSlot slot = new TimeSlot(session, baseStart, baseStart.plus(1, ChronoUnit.HOURS), capacity);
        slot = timeSlotRepository.save(slot);
        Long slotId = slot.getId();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<SlotReservationResult>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await(10, TimeUnit.SECONDS);
                return slotCapacityService.reserveCapacity(slotId);
            }));
        }

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        int successCount = 0;
        int fullCount = 0;
        for (Future<SlotReservationResult> f : futures) {
            SlotReservationResult result = f.get(10, TimeUnit.SECONDS);
            if (result == SlotReservationResult.SUCCESS) {
                successCount++;
            } else if (result == SlotReservationResult.SLOT_FULL) {
                fullCount++;
            }
        }
        executor.shutdown();

        assertEquals(capacity, successCount, "Exactly N reservations must succeed");
        assertEquals(threads - capacity, fullCount, "Remaining K reservation attempts must be rejected");

        TimeSlot updated = timeSlotRepository.findById(slotId).orElseThrow();
        assertEquals(capacity, updated.getReservedCount());
        assertEquals(0, updated.getAvailableCapacity());
    }

    @Test
    @Timeout(30)
    @DisplayName("Task 29: Concurrent Reserve vs Release maintains non-negative and capacity bounds")
    void reserveVsRelease_maintainsNonNegativeAndCapacityInvariants() throws Exception {
        int capacity = 5;
        TimeSlot slot = new TimeSlot(session, baseStart, baseStart.plus(1, ChronoUnit.HOURS), capacity);
        slot.setReservedCount(2); // Initial 2 reservations
        slot = timeSlotRepository.save(slot);
        Long slotId = slot.getId();

        int numReservers = 4;
        int numReleasers = 4;
        int totalThreads = numReservers + numReleasers;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successfulReserves = new AtomicInteger(0);
        AtomicInteger successfulReleases = new AtomicInteger(0);

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < numReservers; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await(10, TimeUnit.SECONDS);
                SlotReservationResult res = slotCapacityService.reserveCapacity(slotId);
                if (res == SlotReservationResult.SUCCESS) {
                    successfulReserves.incrementAndGet();
                }
                return null;
            }));
        }

        for (int i = 0; i < numReleasers; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await(10, TimeUnit.SECONDS);
                boolean rel = slotCapacityService.releaseCapacity(slotId);
                if (rel) {
                    successfulReleases.incrementAndGet();
                }
                return null;
            }));
        }

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        for (Future<Void> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        TimeSlot updated = timeSlotRepository.findById(slotId).orElseThrow();
        int expectedReserved = 2 + successfulReserves.get() - successfulReleases.get();
        assertEquals(expectedReserved, updated.getReservedCount());
        assertTrue(updated.getReservedCount() >= 0, "Reserved count cannot be negative");
        assertTrue(updated.getReservedCount() <= capacity, "Reserved count cannot exceed capacity");
    }

    @Test
    @Timeout(30)
    @DisplayName("Task 30: Capacity reduction vs reservation preserves booking floor")
    void capacityReductionVsReservation_enforcesBookingFloor() throws Exception {
        SessionCapacityPlan plan = new SessionCapacityPlan(
                session, 1, 240, 10, 2, 2, 2, 2, 48, 50, 60, 15, 0.0, null, "Approved override", "admin"
        );
        capacityPlanRepository.save(plan);

        TimeSlot slot = timeSlotService.createSlot(session.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
        slot.setReservedCount(2);
        slot = timeSlotRepository.save(slot);
        Long slotId = slot.getId();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch readyLatch = new CountDownLatch(4);
        CountDownLatch startLatch = new CountDownLatch(1);

        // Thread 1: tries to reduce capacity to 3
        Future<Boolean> reductionFuture = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await(10, TimeUnit.SECONDS);
            try {
                timeSlotService.updateSlotCapacity(slotId, 3);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        // Threads 2, 3, 4: try to reserve capacity
        List<Future<SlotReservationResult>> reserveFutures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            reserveFutures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await(10, TimeUnit.SECONDS);
                return slotCapacityService.reserveCapacity(slotId);
            }));
        }

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        reductionFuture.get(10, TimeUnit.SECONDS);
        for (Future<SlotReservationResult> rf : reserveFutures) {
            rf.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        TimeSlot updated = timeSlotRepository.findById(slotId).orElseThrow();
        assertTrue(updated.getCapacity() >= updated.getReservedCount(),
                "Slot capacity (" + updated.getCapacity() + ") must be >= reserved count (" + updated.getReservedCount() + ")");
    }

    @Test
    @Timeout(30)
    @DisplayName("Task 31: Cross-slot concurrency independence - Simultaneous reservations on disjoint slots")
    void crossSlotIndependence_runsConcurrentlyWithoutContention() throws Exception {
        TimeSlot slotA = timeSlotRepository.save(new TimeSlot(session, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 5));
        TimeSlot slotB = timeSlotRepository.save(new TimeSlot(session, baseStart.plus(1, ChronoUnit.HOURS), baseStart.plus(2, ChronoUnit.HOURS), 5));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<SlotReservationResult> resA = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await(10, TimeUnit.SECONDS);
            return slotCapacityService.reserveCapacity(slotA.getId());
        });

        Future<SlotReservationResult> resB = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await(10, TimeUnit.SECONDS);
            return slotCapacityService.reserveCapacity(slotB.getId());
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        assertEquals(SlotReservationResult.SUCCESS, resA.get(10, TimeUnit.SECONDS));
        assertEquals(SlotReservationResult.SUCCESS, resB.get(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, timeSlotRepository.findById(slotA.getId()).orElseThrow().getReservedCount());
        assertEquals(1, timeSlotRepository.findById(slotB.getId()).orElseThrow().getReservedCount());
    }

    @Test
    @Timeout(30)
    @DisplayName("Task 32: Concurrent two-slot capacity update - Serialized by parent session lock in MySQL")
    void concurrentTwoSlotUpdate_enforcesApprovedCapacityAggregateInMySQL() throws Exception {
        SessionCapacityPlan plan = new SessionCapacityPlan(
                session, 1, 240, 10, 2, 2, 2, 2, 48, 10, 60, 15, 0.0, null, null, "admin"
        );
        capacityPlanRepository.save(plan);

        TimeSlot slotA = timeSlotService.createSlot(session.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 4);
        TimeSlot slotB = timeSlotService.createSlot(session.getId(), baseStart.plus(1, ChronoUnit.HOURS), baseStart.plus(2, ChronoUnit.HOURS), 4);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        // T1: update slotA from 4 to 6 (requires 6 + 4 = 10)
        Future<Boolean> f1 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await(10, TimeUnit.SECONDS);
            try {
                timeSlotService.updateSlotCapacity(slotA.getId(), 6);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        // T2: update slotB from 4 to 6 (requires 4 + 6 = 10; combined would be 12 > 10)
        Future<Boolean> f2 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await(10, TimeUnit.SECONDS);
            try {
                timeSlotService.updateSlotCapacity(slotB.getId(), 6);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        boolean r1 = f1.get(10, TimeUnit.SECONDS);
        boolean r2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one should succeed and one should fail
        assertTrue((r1 && !r2) || (!r1 && r2), "Exactly one conflicting slot capacity update should succeed");

        Integer total = timeSlotRepository.sumCapacityBySessionId(session.getId());
        assertEquals(10, total, "Total configured slot capacity must not exceed approved capacity (10)");
    }

    @Test
    @Timeout(30)
    @DisplayName("Task 33: Concurrent create vs update slot capacity - Serialized by parent session lock in MySQL")
    void concurrentCreateVsUpdate_enforcesApprovedCapacityAggregateInMySQL() throws Exception {
        SessionCapacityPlan plan = new SessionCapacityPlan(
                session, 1, 240, 10, 2, 2, 2, 2, 48, 10, 60, 15, 0.0, null, null, "admin"
        );
        capacityPlanRepository.save(plan);

        TimeSlot slotA = timeSlotService.createSlot(session.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 4);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        // T1: update slotA from 4 to 7
        Future<Boolean> f1 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await(10, TimeUnit.SECONDS);
            try {
                timeSlotService.updateSlotCapacity(slotA.getId(), 7);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        // T2: create slotB with capacity 5 (combined would be 7 + 5 = 12 > 10)
        Future<Boolean> f2 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await(10, TimeUnit.SECONDS);
            try {
                timeSlotService.createSlot(session.getId(), baseStart.plus(1, ChronoUnit.HOURS), baseStart.plus(2, ChronoUnit.HOURS), 5);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        boolean r1 = f1.get(10, TimeUnit.SECONDS);
        boolean r2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Total capacity must remain <= 10
        Integer total = timeSlotRepository.sumCapacityBySessionId(session.getId());
        assertTrue(total <= 10, "Total configured slot capacity (" + total + ") must not exceed approved capacity (10)");
    }

    @Test
    @Timeout(30)
    @DisplayName("Task 34: Concurrent overlapping slot creation - Serialized by parent session lock preventing concurrent occupancy breach")
    void concurrentOverlappingSlotCreation_enforcesConcurrentOccupancyUnderSessionLock() throws Exception {
        // safeConcurrentOccupancy = 10, approvedCapacity = 48 (matching recommended throughput, no override required)
        SessionCapacityPlan plan = new SessionCapacityPlan(
                session, 1, 240, 10, 2, 2, 2, 2, 48, 48, 10, 15, 0.0, null, null, "admin"
        );
        capacityPlanRepository.save(plan);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        // T1: create slot 1 from baseStart to baseStart + 60m with capacity 8 (8 <= 10)
        Future<Boolean> f1 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await(10, TimeUnit.SECONDS);
            try {
                timeSlotService.createSlot(session.getId(), baseStart, baseStart.plus(60, ChronoUnit.MINUTES), 8);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        // T2: create slot 2 from baseStart + 30m to baseStart + 90m with capacity 8 (8 <= 10, but overlap would be 16 > 10)
        Future<Boolean> f2 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await(10, TimeUnit.SECONDS);
            try {
                timeSlotService.createSlot(session.getId(), baseStart.plus(30, ChronoUnit.MINUTES), baseStart.plus(90, ChronoUnit.MINUTES), 8);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        boolean r1 = f1.get(10, TimeUnit.SECONDS);
        boolean r2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one should succeed and one should fail due to concurrent occupancy serialization
        assertTrue((r1 && !r2) || (!r1 && r2), "Exactly one overlapping slot creation should succeed without exceeding safe concurrent occupancy");

        List<TimeSlot> slots = timeSlotRepository.findBySessionIdOrderByStartTimeAsc(session.getId());
        assertEquals(1, slots.size(), "Only one slot must be persisted");
        assertEquals(8, slots.get(0).getCapacity());
    }

    @Test
    @Timeout(30)
    @DisplayName("Center safety: slot create then lowering serializes and rejects unsafe lowering")
    void centerSafety_slotCreateCommitsBeforeLowering_rejectsLowering() throws Exception {
        updateCenterSafeLimit(10);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch slotCreatedInTx = new CountDownLatch(1);
        CountDownLatch releaseSlotTx = new CountDownLatch(1);

        try {
            Future<?> slotFuture = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                timeSlotService.createSlot(session.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 8);
                slotCreatedInTx.countDown();
                awaitLatch(releaseSlotTx);
            }));

            assertTrue(slotCreatedInTx.await(10, TimeUnit.SECONDS));

            Future<Throwable> loweringFuture = executor.submit(() -> {
                try {
                    updateCenterSafeLimit(5);
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            });

            // Deterministically prove that the lowering transaction is blocked in MySQL on the center row lock
            awaitCenterLockWait(10000);

            // Now release the slot creation transaction to commit
            releaseSlotTx.countDown();
            slotFuture.get(10, TimeUnit.SECONDS);

            // Lowering should unblock, execute its validation against the committed slot (peak 8.0), and reject lowering to 5
            Throwable loweringError = loweringFuture.get(10, TimeUnit.SECONDS);
            assertNotNull(loweringError, "Expected center lowering to be rejected");
            assertTrue(loweringError instanceof IllegalArgumentException, "Expected IllegalArgumentException, got: " + loweringError);
            assertTrue(loweringError.getMessage().contains("cannot be lowered to 5"), "Unexpected error: " + loweringError.getMessage());
            assertTrue(loweringError.getMessage().contains("configured peak occupancy 8.0"), "Unexpected error: " + loweringError.getMessage());
        } finally {
            releaseSlotTx.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(10, centerRepository.findById(center.getId()).orElseThrow().getSafeConcurrentOccupancy());
        List<TimeSlot> slots = timeSlotRepository.findBySessionIdOrderByStartTimeAsc(session.getId());
        assertEquals(1, slots.size(), "Target created slot must be persisted");
        assertEquals(8, slots.get(0).getCapacity(), "Target slot capacity must be 8");

        SessionCapacityPlan plan = capacityPlanRepository.findTopBySessionIdOrderByRevisionDesc(session.getId()).orElse(null);
        int expectedDwell = plan != null ? plan.getExpectedDwellMinutes() : SessionCapacityPlan.DEFAULT_EXPECTED_DWELL_MINUTES;
        double accompanyingRatio = plan != null ? plan.getExpectedAccompanyingPersonsPerBooking() : SessionCapacityPlan.DEFAULT_ACCOMPANYING_RATIO;
        List<OccupancyInterval> intervals = calculationService.mapSlotsToOccupancyIntervals(slots, expectedDwell, accompanyingRatio);
        OccupancyEvaluationResult occupancyResult = calculationService.evaluateConcurrentOccupancy(intervals, 10);
        assertEquals(8.0, occupancyResult.peakOccupancyUnits(), 0.001, "Resulting configured peak occupancy must be 8.0");
        assertTrue(occupancyResult.peakOccupancyUnits() <= 10, "Peak occupancy must remain <= center safeConcurrentOccupancy");
    }

    @Test
    @Timeout(30)
    @DisplayName("Center safety: lowering then slot create serializes and rejects unsafe slot")
    void centerSafety_loweringCommitsBeforeSlotCreate_rejectsSlotCreate() throws Exception {
        updateCenterSafeLimit(10);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch centerLoweredInTx = new CountDownLatch(1);
        CountDownLatch releaseLoweringTx = new CountDownLatch(1);

        try {
            Future<?> loweringFuture = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                updateCenterSafeLimit(5);
                centerLoweredInTx.countDown();
                awaitLatch(releaseLoweringTx);
            }));

            assertTrue(centerLoweredInTx.await(10, TimeUnit.SECONDS));

            Future<Throwable> slotFuture = executor.submit(() -> {
                try {
                    timeSlotService.createSlot(session.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 8);
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            });

            // Deterministically prove that the slot creation transaction is blocked in MySQL on the center row lock
            awaitCenterLockWait(10000);

            // Now release the lowering transaction to commit
            releaseLoweringTx.countDown();
            loweringFuture.get(10, TimeUnit.SECONDS);

            // Slot creation should unblock, read the committed center limit (5), and reject peak 8.0 > 5
            Throwable slotError = slotFuture.get(10, TimeUnit.SECONDS);
            assertNotNull(slotError, "Expected slot creation to be rejected");
            assertTrue(slotError instanceof IllegalStateException, "Expected IllegalStateException, got: " + slotError);
            assertTrue(slotError.getMessage().contains("exceeds safe concurrent occupancy limit (5)"), "Unexpected error: " + slotError.getMessage());
        } finally {
            releaseLoweringTx.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(5, centerRepository.findById(center.getId()).orElseThrow().getSafeConcurrentOccupancy());
        assertTrue(timeSlotRepository.findBySessionIdOrderByStartTimeAsc(session.getId()).isEmpty());
    }

    @Test
    @Timeout(30)
    @DisplayName("Center safety: slot capacity update serializes before unsafe lowering")
    void centerSafety_slotUpdateCommitsBeforeLowering_rejectsLowering() throws Exception {
        updateCenterSafeLimit(10);
        TimeSlot slot = timeSlotService.createSlot(
                session.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 4);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch slotUpdatedInTx = new CountDownLatch(1);
        CountDownLatch releaseUpdateTx = new CountDownLatch(1);

        try {
            Future<?> updateFuture = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                timeSlotService.updateSlotCapacity(slot.getId(), 8);
                slotUpdatedInTx.countDown();
                awaitLatch(releaseUpdateTx);
            }));

            assertTrue(slotUpdatedInTx.await(10, TimeUnit.SECONDS));

            Future<Throwable> loweringFuture = executor.submit(() -> {
                try {
                    updateCenterSafeLimit(5);
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            });

            // Deterministically prove that the lowering transaction is blocked in MySQL on the center row lock
            awaitCenterLockWait(10000);

            // Now release the slot update transaction to commit
            releaseUpdateTx.countDown();
            updateFuture.get(10, TimeUnit.SECONDS);

            // Lowering should unblock, check the committed updated slot capacity (peak 8.0), and reject lowering to 5
            Throwable loweringError = loweringFuture.get(10, TimeUnit.SECONDS);
            assertNotNull(loweringError, "Expected center lowering to be rejected");
            assertTrue(loweringError instanceof IllegalArgumentException, "Expected IllegalArgumentException, got: " + loweringError);
            assertTrue(loweringError.getMessage().contains("cannot be lowered to 5"), "Unexpected error: " + loweringError.getMessage());
            assertTrue(loweringError.getMessage().contains("configured peak occupancy 8.0"), "Unexpected error: " + loweringError.getMessage());
        } finally {
            releaseUpdateTx.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(10, centerRepository.findById(center.getId()).orElseThrow().getSafeConcurrentOccupancy());
        assertEquals(8, timeSlotRepository.findById(slot.getId()).orElseThrow().getCapacity());
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

    private void updateCenterSafeLimit(int safeConcurrentOccupancy) {
        center = centerService.updateCenter(
                center.getId(),
                center.getName(),
                center.getAddress(),
                center.getContact(),
                safeConcurrentOccupancy,
                center.getSessionServiceCeiling()
        );
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while coordinating center-safety concurrency test", error);
        }
    }

    private void awaitCenterLockWait(long timeoutMillis) {
        Instant deadline = Instant.now().plusMillis(timeoutMillis);
        String sql = """
                SELECT count(*)
                FROM performance_schema.data_lock_waits w
                JOIN performance_schema.data_locks req ON req.ENGINE_LOCK_ID = w.REQUESTING_ENGINE_LOCK_ID
                WHERE req.OBJECT_NAME = 'vaccination_centers'
                  AND req.LOCK_STATUS = 'WAITING'
                """;
        while (Instant.now().isBefore(deadline)) {
            try (Connection conn = createObserverConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to query MySQL performance_schema for lock waits", e);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while polling for center lock wait", e);
            }
        }
        throw new AssertionError("Contender did not reach database lock wait on vaccination_centers table within " + timeoutMillis + "ms");
    }
}
