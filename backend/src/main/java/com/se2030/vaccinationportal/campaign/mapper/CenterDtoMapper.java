package com.se2030.vaccinationportal.campaign.mapper;

import com.se2030.vaccinationportal.campaign.dto.response.CenterClosureResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CenterResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CenterSummaryResponse;
import com.se2030.vaccinationportal.campaign.dto.response.OperatingHoursResponse;
import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;

/**
 * Pure transformation utility mapping Center, OperatingHours, and CenterClosure domain models to REST response DTOs.
 */
public final class CenterDtoMapper {

    private CenterDtoMapper() {
    }

    public static CenterResponse toResponse(VaccinationCenter center) {
        if (center == null) {
            return null;
        }
        return new CenterResponse(
                center.getId(),
                center.getCode(),
                center.getName(),
                center.getAddress(),
                center.getContact(),
                center.getTimezone(),
                center.getCenterStatus(),
                center.getSafeConcurrentOccupancy(),
                center.getSessionServiceCeiling(),
                center.getCreatedAt(),
                center.getUpdatedAt()
        );
    }

    public static CenterSummaryResponse toSummaryResponse(VaccinationCenter center) {
        if (center == null) {
            return null;
        }
        return new CenterSummaryResponse(
                center.getId(),
                center.getCode(),
                center.getName(),
                center.getCenterStatus(),
                center.getTimezone()
        );
    }

    public static OperatingHoursResponse toResponse(OperatingHours operatingHours) {
        if (operatingHours == null) {
            return null;
        }
        Long centerId = operatingHours.getCenter() != null ? operatingHours.getCenter().getId() : null;
        return new OperatingHoursResponse(
                operatingHours.getId(),
                centerId,
                operatingHours.getDayOfWeek(),
                operatingHours.getOpening(),
                operatingHours.getClosing(),
                operatingHours.getEffectiveFrom(),
                operatingHours.getEffectiveTo(),
                operatingHours.getCreatedAt(),
                operatingHours.getUpdatedAt()
        );
    }

    public static CenterClosureResponse toResponse(CenterClosure centerClosure) {
        if (centerClosure == null) {
            return null;
        }
        Long centerId = centerClosure.getCenter() != null ? centerClosure.getCenter().getId() : null;
        return new CenterClosureResponse(
                centerClosure.getId(),
                centerId,
                centerClosure.getStartAt(),
                centerClosure.getEndAt(),
                centerClosure.getReason(),
                centerClosure.getStatus(),
                centerClosure.getCreatedAt(),
                centerClosure.getUpdatedAt()
        );
    }
}
