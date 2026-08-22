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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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

        // Attempting new capacity plan with approvedCapacity = 25 should succeed
        CreateSessionCapacityPlanRequest requestValid = new CreateSessionCapacityPlanRequest(
                0, 25, 60, 15, 0.0, "Approved expansion", "admin"
        );
        SessionCapacityPlan plan2 = planService.createCapacityPlan(draftSession.getId(), requestValid);
        assertNotNull(plan2);
        assertEquals(25, plan2.getApprovedCapacity());
    }
}
