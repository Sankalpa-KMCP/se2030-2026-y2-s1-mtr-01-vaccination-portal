package com.se2030.vaccinationportal.appointment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for waitlist join request.
 */
public record WaitlistJoinRequest(
        @NotNull(message = "Citizen ID is required") Long citizenId,
        @NotNull(message = "Campaign ID is required") Long campaignId,
        @NotNull(message = "Session ID is required") Long sessionId
) {
}
