package com.se2030.vaccinationportal.appointment.booking.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for rescheduling an existing vaccination appointment to a new time slot.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Reschedule an appointment slot.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-04: Citizen self-rescheduling allowed until 24 hours prior to slot start;
 *       within 24 hours requires staff override with mandatory reason.</li>
 *   <li>MF03-BR-05: Atomic release of old slot and reservation of new slot in one transaction.</li>
 *   <li>MF03-BR-11: COMPLETED appointments cannot be rescheduled.</li>
 * </ul>
 */
public record AppointmentRescheduleRequest(
        Long requestingCitizenId,
        @NotNull(message = "New slot ID is required") Long newSlotId,
        Boolean isStaffOverride,
        String staffReason
) {
}
