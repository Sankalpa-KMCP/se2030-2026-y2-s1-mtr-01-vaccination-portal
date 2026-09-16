package com.se2030.vaccinationportal.appointment.dto;

/**
 * Request DTO for cancelling an existing vaccination appointment.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Cancel an appointment (citizen self-service or staff override).</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-04: Citizen self-cancellation allowed until 24 hours prior to slot start;
 *       within 24 hours requires staff override with mandatory reason.</li>
 *   <li>MF03-BR-11: COMPLETED appointments cannot be cancelled.</li>
 * </ul>
 */
public record AppointmentCancellationRequest(
        Long requestingCitizenId,
        Boolean isStaffOverride,
        String staffReason
) {
}
