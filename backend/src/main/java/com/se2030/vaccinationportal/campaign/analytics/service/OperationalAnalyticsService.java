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
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Summarizes campaign, session and capacity data for operational reporting. */
@Service
public class OperationalAnalyticsService {

    private final VaccinationCampaignRepository campaignRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final SessionCapacityPlanRepository capacityPlanRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final SessionResourceAssignmentRepository resourceAssignmentRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final VaccinationCenterRepository centerRepository;

    public OperationalAnalyticsService(
            VaccinationCampaignRepository campaignRepository,
            VaccinationSessionRepository sessionRepository,
            SessionCapacityPlanRepository capacityPlanRepository,
            TimeSlotRepository timeSlotRepository,
            SessionResourceAssignmentRepository resourceAssignmentRepository,
            StaffAssignmentRepository staffAssignmentRepository,
            VaccinationCenterRepository centerRepository
    ) {
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "VaccinationCampaignRepository must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "VaccinationSessionRepository must not be null");
        this.capacityPlanRepository = Objects.requireNonNull(capacityPlanRepository, "SessionCapacityPlanRepository must not be null");
        this.timeSlotRepository = Objects.requireNonNull(timeSlotRepository, "TimeSlotRepository must not be null");
        this.resourceAssignmentRepository = Objects.requireNonNull(resourceAssignmentRepository, "SessionResourceAssignmentRepository must not be null");
        this.staffAssignmentRepository = Objects.requireNonNull(staffAssignmentRepository, "StaffAssignmentRepository must not be null");
        this.centerRepository = Objects.requireNonNull(centerRepository, "VaccinationCenterRepository must not be null");
    }

    @Transactional(readOnly = true)
    public OperationalAnalyticsResponse getOperationalAnalytics(Long centerId, Long campaignId) {
        // Validate filter presence if provided
        if (centerId != null && !centerRepository.existsById(centerId)) {
            throw new IllegalArgumentException("Vaccination center not found: " + centerId);
        }
        if (campaignId != null && !campaignRepository.existsById(campaignId)) {
            throw new IllegalArgumentException("Vaccination campaign not found: " + campaignId);
        }

        // 1. Campaign count by status (scoped to center if centerId is provided, otherwise portal-wide)
        Map<CampaignStatus, Long> campaignCountByStatus = new EnumMap<>(CampaignStatus.class);
        for (CampaignStatus status : CampaignStatus.values()) {
            campaignCountByStatus.put(status, 0L);
        }
        List<Object[]> campaignStatusRows = (centerId != null)
                ? sessionRepository.countDistinctCampaignsByStatusForCenter(centerId, campaignId)
                : campaignRepository.countCampaignsByStatus(campaignId);
        for (Object[] row : campaignStatusRows) {
            if (row != null && row.length >= 2 && row[0] instanceof CampaignStatus status && row[1] instanceof Number count) {
                campaignCountByStatus.put(status, count.longValue());
            }
        }

        // 2. Session count by lifecycle status
        Map<SessionLifecycleStatus, Long> sessionCountByLifecycleStatus = new EnumMap<>(SessionLifecycleStatus.class);
        for (SessionLifecycleStatus status : SessionLifecycleStatus.values()) {
            sessionCountByLifecycleStatus.put(status, 0L);
        }
        List<Object[]> sessionStatusRows = sessionRepository.countSessionsByLifecycleStatus(campaignId, centerId);
        for (Object[] row : sessionStatusRows) {
            if (row != null && row.length >= 2 && row[0] instanceof SessionLifecycleStatus status && row[1] instanceof Number count) {
                sessionCountByLifecycleStatus.put(status, count.longValue());
            }
        }

        // 3. Reconciled capacity from latest capacity plan revisions
        long totalApprovedCapacity = 0L;
        long totalRecommendedThroughput = 0L;
        List<Object[]> capacityRows = capacityPlanRepository.sumLatestApprovedCapacityAndRecommendedThroughput(
                campaignId, centerId, SessionLifecycleStatus.CANCELLED
        );
        if (capacityRows != null && !capacityRows.isEmpty()) {
            Object[] row = capacityRows.get(0);
            if (row != null && row.length >= 2) {
                if (row[0] instanceof Number approved) {
                    totalApprovedCapacity = approved.longValue();
                }
                if (row[1] instanceof Number throughput) {
                    totalRecommendedThroughput = throughput.longValue();
                }
            }
        }

        // 4. Time slot capacity and reserved counts
        long totalConfiguredSlotCapacity = 0L;
        long totalReservedSlots = 0L;
        List<Object[]> slotRows = timeSlotRepository.sumCapacityAndReservedCount(
                campaignId, centerId, SessionLifecycleStatus.CANCELLED
        );
        if (slotRows != null && !slotRows.isEmpty()) {
            Object[] row = slotRows.get(0);
            if (row != null && row.length >= 2) {
                if (row[0] instanceof Number capacity) {
                    totalConfiguredSlotCapacity = capacity.longValue();
                }
                if (row[1] instanceof Number reserved) {
                    totalReservedSlots = reserved.longValue();
                }
            }
        }

        long totalRemainingSlotCapacity = Math.max(0L, totalConfiguredSlotCapacity - totalReservedSlots);
        double slotUtilizationPercentage = 0.0;
        if (totalConfiguredSlotCapacity > 0) {
            double rawUtil = ((double) totalReservedSlots / totalConfiguredSlotCapacity) * 100.0;
            slotUtilizationPercentage = Math.round(rawUtil * 100.0) / 100.0;
        }

        // 5. Cancellations
        long cancelledSessionCount = sessionRepository.countCancelledSessions(
                campaignId, centerId, SessionLifecycleStatus.CANCELLED
        );
        long cancelledCampaignCount = (centerId != null)
                ? sessionRepository.countDistinctCancelledCampaignsForCenter(centerId, campaignId, CampaignStatus.CANCELLED)
                : campaignRepository.countCancelledCampaigns(campaignId, CampaignStatus.CANCELLED);

        // 6. Resource load
        long assignedStationCount = resourceAssignmentRepository.countAssignedStations(
                campaignId, centerId, ResourceType.VACCINATION_STATION,
                OperationalResourceStatus.ACTIVE, SessionLifecycleStatus.CANCELLED
        );
        long assignedVaccinatorCount = staffAssignmentRepository.countAssignedVaccinators(
                campaignId, centerId, StaffRole.VACCINATOR,
                OperationalStaffStatus.ACTIVE, SessionLifecycleStatus.CANCELLED
        );

        Map<ResourceType, Long> assignedResourceCountsByType = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            assignedResourceCountsByType.put(type, 0L);
        }
        List<Object[]> resourceTypeRows = resourceAssignmentRepository.countAssignedResourcesByType(
                campaignId, centerId, OperationalResourceStatus.ACTIVE, SessionLifecycleStatus.CANCELLED
        );
        for (Object[] row : resourceTypeRows) {
            if (row != null && row.length >= 2 && row[0] instanceof ResourceType type && row[1] instanceof Number count) {
                assignedResourceCountsByType.put(type, count.longValue());
            }
        }

        return new OperationalAnalyticsResponse(
                campaignCountByStatus,
                sessionCountByLifecycleStatus,
                totalApprovedCapacity,
                totalRecommendedThroughput,
                totalConfiguredSlotCapacity,
                totalReservedSlots,
                totalRemainingSlotCapacity,
                slotUtilizationPercentage,
                cancelledSessionCount,
                cancelledCampaignCount,
                assignedStationCount,
                assignedVaccinatorCount,
                assignedResourceCountsByType,
                Instant.now()
        );
    }
}
