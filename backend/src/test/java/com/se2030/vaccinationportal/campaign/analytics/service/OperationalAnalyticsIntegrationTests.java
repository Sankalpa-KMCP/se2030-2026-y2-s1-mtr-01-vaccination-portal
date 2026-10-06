package com.se2030.vaccinationportal.campaign.analytics.service;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.session.planning.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;


import com.se2030.vaccinationportal.campaign.analytics.dto.response.OperationalAnalyticsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OperationalAnalyticsIntegrationTests {

    @Autowired
    private OperationalAnalyticsService analyticsService;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private VaccinationSessionRepository sessionRepository;

    @Autowired
    private SessionCapacityPlanRepository capacityPlanRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private SessionResourceAssignmentRepository resourceAssignmentRepository;

    @Autowired
    private StaffAssignmentRepository staffAssignmentRepository;

    @Autowired
    private OperationalResourceRepository resourceRepository;

    @Autowired
    private OperationalStaffRosterRepository staffRosterRepository;

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    private VaccinationCenter center1;
    private VaccinationCenter center2;
    private VaccinationCampaign campaign1;
    private VaccinationCampaign campaign2;
    private VaccinationSession session1;
    private VaccinationSession session2;
    private VaccinationSession sessionCancelled;

    @BeforeEach
    void setUp() {
        VaccineType vaccineType = vaccineTypeRepository.save(new VaccineType("VAX-INT-01", "MRNA COVID-19", "dose", true));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-INT-01", "Adults 18-60", true));

        center1 = centerRepository.save(new VaccinationCenter("CTR-INT-01", "Center North", "100 North Rd", "011223344", CenterStatus.ACTIVE, 50, 100, "UTC"));
        center2 = centerRepository.save(new VaccinationCenter("CTR-INT-02", "Center South", "200 South Rd", "011223355", CenterStatus.ACTIVE, 60, 120, "UTC"));

        campaign1 = campaignRepository.save(new VaccinationCampaign(
                "CMP-INT-01", "National Campaign 1", "Description 1",
                vaccineType, targetGroup,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z")
        ));
        campaign1.publish();
        campaign1.activate();
        campaign1 = campaignRepository.save(campaign1);

        campaign2 = campaignRepository.save(new VaccinationCampaign(
                "CMP-INT-02", "National Campaign 2", "Description 2",
                vaccineType, targetGroup,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z")
        ));
        campaign2.cancel();
        campaign2 = campaignRepository.save(campaign2);

        Instant start1 = Instant.parse("2026-10-10T08:00:00Z");
        Instant end1 = Instant.parse("2026-10-10T12:00:00Z");
        session1 = sessionRepository.save(new VaccinationSession(campaign1, center1, start1, end1, start1, "UTC"));
        session1.schedule();
        session1 = sessionRepository.save(session1);

        Instant start2 = Instant.parse("2026-10-11T08:00:00Z");
        Instant end2 = Instant.parse("2026-10-11T12:00:00Z");
        session2 = sessionRepository.save(new VaccinationSession(campaign1, center2, start2, end2, start2, "UTC"));
        session2.schedule();
        session2.start();
        session2 = sessionRepository.save(session2);

        Instant start3 = Instant.parse("2026-10-12T08:00:00Z");
        Instant end3 = Instant.parse("2026-10-12T12:00:00Z");
        sessionCancelled = sessionRepository.save(new VaccinationSession(campaign1, center1, start3, end3, start3, "UTC"));
        sessionCancelled.cancel();
        sessionCancelled = sessionRepository.save(sessionCancelled);
    }

    @Test
    @DisplayName("Revision deduplication: only latest SessionCapacityPlan revision per session is summed")
    void getOperationalAnalytics_capacityPlanRevisionDeduplication() {
        // Session 1: revision 1 (approved = 40, recommended = 48)
        SessionCapacityPlan rev1 = new SessionCapacityPlan(session1, 1, 240, 10, 2, 2, 2, 2, 48, 40, 50, 15, 0.0, null, null, "Planner");
        capacityPlanRepository.save(rev1);

        // Session 1: revision 2 (approved = 60, recommended = 72) - SHOULD OVERRIDE REV 1
        SessionCapacityPlan rev2 = new SessionCapacityPlan(session1, 2, 240, 10, 3, 3, 3, 3, 72, 60, 50, 15, 0.0, null, null, "Planner");
        capacityPlanRepository.save(rev2);

        // Session 2: revision 1 (approved = 45, recommended = 48)
        SessionCapacityPlan s2Rev1 = new SessionCapacityPlan(session2, 1, 240, 10, 2, 2, 2, 2, 48, 45, 50, 15, 0.0, null, null, "Planner");
        capacityPlanRepository.save(s2Rev1);

        // Cancelled session should be excluded from capacity sums
        SessionCapacityPlan sCanRev1 = new SessionCapacityPlan(sessionCancelled, 1, 240, 10, 2, 2, 2, 2, 48, 45, 50, 15, 0.0, null, null, "Planner");
        capacityPlanRepository.save(sCanRev1);

        OperationalAnalyticsResponse response = analyticsService.getOperationalAnalytics(null, null);

        // Total approved: Session 1 (60) + Session 2 (45) = 105 (NOT 40+60+45+45 = 190)
        assertEquals(105L, response.totalApprovedCapacity());
        // Total recommended: Session 1 (72) + Session 2 (48) = 120 (NOT 48+72+48+48 = 216)
        assertEquals(120L, response.totalRecommendedThroughput());
    }

    @Test
    @DisplayName("TimeSlot capacity, reservations, and utilization reconciliation")
    void getOperationalAnalytics_timeSlotReconciliation() {
        // Session 1 slots: 10 cap, 6 reserved
        TimeSlot slot1 = new TimeSlot(session1, Instant.parse("2026-10-10T08:00:00Z"), Instant.parse("2026-10-10T09:00:00Z"), 10);
        slot1.setReservedCount(6);
        timeSlotRepository.save(slot1);

        // Session 1 slots: 10 cap, 4 reserved
        TimeSlot slot2 = new TimeSlot(session1, Instant.parse("2026-10-10T09:00:00Z"), Instant.parse("2026-10-10T10:00:00Z"), 10);
        slot2.setReservedCount(4);
        timeSlotRepository.save(slot2);

        // Session 2 slots: 30 cap, 15 reserved
        TimeSlot slot3 = new TimeSlot(session2, Instant.parse("2026-10-11T08:00:00Z"), Instant.parse("2026-10-11T09:00:00Z"), 30);
        slot3.setReservedCount(15);
        timeSlotRepository.save(slot3);

        // Cancelled session slots should be excluded
        TimeSlot slotCancelled = new TimeSlot(sessionCancelled, Instant.parse("2026-10-12T08:00:00Z"), Instant.parse("2026-10-12T09:00:00Z"), 20);
        slotCancelled.setReservedCount(10);
        timeSlotRepository.save(slotCancelled);

        OperationalAnalyticsResponse response = analyticsService.getOperationalAnalytics(null, null);

        // Configured: 10 + 10 + 30 = 50
        assertEquals(50L, response.totalConfiguredSlotCapacity());
        // Reserved: 6 + 4 + 15 = 25
        assertEquals(25L, response.totalReservedSlots());
        // Remaining: 50 - 25 = 25
        assertEquals(25L, response.totalRemainingSlotCapacity());
        // Utilization: 25 / 50 = 50.0%
        assertEquals(50.0, response.slotUtilizationPercentage(), 0.001);
    }

    @Test
    @DisplayName("Status breakdown and operational cancellation counts")
    void getOperationalAnalytics_statusAndCancellations() {
        OperationalAnalyticsResponse globalResponse = analyticsService.getOperationalAnalytics(null, null);
        assertTrue(globalResponse.campaignCountByStatus().get(CampaignStatus.ACTIVE) >= 1L);
        assertTrue(globalResponse.campaignCountByStatus().get(CampaignStatus.CANCELLED) >= 1L);
        assertTrue(globalResponse.cancelledCampaignCount() >= 1L);
        assertTrue(globalResponse.cancelledSessionCount() >= 1L);

        // Scoped by campaign1 and center1
        OperationalAnalyticsResponse scopedResponse = analyticsService.getOperationalAnalytics(center1.getId(), campaign1.getId());
        assertEquals(1L, scopedResponse.campaignCountByStatus().get(CampaignStatus.ACTIVE));
        assertEquals(0L, scopedResponse.campaignCountByStatus().get(CampaignStatus.CANCELLED));
        assertEquals(0L, scopedResponse.campaignCountByStatus().get(CampaignStatus.DRAFT));

        assertEquals(1L, scopedResponse.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.SCHEDULED));
        assertEquals(0L, scopedResponse.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.IN_PROGRESS));
        assertEquals(1L, scopedResponse.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.CANCELLED));

        assertEquals(0L, scopedResponse.cancelledCampaignCount());
        assertEquals(1L, scopedResponse.cancelledSessionCount());
    }

    @Test
    @DisplayName("Resource and staff load aggregation")
    void getOperationalAnalytics_resourceLoad() {
        OperationalResource station1 = resourceRepository.save(new OperationalResource(center1, "ST-01", ResourceType.VACCINATION_STATION, "Station 1", OperationalResourceStatus.ACTIVE));
        OperationalResource station2 = resourceRepository.save(new OperationalResource(center1, "ST-02", ResourceType.VACCINATION_STATION, "Station 2", OperationalResourceStatus.ACTIVE));
        OperationalResource equip1 = resourceRepository.save(new OperationalResource(center1, "EQ-01", ResourceType.EQUIPMENT, "Equip 1", OperationalResourceStatus.ACTIVE));
        OperationalResource room1 = resourceRepository.save(new OperationalResource(center1, "RM-01", ResourceType.ROOM, "Room 1", OperationalResourceStatus.ACTIVE));

        OperationalStaffRoster roster1 = staffRosterRepository.save(new OperationalStaffRoster(center1, 501L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE));
        OperationalStaffRoster roster2 = staffRosterRepository.save(new OperationalStaffRoster(center1, 502L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE));

        resourceAssignmentRepository.save(new SessionResourceAssignment(session1, station1, session1.getStartAt(), session1.getEndAt()));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session1, station2, session1.getStartAt(), session1.getEndAt()));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session1, equip1, session1.getStartAt(), session1.getEndAt()));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session1, room1, session1.getStartAt(), session1.getEndAt()));

        staffAssignmentRepository.save(new StaffAssignment(session1, roster1, StaffRole.VACCINATOR, session1.getStartAt(), session1.getEndAt()));
        staffAssignmentRepository.save(new StaffAssignment(session1, roster2, StaffRole.VACCINATOR, session1.getStartAt(), session1.getEndAt()));

        OperationalAnalyticsResponse response = analyticsService.getOperationalAnalytics(center1.getId(), null);

        assertEquals(2L, response.assignedStationCount());
        assertEquals(2L, response.assignedVaccinatorCount());
        assertEquals(2L, response.assignedResourceCountsByType().get(ResourceType.VACCINATION_STATION));
        assertEquals(1L, response.assignedResourceCountsByType().get(ResourceType.EQUIPMENT));
        assertEquals(1L, response.assignedResourceCountsByType().get(ResourceType.ROOM));
    }

    @Test
    @DisplayName("Filter scoping: centerId scopes metrics to the designated center")
    void getOperationalAnalytics_filterScopingByCenter() {
        // Session 1 is in center1, Session 2 is in center2
        TimeSlot slot1 = new TimeSlot(session1, Instant.parse("2026-10-10T08:00:00Z"), Instant.parse("2026-10-10T09:00:00Z"), 20);
        slot1.setReservedCount(10);
        timeSlotRepository.save(slot1);

        TimeSlot slot2 = new TimeSlot(session2, Instant.parse("2026-10-11T08:00:00Z"), Instant.parse("2026-10-11T09:00:00Z"), 50);
        slot2.setReservedCount(25);
        timeSlotRepository.save(slot2);

        OperationalAnalyticsResponse center1Response = analyticsService.getOperationalAnalytics(center1.getId(), null);

        // Center 1 only has slot1 (20 cap, 10 reserved)
        assertEquals(20L, center1Response.totalConfiguredSlotCapacity());
        assertEquals(10L, center1Response.totalReservedSlots());
        assertEquals(50.0, center1Response.slotUtilizationPercentage(), 0.001);
        assertEquals(1L, center1Response.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.SCHEDULED));
        assertEquals(0L, center1Response.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("Center filter scopes distinct campaign counts to campaigns represented at that center")
    void getOperationalAnalytics_centerFilterScopingForCampaigns() {
        // Campaign 1 has sessions at Center 1 (session1) and Center 2 (session2).
        // Campaign 2 has a session ONLY at Center 2.
        Instant startCamp2 = Instant.parse("2026-11-15T08:00:00Z");
        Instant endCamp2 = Instant.parse("2026-11-15T12:00:00Z");
        VaccinationSession sessionCamp2Center2 = sessionRepository.save(
                new VaccinationSession(campaign2, center2, startCamp2, endCamp2, startCamp2, "UTC")
        );

        // Center 1 filter: Campaign 1 is present, Campaign 2 is NOT present
        OperationalAnalyticsResponse center1Response = analyticsService.getOperationalAnalytics(center1.getId(), null);
        assertEquals(1L, center1Response.campaignCountByStatus().get(CampaignStatus.ACTIVE));
        assertEquals(0L, center1Response.campaignCountByStatus().get(CampaignStatus.CANCELLED));
        assertEquals(0L, center1Response.cancelledCampaignCount());

        // Center 2 filter: Both Campaign 1 and Campaign 2 are present
        OperationalAnalyticsResponse center2Response = analyticsService.getOperationalAnalytics(center2.getId(), null);
        assertEquals(1L, center2Response.campaignCountByStatus().get(CampaignStatus.ACTIVE));
        assertEquals(1L, center2Response.campaignCountByStatus().get(CampaignStatus.CANCELLED));
        assertEquals(1L, center2Response.cancelledCampaignCount());
    }

    @Test
    @DisplayName("Intersection semantics: centerId and campaignId applied simultaneously")
    void getOperationalAnalytics_centerAndCampaignIntersection() {
        // Query Center 1 + Campaign 1
        OperationalAnalyticsResponse intersectionResponse = analyticsService.getOperationalAnalytics(center1.getId(), campaign1.getId());
        assertEquals(1L, intersectionResponse.campaignCountByStatus().get(CampaignStatus.ACTIVE));
        assertEquals(0L, intersectionResponse.campaignCountByStatus().get(CampaignStatus.CANCELLED));
        assertEquals(0L, intersectionResponse.cancelledCampaignCount());

        // Only session1 (SCHEDULED) and sessionCancelled (CANCELLED) are in Center 1 for Campaign 1
        assertEquals(1L, intersectionResponse.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.SCHEDULED));
        assertEquals(0L, intersectionResponse.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.IN_PROGRESS));
        assertEquals(1L, intersectionResponse.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.CANCELLED));
        assertEquals(1L, intersectionResponse.cancelledSessionCount());
    }
}
