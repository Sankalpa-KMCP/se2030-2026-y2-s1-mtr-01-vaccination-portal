package com.se2030.vaccinationportal.appointment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for joining a session waitlist.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-05: Join a waitlist when a session is full.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-01: Citizen must have a valid ELIGIBLE assessment for this campaign.</li>
 *   <li>MF03-BR-06: Session must have no available booking capacity across all its slots.</li>
 *   <li>MF03-BR-14: Citizen cannot hold an active WAITING entry while already holding an active appointment.</li>
 * </ul>
 */
public record WaitlistJoinRequest(
        @NotNull(message = "Citizen ID is required") Long citizenId,
        @NotNull(message = "Campaign ID is required") Long campaignId,
        @NotNull(message = "Session ID is required") Long sessionId
) {
}
