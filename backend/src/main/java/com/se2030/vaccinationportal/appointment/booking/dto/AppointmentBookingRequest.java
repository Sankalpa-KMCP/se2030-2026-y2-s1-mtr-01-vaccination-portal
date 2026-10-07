package com.se2030.vaccinationportal.appointment.booking.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for booking a vaccination appointment.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Search and book an available appointment slot.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-01: Citizen must have an active ELIGIBLE screening assessment for this campaign.</li>
 *   <li>MF03-BR-02: Citizen must not already have an active appointment for this campaign.</li>
 *   <li>MF03-BR-03: Capacity allocation on the time slot must be atomic.</li>
 * </ul>
 */
public record AppointmentBookingRequest(
        @NotNull(message = "Citizen ID is required") Long citizenId,
        @NotNull(message = "Campaign ID is required") Long campaignId,
        @NotNull(message = "Slot ID is required") Long slotId
) {
}
