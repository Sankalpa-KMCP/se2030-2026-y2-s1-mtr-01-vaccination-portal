package com.se2030.vaccinationportal.campaign.analytics.service;

import com.se2030.vaccinationportal.campaign.analytics.dto.response.OperationalAnalyticsResponse;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalAnalyticsServiceTests {

    @Mock
    private VaccinationCampaignRepository campaignRepository;
    @Mock
    private VaccinationSessionRepository sessionRepository;
    @Mock
    private SessionCapacityPlanRepository capacityPlanRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private SessionResourceAssignmentRepository resourceAssignmentRepository;
    @Mock
    private StaffAssignmentRepository staffAssignmentRepository;
    @Mock
    private VaccinationCenterRepository centerRepository;

    private OperationalAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new OperationalAnalyticsService(
                campaignRepository,
                sessionRepository,
                capacityPlanRepository,
                timeSlotRepository,
                resourceAssignmentRepository,
                staffAssignmentRepository,
                centerRepository
        );
    }

    @Test
    @DisplayName("Empty repository data returns safe zero counts, empty maps initialized, and 0.0% utilization")
    void getOperationalAnalytics_emptyData_returnsSafeZeroes() {
        when(campaignRepository.countCampaignsByStatus(null)).thenReturn(new ArrayList<>());
        when(sessionRepository.countSessionsByLifecycleStatus(null, null)).thenReturn(new ArrayList<>());

        List<Object[]> capacityRows = new ArrayList<>();
        capacityRows.add(new Object[]{0L, 0L});
        when(capacityPlanRepository.sumLatestApprovedCapacityAndRecommendedThroughput(null, null, SessionLifecycleStatus.CANCELLED))
                .thenReturn(capacityRows);

        List<Object[]> slotRows = new ArrayList<>();
        slotRows.add(new Object[]{0L, 0L});
        when(timeSlotRepository.sumCapacityAndReservedCount(null, null, SessionLifecycleStatus.CANCELLED))
                .thenReturn(slotRows);

        when(sessionRepository.countCancelledSessions(null, null, SessionLifecycleStatus.CANCELLED)).thenReturn(0L);
        when(campaignRepository.countCancelledCampaigns(null, CampaignStatus.CANCELLED)).thenReturn(0L);
        when(resourceAssignmentRepository.countAssignedStations(null, null, ResourceType.VACCINATION_STATION, OperationalResourceStatus.ACTIVE, SessionLifecycleStatus.CANCELLED))
                .thenReturn(0L);
        when(staffAssignmentRepository.countAssignedVaccinators(null, null, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE, SessionLifecycleStatus.CANCELLED))
                .thenReturn(0L);
        when(resourceAssignmentRepository.countAssignedResourcesByType(null, null, OperationalResourceStatus.ACTIVE, SessionLifecycleStatus.CANCELLED))
                .thenReturn(new ArrayList<>());

        OperationalAnalyticsResponse response = analyticsService.getOperationalAnalytics(null, null);

        assertNotNull(response);
        assertNotNull(response.campaignCountByStatus());
        assertEquals(CampaignStatus.values().length, response.campaignCountByStatus().size());
        assertEquals(0L, response.campaignCountByStatus().get(CampaignStatus.ACTIVE));
        assertEquals(0L, response.campaignCountByStatus().get(CampaignStatus.DRAFT));

        assertNotNull(response.sessionCountByLifecycleStatus());
        assertEquals(SessionLifecycleStatus.values().length, response.sessionCountByLifecycleStatus().size());
        assertEquals(0L, response.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.SCHEDULED));

        assertEquals(0L, response.totalApprovedCapacity());
        assertEquals(0L, response.totalRecommendedThroughput());
        assertEquals(0L, response.totalConfiguredSlotCapacity());
        assertEquals(0L, response.totalReservedSlots());
        assertEquals(0L, response.totalRemainingSlotCapacity());
        assertEquals(0.0, response.slotUtilizationPercentage(), 0.001);
        assertEquals(0L, response.cancelledSessionCount());
        assertEquals(0L, response.cancelledCampaignCount());
        assertEquals(0L, response.assignedStationCount());
        assertEquals(0L, response.assignedVaccinatorCount());
        assertNotNull(response.assignedResourceCountsByType());
        assertEquals(0L, response.assignedResourceCountsByType().get(ResourceType.VACCINATION_STATION));
        assertNotNull(response.calculatedAt());
    }

    @Test
    @DisplayName("Populated operational aggregates are calculated correctly with utilization")
    void getOperationalAnalytics_populatedData_calculatesCorrectly() {
        List<Object[]> campaignStatusRows = new ArrayList<>();
        campaignStatusRows.add(new Object[]{CampaignStatus.ACTIVE, 3L});
        campaignStatusRows.add(new Object[]{CampaignStatus.PUBLISHED, 2L});
        campaignStatusRows.add(new Object[]{CampaignStatus.CANCELLED, 1L});
        when(campaignRepository.countCampaignsByStatus(null)).thenReturn(campaignStatusRows);

        List<Object[]> sessionStatusRows = new ArrayList<>();
        sessionStatusRows.add(new Object[]{SessionLifecycleStatus.SCHEDULED, 10L});
        sessionStatusRows.add(new Object[]{SessionLifecycleStatus.IN_PROGRESS, 2L});
        sessionStatusRows.add(new Object[]{SessionLifecycleStatus.CANCELLED, 1L});
        when(sessionRepository.countSessionsByLifecycleStatus(null, null)).thenReturn(sessionStatusRows);

        List<Object[]> capacityRows = new ArrayList<>();
        capacityRows.add(new Object[]{1200L, 1400L});
        when(capacityPlanRepository.sumLatestApprovedCapacityAndRecommendedThroughput(null, null, SessionLifecycleStatus.CANCELLED))
                .thenReturn(capacityRows);

        List<Object[]> slotRows = new ArrayList<>();
        slotRows.add(new Object[]{1000L, 650L});
        when(timeSlotRepository.sumCapacityAndReservedCount(null, null, SessionLifecycleStatus.CANCELLED))
                .thenReturn(slotRows);

        when(sessionRepository.countCancelledSessions(null, null, SessionLifecycleStatus.CANCELLED)).thenReturn(1L);
        when(campaignRepository.countCancelledCampaigns(null, CampaignStatus.CANCELLED)).thenReturn(1L);
        when(resourceAssignmentRepository.countAssignedStations(null, null, ResourceType.VACCINATION_STATION, OperationalResourceStatus.ACTIVE, SessionLifecycleStatus.CANCELLED))
                .thenReturn(15L);
        when(staffAssignmentRepository.countAssignedVaccinators(null, null, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE, SessionLifecycleStatus.CANCELLED))
                .thenReturn(18L);

        List<Object[]> resourceTypeRows = new ArrayList<>();
        resourceTypeRows.add(new Object[]{ResourceType.VACCINATION_STATION, 15L});
        resourceTypeRows.add(new Object[]{ResourceType.EQUIPMENT, 12L});
        resourceTypeRows.add(new Object[]{ResourceType.ROOM, 5L});
        when(resourceAssignmentRepository.countAssignedResourcesByType(null, null, OperationalResourceStatus.ACTIVE, SessionLifecycleStatus.CANCELLED))
                .thenReturn(resourceTypeRows);

        OperationalAnalyticsResponse response = analyticsService.getOperationalAnalytics(null, null);

        assertNotNull(response);
        assertEquals(3L, response.campaignCountByStatus().get(CampaignStatus.ACTIVE));
        assertEquals(2L, response.campaignCountByStatus().get(CampaignStatus.PUBLISHED));
        assertEquals(1L, response.campaignCountByStatus().get(CampaignStatus.CANCELLED));

        assertEquals(10L, response.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.SCHEDULED));
        assertEquals(2L, response.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.IN_PROGRESS));
        assertEquals(1L, response.sessionCountByLifecycleStatus().get(SessionLifecycleStatus.CANCELLED));

        assertEquals(1200L, response.totalApprovedCapacity());
        assertEquals(1400L, response.totalRecommendedThroughput());
        assertEquals(1000L, response.totalConfiguredSlotCapacity());
        assertEquals(650L, response.totalReservedSlots());
        assertEquals(350L, response.totalRemainingSlotCapacity());
        assertEquals(65.0, response.slotUtilizationPercentage(), 0.001);

        assertEquals(1L, response.cancelledSessionCount());
        assertEquals(1L, response.cancelledCampaignCount());
        assertEquals(15L, response.assignedStationCount());
        assertEquals(18L, response.assignedVaccinatorCount());
        assertEquals(15L, response.assignedResourceCountsByType().get(ResourceType.VACCINATION_STATION));
        assertEquals(12L, response.assignedResourceCountsByType().get(ResourceType.EQUIPMENT));
        assertEquals(5L, response.assignedResourceCountsByType().get(ResourceType.ROOM));
    }

    @Test
    @DisplayName("When centerId is provided, campaign metrics are queried through session repository for center scope")
    void getOperationalAnalytics_centerScopedCampaigns_queriesSessionRepository() {
        when(centerRepository.existsById(10L)).thenReturn(true);

        List<Object[]> distinctCampaignRows = new ArrayList<>();
        distinctCampaignRows.add(new Object[]{CampaignStatus.ACTIVE, 2L});
        when(sessionRepository.countDistinctCampaignsByStatusForCenter(10L, null)).thenReturn(distinctCampaignRows);
        when(sessionRepository.countSessionsByLifecycleStatus(null, 10L)).thenReturn(new ArrayList<>());

        List<Object[]> capacityRows = new ArrayList<>();
        capacityRows.add(new Object[]{500L, 600L});
        when(capacityPlanRepository.sumLatestApprovedCapacityAndRecommendedThroughput(null, 10L, SessionLifecycleStatus.CANCELLED))
                .thenReturn(capacityRows);

        List<Object[]> slotRows = new ArrayList<>();
        slotRows.add(new Object[]{400L, 200L});
        when(timeSlotRepository.sumCapacityAndReservedCount(null, 10L, SessionLifecycleStatus.CANCELLED))
                .thenReturn(slotRows);

        when(sessionRepository.countCancelledSessions(null, 10L, SessionLifecycleStatus.CANCELLED)).thenReturn(0L);
        when(sessionRepository.countDistinctCancelledCampaignsForCenter(10L, null, CampaignStatus.CANCELLED)).thenReturn(0L);
        when(resourceAssignmentRepository.countAssignedStations(null, 10L, ResourceType.VACCINATION_STATION, OperationalResourceStatus.ACTIVE, SessionLifecycleStatus.CANCELLED))
                .thenReturn(4L);
        when(staffAssignmentRepository.countAssignedVaccinators(null, 10L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE, SessionLifecycleStatus.CANCELLED))
                .thenReturn(6L);
        when(resourceAssignmentRepository.countAssignedResourcesByType(null, 10L, OperationalResourceStatus.ACTIVE, SessionLifecycleStatus.CANCELLED))
                .thenReturn(new ArrayList<>());

        OperationalAnalyticsResponse response = analyticsService.getOperationalAnalytics(10L, null);

        assertNotNull(response);
        assertEquals(2L, response.campaignCountByStatus().get(CampaignStatus.ACTIVE));
        assertEquals(0L, response.campaignCountByStatus().get(CampaignStatus.CANCELLED));
        assertEquals(0L, response.cancelledCampaignCount());
    }

    @Test
    @DisplayName("Invalid centerId throws IllegalArgumentException")
    void getOperationalAnalytics_invalidCenterId_throwsException() {
        when(centerRepository.existsById(999L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getOperationalAnalytics(999L, null)
        );
        assertTrue(ex.getMessage().contains("Vaccination center not found: 999"));
    }

    @Test
    @DisplayName("Invalid campaignId throws IllegalArgumentException")
    void getOperationalAnalytics_invalidCampaignId_throwsException() {
        when(campaignRepository.existsById(888L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getOperationalAnalytics(null, 888L)
        );
        assertTrue(ex.getMessage().contains("Vaccination campaign not found: 888"));
    }
}
