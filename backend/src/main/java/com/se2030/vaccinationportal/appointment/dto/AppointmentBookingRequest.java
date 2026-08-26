package com.se2030.vaccinationportal.appointment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for appointment booking request.
 */
public record AppointmentBookingRequest(
        @NotNull(message = "Citizen ID is required") Long citizenId,
        @NotNull(message = "Campaign ID is required") Long campaignId,
        @NotNull(message = "Slot ID is required") Long slotId
) {
}
