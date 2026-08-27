package com.se2030.vaccinationportal.appointment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for appointment reschedule request.
 */
public record AppointmentRescheduleRequest(
        Long requestingCitizenId,
        @NotNull(message = "New slot ID is required") Long newSlotId,
        Boolean isStaffOverride,
        String staffReason
) {
}
