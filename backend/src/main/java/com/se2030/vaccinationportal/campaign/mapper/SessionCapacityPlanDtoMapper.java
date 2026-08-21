package com.se2030.vaccinationportal.campaign.mapper;

import com.se2030.vaccinationportal.campaign.dto.response.SessionCapacityPlanResponse;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;

/**
 * Pure conversion mapper between SessionCapacityPlan model entities and REST DTOs.
 */
public final class SessionCapacityPlanDtoMapper {

    private SessionCapacityPlanDtoMapper() {
    }

    public static SessionCapacityPlanResponse toResponse(SessionCapacityPlan plan) {
        if (plan == null) {
            return null;
        }
        return new SessionCapacityPlanResponse(
                plan.getId(),
                plan.getSession() != null ? plan.getSession().getId() : null,
                plan.getRevision(),
                plan.getDurationMinutes(),
                plan.getExpectedServiceMinutes(),
                plan.getAssignedVaccinatorCount(),
                plan.getAvailableStationCount(),
                plan.getFullyEquippedStationCount(),
                plan.getEffectiveStationCount(),
                plan.getRecommendedThroughput(),
                plan.getApprovedCapacity(),
                plan.getSafeConcurrentOccupancy(),
                plan.getExpectedDwellMinutes(),
                plan.getExpectedAccompanyingPersonsPerBooking(),
                plan.getSessionServiceCeiling(),
                plan.getOverrideReason(),
                plan.getApprovedBy(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
