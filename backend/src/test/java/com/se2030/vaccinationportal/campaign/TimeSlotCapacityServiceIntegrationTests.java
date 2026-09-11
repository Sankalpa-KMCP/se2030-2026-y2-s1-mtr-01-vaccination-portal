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
import com.se2030.vaccinationportal.campaign.service.VaccinationCenterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.service.SessionCapacityPlanService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TimeSlotCapacityServiceIntegrationTests {

    @Autowired
    private TimeSlotCapacityService slotCapacityService;

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private SessionCapacityPlanService planService;

    @Autowired
    private VaccinationCenterService centerService;

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

    private VaccinationCampaign testCampaign;
    private VaccinationCenter testCenter;
    private VaccinationSession scheduledSession;
    private VaccinationSession draftSession;
    private Instant baseStart;
    private Instant baseEnd;

    @BeforeEach
    void setUp() {
        cleanDb();
        VaccineType vax = vaccineTypeRepository.save(new VaccineType("VAX-TS-01", "Test Vaccine"));
        TargetGroup tg = targetGroupRepository.save(new TargetGroup("TG-TS-01", "Test Target Group"));

        testCampaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-TS-01", "TimeSlot Campaign", "Testing TimeSlots",
                vax, tg,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                CampaignStatus.ACTIVE
        ));

        testCenter = centerRepository.save(new VaccinationCenter(
                "CTR-TS-01", "TimeSlot Center", "100 Health Way", "+12345",
                CenterStatus.ACTIVE, 60, 100, "UTC"
        ));

        baseStart = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        baseEnd = baseStart.plus(8, ChronoUnit.HOURS);

        scheduledSession = new VaccinationSession(
                testCampaign,
                testCenter,
                baseStart,
                baseEnd,
                baseStart.minus(2, ChronoUnit.HOURS),
                "UTC"
        );
        scheduledSession.schedule();
        scheduledSession = sessionRepository.save(scheduledSession);

        draftSession = new VaccinationSession(
                testCampaign,
                testCenter,
                baseStart,
                baseEnd,
                baseStart.minus(2, ChronoUnit.HOURS),
                "UTC"
        );
        draftSession = sessionRepository.save(draftSession);
    }

    @AfterEach
    void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        timeSlotRepository.deleteAllInBatch();
        capacityPlanRepository.deleteAllInBatch();
        sessionRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
        centerRepository.deleteAllInBatch();
        targetGroupRepository.deleteAllInBatch();
        vaccineTypeRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("hasAvailableCapacity returns true when reserved count is below capacity")
    void hasAvailableCapacity_trueWhenBelowCapacity() {
        TimeSlot slot = new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
        slot.setReservedCount(5);
        slot = timeSlotRepository.save(slot);

        assertTrue(slotCapacityService.hasAvailableCapacity(slot.getId()));
    }

    @Test
    @DisplayName("hasAvailableCapacity returns false when slot is fully booked")
    void hasAvailableCapacity_falseWhenFull() {
        TimeSlot slot = new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 5);
        slot.setReservedCount(5);
        slot = timeSlotRepository.save(slot);

        assertFalse(slotCapacityService.hasAvailableCapacity(slot.getId()));
    }

    @Test
    @DisplayName("hasAvailableCapacity returns false when slot ID is invalid or not found")
    void hasAvailableCapacity_falseWhenNotFound() {
        assertFalse(slotCapacityService.hasAvailableCapacity(null));
        assertFalse(slotCapacityService.hasAvailableCapacity(999999L));
    }

    @Test
    @DisplayName("hasAvailableCapacity returns false when parent session is not SCHEDULED")
    void hasAvailableCapacity_falseWhenSessionNotScheduled() {
        TimeSlot slot = new TimeSlot(draftSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
        slot = timeSlotRepository.save(slot);

        assertFalse(slotCapacityService.hasAvailableCapacity(slot.getId()));
    }

    @Test
    @DisplayName("reserveCapacity succeeds and increments reserved count when capacity remains")
    void reserveCapacity_success() {
        TimeSlot slot = new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 3);
        slot = timeSlotRepository.save(slot);

        SlotReservationResult result = slotCapacityService.reserveCapacity(slot.getId());
        assertEquals(SlotReservationResult.SUCCESS, result);

        TimeSlot updated = timeSlotRepository.findById(slot.getId()).orElseThrow();
        assertEquals(1, updated.getReservedCount());
        assertEquals(2, updated.getAvailableCapacity());
    }

    @Test
    @DisplayName("reserveCapacity returns SLOT_FULL when slot capacity is exhausted")
    void reserveCapacity_slotFull() {
        TimeSlot slot = new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 1);
        slot.setReservedCount(1);
        slot = timeSlotRepository.save(slot);

        SlotReservationResult result = slotCapacityService.reserveCapacity(slot.getId());
        assertEquals(SlotReservationResult.SLOT_FULL, result);

        TimeSlot updated = timeSlotRepository.findById(slot.getId()).orElseThrow();
        assertEquals(1, updated.getReservedCount());
    }

    @Test
    @DisplayName("reserveCapacity returns SLOT_NOT_FOUND when slot does not exist")
    void reserveCapacity_slotNotFound() {
        assertEquals(SlotReservationResult.SLOT_NOT_FOUND, slotCapacityService.reserveCapacity(null));
        assertEquals(SlotReservationResult.SLOT_NOT_FOUND, slotCapacityService.reserveCapacity(999999L));
    }

    @Test
    @DisplayName("reserveCapacity returns SLOT_FULL when parent session is not SCHEDULED")
    void reserveCapacity_sessionNotScheduled() {
        TimeSlot slot = new TimeSlot(draftSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 5);
        slot = timeSlotRepository.save(slot);

        SlotReservationResult result = slotCapacityService.reserveCapacity(slot.getId());
        assertEquals(SlotReservationResult.SLOT_FULL, result);
    }

    @Test
    @DisplayName("releaseCapacity decrements reserved count and returns true")
    void releaseCapacity_success() {
        TimeSlot slot = new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 5);
        slot.setReservedCount(3);
        slot = timeSlotRepository.save(slot);

        boolean released = slotCapacityService.releaseCapacity(slot.getId());
        assertTrue(released);

        TimeSlot updated = timeSlotRepository.findById(slot.getId()).orElseThrow();
        assertEquals(2, updated.getReservedCount());
    }

    @Test
    @DisplayName("releaseCapacity returns false when reserved count is already zero")
    void releaseCapacity_falseWhenZero() {
        TimeSlot slot = new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 5);
        slot.setReservedCount(0);
        slot = timeSlotRepository.save(slot);

        boolean released = slotCapacityService.releaseCapacity(slot.getId());
        assertFalse(released);

        TimeSlot updated = timeSlotRepository.findById(slot.getId()).orElseThrow();
        assertEquals(0, updated.getReservedCount());
    }

    @Test
    @DisplayName("releaseCapacity returns false for invalid or nonexistent slot ID")
    void releaseCapacity_falseWhenNotFound() {
        assertFalse(slotCapacityService.releaseCapacity(null));
        assertFalse(slotCapacityService.releaseCapacity(999999L));
    }

    @Test
    @DisplayName("getSlotStartTime returns start timestamp of valid slot")
    void getSlotStartTime_success() {
        TimeSlot slot = new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 5);
        slot = timeSlotRepository.save(slot);

        Instant startTime = slotCapacityService.getSlotStartTime(slot.getId());
        assertNotNull(startTime);
        assertEquals(baseStart, startTime);
    }

    @Test
    @DisplayName("getSlotStartTime returns null for invalid slot ID")
    void getSlotStartTime_nullWhenNotFound() {
        assertNull(slotCapacityService.getSlotStartTime(null));
        assertNull(slotCapacityService.getSlotStartTime(999999L));
    }

    @Test
    @DisplayName("isSessionFull returns false when at least one slot has available capacity")
    void isSessionFull_falseWhenAtLeastOneSlotHasCapacity() {
        TimeSlot slotA = new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 5);
        slotA.setReservedCount(5);
        timeSlotRepository.save(slotA);

        TimeSlot slotB = new TimeSlot(scheduledSession, baseStart.plus(1, ChronoUnit.HOURS), baseStart.plus(2, ChronoUnit.HOURS), 5);
        slotB.setReservedCount(3);
        timeSlotRepository.save(slotB);

        assertFalse(slotCapacityService.isSessionFull(scheduledSession.getId()));
    }

    @Test
    @DisplayName("isSessionFull returns true when all slots in session are fully booked")
    void isSessionFull_trueWhenAllSlotsFullyBooked() {
        TimeSlot slotA = new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 5);
        slotA.setReservedCount(5);
        timeSlotRepository.save(slotA);

        TimeSlot slotB = new TimeSlot(scheduledSession, baseStart.plus(1, ChronoUnit.HOURS), baseStart.plus(2, ChronoUnit.HOURS), 3);
        slotB.setReservedCount(3);
        timeSlotRepository.save(slotB);

        assertTrue(slotCapacityService.isSessionFull(scheduledSession.getId()));
    }

    @Test
    @DisplayName("TimeSlot rejects occupancy that exceeds capacity")
    void reservedCountAboveCapacityIsRejected() {
        TimeSlot slot = new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 4);

        assertThrows(IllegalArgumentException.class, () -> slot.setReservedCount(5));
        assertEquals(0, slot.getReservedCount());
    }

    @Test
    @DisplayName("Valid TimeSlot entity persists")
    void validTimeSlotEntityPersists() {
        TimeSlot slot = timeSlotRepository.saveAndFlush(
                new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 4)
        );

        assertNotNull(slot.getId());
        assertEquals(4, slot.getCapacity());
        assertEquals(0, slot.getReservedCount());
    }

    @Test
    @DisplayName("JPA lifecycle rejects an invalid mutated TimeSlot during flush")
    void invalidMutatedTimeSlotCannotFlush() {
        TimeSlot slot = timeSlotRepository.saveAndFlush(
                new TimeSlot(scheduledSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 4)
        );
        ReflectionTestUtils.setField(slot, "reservedCount", 5);

        InvalidDataAccessApiUsageException exception = assertThrows(
                InvalidDataAccessApiUsageException.class,
                () -> timeSlotRepository.saveAndFlush(slot)
        );
        assertTrue(exception.getMostSpecificCause() instanceof IllegalArgumentException);
    }

    @Test
    @DisplayName("isSessionFull throws IllegalStateException when session has no slots configured")
    void isSessionFull_throwsWhenNoSlotsConfigured() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                slotCapacityService.isSessionFull(scheduledSession.getId())
        );
        assertTrue(ex.getMessage().contains("has no configured time slots"));
    }

    @Test
    @DisplayName("isSessionFull throws IllegalStateException when session is in DRAFT lifecycle status")
    void isSessionFull_throwsWhenSessionInDraftStatus() {
        // Without slots
        IllegalStateException ex1 = assertThrows(IllegalStateException.class, () ->
                slotCapacityService.isSessionFull(draftSession.getId())
        );
        assertTrue(ex1.getMessage().contains("DRAFT"));

        // With slots configured on draft session
        TimeSlot slot = new TimeSlot(draftSession, baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
        slot.setReservedCount(0);
        timeSlotRepository.save(slot);

        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () ->
                slotCapacityService.isSessionFull(draftSession.getId())
        );
        assertTrue(ex2.getMessage().contains("DRAFT"));
    }

    @Test
    @DisplayName("isSessionFull throws IllegalStateException when session is in CANCELLED lifecycle status")
    void isSessionFull_throwsWhenSessionInCancelledStatus() {
        VaccinationSession cancelledSession = new VaccinationSession(
                testCampaign, testCenter, baseStart, baseEnd, baseStart.minus(2, ChronoUnit.HOURS), "UTC");
        cancelledSession.cancel();
        cancelledSession = sessionRepository.save(cancelledSession);

        final Long cancelledId = cancelledSession.getId();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                slotCapacityService.isSessionFull(cancelledId)
        );
        assertTrue(ex.getMessage().contains("CANCELLED"));
    }

    @Test
    @DisplayName("isSessionFull throws IllegalStateException when session is IN_PROGRESS or COMPLETED")
    void isSessionFull_throwsWhenSessionInProgressOrCompleted() {
        VaccinationSession activeSession = new VaccinationSession(
                testCampaign, testCenter, baseStart, baseEnd, baseStart.minus(2, ChronoUnit.HOURS), "UTC");
        activeSession.schedule();
        activeSession.start();
        activeSession = sessionRepository.save(activeSession);

        final Long activeId = activeSession.getId();
        IllegalStateException ex1 = assertThrows(IllegalStateException.class, () ->
                slotCapacityService.isSessionFull(activeId)
        );
        assertTrue(ex1.getMessage().contains("IN_PROGRESS"));

        activeSession.complete();
        activeSession = sessionRepository.save(activeSession);

        final Long completedId = activeSession.getId();
        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () ->
                slotCapacityService.isSessionFull(completedId)
        );
        assertTrue(ex2.getMessage().contains("COMPLETED"));
    }

    @Test
    @DisplayName("isSessionFull throws IllegalArgumentException when sessionId is null or does not exist")
    void isSessionFull_throwsWhenSessionIdNullOrNotFound() {
        assertThrows(IllegalArgumentException.class, () -> slotCapacityService.isSessionFull(null));
        assertThrows(IllegalArgumentException.class, () -> slotCapacityService.isSessionFull(999999L));
    }

    @Test
    @DisplayName("createSlot creates slot within session window and enforces approved capacity bound")
    void createSlot_enforcesBounds() {
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 50, 60, 15, 0.0, null, "Approved override", "admin"
        );
        capacityPlanRepository.save(plan);

        TimeSlot slot1 = timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 30);
        assertNotNull(slot1.getId());
        assertEquals(30, slot1.getCapacity());

        TimeSlot slot2 = timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(1, ChronoUnit.HOURS), baseStart.plus(2, ChronoUnit.HOURS), 20);
        assertNotNull(slot2.getId());
        assertEquals(20, slot2.getCapacity());

        // Attempting to exceed approved capacity (50) should fail
        assertThrows(IllegalStateException.class, () ->
                timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(2, ChronoUnit.HOURS), baseStart.plus(3, ChronoUnit.HOURS), 10)
        );
    }

    @Test
    @DisplayName("createSlot rejects interval outside session window")
    void createSlot_rejectsOutsideSessionWindow() {
        assertThrows(IllegalArgumentException.class, () ->
                timeSlotService.createSlot(scheduledSession.getId(), baseStart.minus(1, ChronoUnit.HOURS), baseStart.plus(1, ChronoUnit.HOURS), 10)
        );
        assertThrows(IllegalArgumentException.class, () ->
                timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseEnd.plus(1, ChronoUnit.HOURS), 10)
        );
    }

    @Test
    @DisplayName("updateSlotCapacity enforces booking floor and approved session capacity")
    void updateSlotCapacity_enforcesFloorAndCap() {
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 50, 60, 15, 0.0, null, "Approved override", "admin"
        );
        capacityPlanRepository.save(plan);

        TimeSlot slot = timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 30);
        slot.setReservedCount(15);
        timeSlotRepository.save(slot);

        // Cannot reduce below reserved count (15)
        assertThrows(IllegalStateException.class, () ->
                timeSlotService.updateSlotCapacity(slot.getId(), 10)
        );

        // Valid reduction to 20 succeeds
        TimeSlot updated = timeSlotService.updateSlotCapacity(slot.getId(), 20);
        assertEquals(20, updated.getCapacity());

        // Cannot increase beyond session plan approved capacity (50)
        assertThrows(IllegalStateException.class, () ->
                timeSlotService.updateSlotCapacity(slot.getId(), 55)
        );
    }

    @Test
    @DisplayName("updateSlotCapacity enforces approved session capacity aggregate across multiple slots")
    void updateSlotCapacity_concurrentTwoSlotAggregate_enforcesApprovedCapacity() {
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 10, 60, 15, 0.0, null, null, "admin"
        );
        capacityPlanRepository.save(plan);

        TimeSlot slotA = timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 4);
        TimeSlot slotB = timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(1, ChronoUnit.HOURS), baseStart.plus(2, ChronoUnit.HOURS), 4);

        // Update slotA from 4 to 6 (total: 6 + 4 = 10 <= 10) -> succeeds
        TimeSlot updatedA = timeSlotService.updateSlotCapacity(slotA.getId(), 6);
        assertEquals(6, updatedA.getCapacity());

        // Update slotB from 4 to 6 (total would be 6 + 6 = 12 > 10) -> rejected
        assertThrows(IllegalStateException.class, () ->
                timeSlotService.updateSlotCapacity(slotB.getId(), 6)
        );

        Integer total = timeSlotRepository.sumCapacityBySessionId(scheduledSession.getId());
        assertEquals(10, total);
    }

    @Test
    @DisplayName("createSlot vs updateSlotCapacity enforces approved session capacity aggregate")
    void createSlot_vs_updateSlot_enforcesApprovedCapacity() {
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 10, 60, 15, 0.0, null, null, "admin"
        );
        capacityPlanRepository.save(plan);

        TimeSlot slotA = timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 4);
        timeSlotService.updateSlotCapacity(slotA.getId(), 6);

        // Attempting to create slotB with capacity 5 (total would be 6 + 5 = 11 > 10) -> rejected
        assertThrows(IllegalStateException.class, () ->
                timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(1, ChronoUnit.HOURS), baseStart.plus(2, ChronoUnit.HOURS), 5)
        );

        // Creating slotB with capacity 4 (total 6 + 4 = 10 <= 10) -> succeeds
        TimeSlot slotB = timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(1, ChronoUnit.HOURS), baseStart.plus(2, ChronoUnit.HOURS), 4);
        assertEquals(4, slotB.getCapacity());
    }

    @Test
    @DisplayName("createCapacityPlan rejects approved capacity below existing configured slot total")
    void createCapacityPlan_rejectsApprovedCapacityBelowExistingSlotTotal() {
        SessionCapacityPlan plan1 = new SessionCapacityPlan(
                draftSession, 1, 240, 10, 2, 2, 2, 2, 48, 20, 60, 15, 0.0, null, null, "admin"
        );
        capacityPlanRepository.save(plan1);

        timeSlotService.createSlot(draftSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
        timeSlotService.createSlot(draftSession.getId(), baseStart.plus(1, ChronoUnit.HOURS), baseStart.plus(2, ChronoUnit.HOURS), 10);

        // Configured slot total is 20. Attempting new capacity plan with approvedCapacity = 15 should fail
        CreateSessionCapacityPlanRequest requestInvalid = new CreateSessionCapacityPlanRequest(
                0, 15, 60, 15, 0.0, "Approved reduction", "admin"
        );
        assertThrows(IllegalStateException.class, () ->
                planService.createCapacityPlan(draftSession.getId(), requestInvalid)
        );

        // Attempting new capacity plan with approvedCapacity = 25 (override above 0 recommended) should succeed with ROLE_CENTER_MANAGER
        com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor manager =
                new com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor(42L, java.util.Set.of("ROLE_CENTER_MANAGER"));
        CreateSessionCapacityPlanRequest requestValid = new CreateSessionCapacityPlanRequest(
                0, 25, 60, 15, 0.0, "Approved expansion", "admin"
        );
        SessionCapacityPlan plan2 = planService.createCapacityPlan(draftSession.getId(), requestValid, manager);
        assertNotNull(plan2);
        assertEquals(25, plan2.getApprovedCapacity());
        assertEquals("42", plan2.getApprovedBy());
    }

    @Test
    @DisplayName("createSlot: overlapping slots within safe concurrent occupancy succeeds")
    void createSlot_overlappingSlotsWithinSafeOccupancy_succeeds() {
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 100, 30, 15, 0.0, null, "Capacity override", "admin"
        );
        capacityPlanRepository.save(plan);

        TimeSlot slot1 = timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(60, ChronoUnit.MINUTES), 15);
        TimeSlot slot2 = timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(30, ChronoUnit.MINUTES), baseStart.plus(90, ChronoUnit.MINUTES), 15);

        assertNotNull(slot1);
        assertNotNull(slot2);
        List<TimeSlot> slots = timeSlotService.getSlotsForSession(scheduledSession.getId());
        assertEquals(2, slots.size());
    }

    @Test
    @DisplayName("createSlot: overlapping slot exceeding safe concurrent occupancy fails and rolls back")
    void createSlot_overlappingSlotsExceedingSafeOccupancy_failsAndRollsBack() {
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 100, 25, 15, 0.0, null, "Capacity override", "admin"
        );
        capacityPlanRepository.save(plan);

        timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(60, ChronoUnit.MINUTES), 15);

        // Slot 2 overlaps by 30 mins, total peak = 15 + 15 = 30 > safe limit 25 -> must throw IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(30, ChronoUnit.MINUTES), baseStart.plus(90, ChronoUnit.MINUTES), 15)
        );
        assertTrue(ex.getMessage().contains("exceeds safe concurrent occupancy limit (25)"));

        List<TimeSlot> slots = timeSlotService.getSlotsForSession(scheduledSession.getId());
        assertEquals(1, slots.size(), "Failed slot creation must not persist partial records");
    }

    @Test
    @DisplayName("updateSlotCapacity: updating overlapping slot within safe concurrent occupancy succeeds")
    void updateSlotCapacity_overlappingSlotsWithinSafeOccupancy_succeeds() {
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 100, 40, 15, 0.0, null, "Capacity override", "admin"
        );
        capacityPlanRepository.save(plan);

        TimeSlot slot1 = timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(60, ChronoUnit.MINUTES), 15);
        TimeSlot slot2 = timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(30, ChronoUnit.MINUTES), baseStart.plus(90, ChronoUnit.MINUTES), 15);

        // Peak becomes 15 + 25 = 40 <= 40 -> succeeds
        TimeSlot updated = timeSlotService.updateSlotCapacity(slot2.getId(), 25);
        assertEquals(25, updated.getCapacity());
    }

    @Test
    @DisplayName("updateSlotCapacity: updating overlapping slot exceeding safe limit fails and preserves original capacity")
    void updateSlotCapacity_overlappingSlotsExceedingSafeOccupancy_failsAndPreservesOriginalCapacity() {
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 100, 30, 15, 0.0, null, "Capacity override", "admin"
        );
        capacityPlanRepository.save(plan);

        TimeSlot slot1 = timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(60, ChronoUnit.MINUTES), 15);
        TimeSlot slot2 = timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(30, ChronoUnit.MINUTES), baseStart.plus(90, ChronoUnit.MINUTES), 15);

        // Attempting to update slot2 to 20 -> Peak = 15 + 20 = 35 > 30 -> rejected
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                timeSlotService.updateSlotCapacity(slot2.getId(), 20)
        );
        assertTrue(ex.getMessage().contains("exceeds safe concurrent occupancy limit (30)"));

        TimeSlot persistedSlot2 = timeSlotRepository.findById(slot2.getId()).orElseThrow();
        assertEquals(15, persistedSlot2.getCapacity(), "Original slot capacity must be preserved on rejection");
    }

    @Test
    @DisplayName("createSlot: dwell time extension causing overlap on adjacent slots exceeding safe limit is rejected")
    void createSlot_dwellTimeExtensionCausingOverlap_exceedingSafeOccupancy_rejected() {
        // Slot duration = 15 min, but expectedDwellMinutes = 30 min -> effective occupancy lasts 30 min from start
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 100, 25, 30, 0.0, null, "Capacity override", "admin"
        );
        capacityPlanRepository.save(plan);

        // Slot 1: [baseStart, baseStart + 15m), effective dwell until baseStart + 30m, cap = 15
        timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(15, ChronoUnit.MINUTES), 15);

        // Slot 2: [baseStart + 15m, baseStart + 30m), cap = 15.
        // At boundary baseStart + 15m, slot 1 is still dwelling (effective dwell end = baseStart + 30m), so peak = 15 + 15 = 30 > 25
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(15, ChronoUnit.MINUTES), baseStart.plus(30, ChronoUnit.MINUTES), 15)
        );
        assertTrue(ex.getMessage().contains("exceeds safe concurrent occupancy limit (25)"));
    }

    @Test
    @DisplayName("createSlot: accompanying persons multiplier breaching safe concurrent occupancy is rejected")
    void createSlot_accompanyingPersonsMultiplierBreachingSafeOccupancy_rejected() {
        // Accompanying ratio = 0.5 -> 1.5x occupancy multiplier per booking
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 100, 20, 15, 0.5, null, "Capacity override", "admin"
        );
        capacityPlanRepository.save(plan);

        // Slot 1: cap = 10 -> expected units = 10 * 1.5 = 15.0 <= 20
        timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(60, ChronoUnit.MINUTES), 10);

        // Slot 2 overlapping: cap = 10 -> expected units = 15.0. Peak = 15.0 + 15.0 = 30.0 > 20
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(30, ChronoUnit.MINUTES), baseStart.plus(90, ChronoUnit.MINUTES), 10)
        );
        assertTrue(ex.getMessage().contains("Peak concurrent occupancy (30.0) exceeds safe concurrent occupancy limit (20)"));
    }

    @Test
    @DisplayName("createSlot: enforces lowered current center safety limit over stale higher plan snapshot")
    void createSlot_enforcesLoweredCenterSafetyLimit_overStaleHigherPlanSnapshot() {
        // Plan snapshot has safe occupancy = 40
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 100, 40, 15, 0.0, null, "Override", "admin"
        );
        capacityPlanRepository.save(plan);

        // Center safe occupancy is later lowered to 20
        testCenter.updateDetails("Capacity Center", "123 Main St", "+1234567890", 20, null);
        centerRepository.saveAndFlush(testCenter);

        // Slot 1: [baseStart, baseStart + 60m), cap = 15 <= 20
        timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(60, ChronoUnit.MINUTES), 15);

        // Slot 2 overlapping: [baseStart + 30m, baseStart + 90m), cap = 10.
        // Peak = 15 + 10 = 25. Under stale plan (40), it would pass. Under current center (20), it must fail!
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(30, ChronoUnit.MINUTES), baseStart.plus(90, ChronoUnit.MINUTES), 10)
        );
        assertTrue(ex.getMessage().contains("exceeds safe concurrent occupancy limit (20)"));
    }

    @Test
    @DisplayName("createSlot: respects conservative plan snapshot when center limit is higher")
    void createSlot_respectsConservativePlanSnapshot_whenCenterLimitIsHigher() {
        // Plan snapshot has conservative safe occupancy = 15 (center is 50)
        SessionCapacityPlan plan = new SessionCapacityPlan(
                scheduledSession, 1, 240, 10, 2, 2, 2, 2, 48, 100, 15, 15, 0.0, null, "Override", "admin"
        );
        capacityPlanRepository.save(plan);

        // Slot 1: [baseStart, baseStart + 60m), cap = 10
        timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(60, ChronoUnit.MINUTES), 10);

        // Slot 2 overlapping: [baseStart + 30m, baseStart + 90m), cap = 10. Peak = 20 > conservative plan (15).
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                timeSlotService.createSlot(scheduledSession.getId(), baseStart.plus(30, ChronoUnit.MINUTES), baseStart.plus(90, ChronoUnit.MINUTES), 10)
        );
        assertTrue(ex.getMessage().contains("exceeds safe concurrent occupancy limit (15)"));
    }

    @Test
    @DisplayName("Center safety lowering accepts configured peak below proposed limit")
    void updateCenter_safeOccupancyLoweringBelowConfiguredPeakAccepted() {
        setCenterSafeLimit(10);
        timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 4);

        setCenterSafeLimit(5);

        assertEquals(5, centerRepository.findById(testCenter.getId()).orElseThrow().getSafeConcurrentOccupancy());
    }

    @Test
    @DisplayName("Center safety lowering rejects configured peak above proposed limit atomically")
    void updateCenter_safeOccupancyLoweringAboveConfiguredPeakRejected() {
        setCenterSafeLimit(10);
        TimeSlot slot = timeSlotService.createSlot(
                scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 8);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> setCenterSafeLimit(5));

        assertTrue(error.getMessage().contains("cannot be lowered to 5"));
        assertTrue(error.getMessage().contains("configured peak occupancy 8.0"));
        assertEquals(10, centerRepository.findById(testCenter.getId()).orElseThrow().getSafeConcurrentOccupancy());
        assertEquals(8, timeSlotRepository.findById(slot.getId()).orElseThrow().getCapacity());
    }

    @Test
    @DisplayName("Center safety lowering accepts configured peak equal to proposed limit")
    void updateCenter_safeOccupancyLoweringEqualBoundaryAccepted() {
        setCenterSafeLimit(10);
        timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 5);

        setCenterSafeLimit(5);

        assertEquals(5, centerRepository.findById(testCenter.getId()).orElseThrow().getSafeConcurrentOccupancy());
    }

    @Test
    @DisplayName("Center safety lowering evaluates all operational sessions")
    void updateCenter_safeOccupancyLoweringEvaluatesAllOperationalSessions() {
        setCenterSafeLimit(10);
        timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 4);
        timeSlotService.createSlot(draftSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 7);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> setCenterSafeLimit(5));

        assertTrue(error.getMessage().contains("session " + draftSession.getId()));
        assertEquals(10, centerRepository.findById(testCenter.getId()).orElseThrow().getSafeConcurrentOccupancy());
    }

    @Test
    @DisplayName("Center safety lowering excludes terminal cancelled sessions")
    void updateCenter_safeOccupancyLoweringExcludesCancelledSessions() {
        setCenterSafeLimit(10);
        timeSlotService.createSlot(draftSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 8);
        draftSession.cancel();
        sessionRepository.saveAndFlush(draftSession);

        setCenterSafeLimit(5);

        assertEquals(5, centerRepository.findById(testCenter.getId()).orElseThrow().getSafeConcurrentOccupancy());
    }

    @Test
    @DisplayName("Center safety lowering reuses latest plan occupancy parameters")
    void updateCenter_safeOccupancyLoweringUsesLatestPlanOccupancyParameters() {
        setCenterSafeLimit(10);
        capacityPlanRepository.save(new SessionCapacityPlan(
                scheduledSession, 1, 480, 10, 1, 1, 1, 1, 48, 48,
                10, 15, 0.5, null, null, "admin"
        ));
        timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 4);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> setCenterSafeLimit(5));

        assertTrue(error.getMessage().contains("configured peak occupancy 6.0"));
        assertEquals(10, centerRepository.findById(testCenter.getId()).orElseThrow().getSafeConcurrentOccupancy());
    }

    private void setCenterSafeLimit(int safeConcurrentOccupancy) {
        testCenter = centerService.updateCenter(
                testCenter.getId(),
                testCenter.getName(),
                testCenter.getAddress(),
                testCenter.getContact(),
                safeConcurrentOccupancy,
                testCenter.getSessionServiceCeiling()
        );
    }

    @Nested
    @DisplayName("TimeSlot lifecycle guard matrix")
    class TimeSlotLifecycleGuardMatrixTests {

        @Test
        @DisplayName("createSlot: allowed in DRAFT status")
        void createSlot_allowedInDraft() {
            TimeSlot slot = timeSlotService.createSlot(draftSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
            assertNotNull(slot);
            assertEquals(10, slot.getCapacity());
            assertEquals(SessionLifecycleStatus.DRAFT, draftSession.getLifecycleStatus());
        }

        @Test
        @DisplayName("createSlot: allowed in SCHEDULED status")
        void createSlot_allowedInScheduled() {
            TimeSlot slot = timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
            assertNotNull(slot);
            assertEquals(10, slot.getCapacity());
            assertEquals(SessionLifecycleStatus.SCHEDULED, scheduledSession.getLifecycleStatus());
        }

        @Test
        @DisplayName("createSlot: rejected in IN_PROGRESS status")
        void createSlot_rejectedInInProgress() {
            VaccinationSession session = createSessionInStatus(SessionLifecycleStatus.IN_PROGRESS);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    timeSlotService.createSlot(session.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10));
            assertTrue(ex.getMessage().contains("Cannot configure time slots for session in IN_PROGRESS lifecycle status"));
        }

        @Test
        @DisplayName("createSlot: rejected in COMPLETED status")
        void createSlot_rejectedInCompleted() {
            VaccinationSession session = createSessionInStatus(SessionLifecycleStatus.COMPLETED);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    timeSlotService.createSlot(session.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10));
            assertTrue(ex.getMessage().contains("Cannot configure time slots for session in COMPLETED lifecycle status"));
        }

        @Test
        @DisplayName("createSlot: rejected in CANCELLED status")
        void createSlot_rejectedInCancelled() {
            VaccinationSession session = createSessionInStatus(SessionLifecycleStatus.CANCELLED);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    timeSlotService.createSlot(session.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10));
            assertTrue(ex.getMessage().contains("Cannot configure time slots for session in CANCELLED lifecycle status"));
        }

        @Test
        @DisplayName("updateSlotCapacity: allowed in DRAFT status")
        void updateSlotCapacity_allowedInDraft() {
            TimeSlot slot = timeSlotService.createSlot(draftSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
            TimeSlot updated = timeSlotService.updateSlotCapacity(slot.getId(), 15);
            assertNotNull(updated);
            assertEquals(15, updated.getCapacity());

            TimeSlot updatedWithSession = timeSlotService.updateSlotCapacity(draftSession.getId(), slot.getId(), 18);
            assertEquals(18, updatedWithSession.getCapacity());
        }

        @Test
        @DisplayName("updateSlotCapacity: allowed in SCHEDULED status")
        void updateSlotCapacity_allowedInScheduled() {
            TimeSlot slot = timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
            TimeSlot updated = timeSlotService.updateSlotCapacity(slot.getId(), 15);
            assertNotNull(updated);
            assertEquals(15, updated.getCapacity());

            TimeSlot updatedWithSession = timeSlotService.updateSlotCapacity(scheduledSession.getId(), slot.getId(), 18);
            assertEquals(18, updatedWithSession.getCapacity());
        }

        @Test
        @DisplayName("updateSlotCapacity: rejected in IN_PROGRESS status")
        void updateSlotCapacity_rejectedInInProgress() {
            TimeSlot slot = timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
            scheduledSession.start();
            sessionRepository.saveAndFlush(scheduledSession);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    timeSlotService.updateSlotCapacity(slot.getId(), 15));
            assertTrue(ex.getMessage().contains("Cannot configure time slots for session in IN_PROGRESS lifecycle status"));

            IllegalStateException ex2 = assertThrows(IllegalStateException.class, () ->
                    timeSlotService.updateSlotCapacity(scheduledSession.getId(), slot.getId(), 15));
            assertTrue(ex2.getMessage().contains("Cannot configure time slots for session in IN_PROGRESS lifecycle status"));
        }

        @Test
        @DisplayName("updateSlotCapacity: rejected in COMPLETED status")
        void updateSlotCapacity_rejectedInCompleted() {
            TimeSlot slot = timeSlotService.createSlot(scheduledSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
            scheduledSession.start();
            scheduledSession.complete();
            sessionRepository.saveAndFlush(scheduledSession);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    timeSlotService.updateSlotCapacity(slot.getId(), 15));
            assertTrue(ex.getMessage().contains("Cannot configure time slots for session in COMPLETED lifecycle status"));

            IllegalStateException ex2 = assertThrows(IllegalStateException.class, () ->
                    timeSlotService.updateSlotCapacity(scheduledSession.getId(), slot.getId(), 15));
            assertTrue(ex2.getMessage().contains("Cannot configure time slots for session in COMPLETED lifecycle status"));
        }

        @Test
        @DisplayName("updateSlotCapacity: rejected in CANCELLED status")
        void updateSlotCapacity_rejectedInCancelled() {
            TimeSlot slot = timeSlotService.createSlot(draftSession.getId(), baseStart, baseStart.plus(1, ChronoUnit.HOURS), 10);
            draftSession.cancel();
            sessionRepository.saveAndFlush(draftSession);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    timeSlotService.updateSlotCapacity(slot.getId(), 15));
            assertTrue(ex.getMessage().contains("Cannot configure time slots for session in CANCELLED lifecycle status"));

            IllegalStateException ex2 = assertThrows(IllegalStateException.class, () ->
                    timeSlotService.updateSlotCapacity(draftSession.getId(), slot.getId(), 15));
            assertTrue(ex2.getMessage().contains("Cannot configure time slots for session in CANCELLED lifecycle status"));
        }

        private VaccinationSession createSessionInStatus(SessionLifecycleStatus targetStatus) {
            VaccinationSession session = new VaccinationSession(
                    testCampaign,
                    testCenter,
                    baseStart,
                    baseEnd,
                    baseStart.minus(2, ChronoUnit.HOURS),
                    "UTC"
            );
            if (targetStatus == SessionLifecycleStatus.SCHEDULED) {
                session.schedule();
            } else if (targetStatus == SessionLifecycleStatus.IN_PROGRESS) {
                session.schedule();
                session.start();
            } else if (targetStatus == SessionLifecycleStatus.COMPLETED) {
                session.schedule();
                session.start();
                session.complete();
            } else if (targetStatus == SessionLifecycleStatus.CANCELLED) {
                session.cancel();
            }
            return sessionRepository.saveAndFlush(session);
        }
    }
}
