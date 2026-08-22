package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.appointment.dependency.SlotReservationResult;
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
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.TimeSlotCapacityService;
import com.se2030.vaccinationportal.campaign.service.TimeSlotService;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private VaccinationSessionRepository sessionRepository;

    @Autowired
    private SessionCapacityPlanRepository capacityPlanRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

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

        timeSlotRepository.deleteAllInBatch();
        capacityPlanRepository.deleteAllInBatch();
        sessionRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
        centerRepository.deleteAllInBatch();
        targetGroupRepository.deleteAllInBatch();
        vaccineTypeRepository.deleteAllInBatch();

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
}
