package com.se2030.vaccinationportal.campaign.session.planning.mapper;

import com.se2030.vaccinationportal.campaign.session.planning.dto.response.TimeSlotResponse;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;

/**
 * Mapper utility converting TimeSlot entity models to TimeSlotResponse DTO representations.
 */
public final class TimeSlotDtoMapper {

    private TimeSlotDtoMapper() {
    }

    public static TimeSlotResponse toResponse(TimeSlot slot) {
        if (slot == null) {
            return null;
        }
        return new TimeSlotResponse(
                slot.getId(),
                slot.getSession() != null ? slot.getSession().getId() : null,
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getCapacity(),
                slot.getReservedCount(),
                slot.getAvailableCapacity(),
                slot.getLockVersion(),
                slot.getCreatedAt(),
                slot.getUpdatedAt()
        );
    }
}
