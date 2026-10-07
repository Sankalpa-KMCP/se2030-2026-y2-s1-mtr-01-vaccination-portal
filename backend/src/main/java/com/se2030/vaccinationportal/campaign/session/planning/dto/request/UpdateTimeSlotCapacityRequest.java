package com.se2030.vaccinationportal.campaign.session.planning.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating the bookable capacity of an existing time slot.
 */
public record UpdateTimeSlotCapacityRequest(
        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity
) {
}
