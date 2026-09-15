package com.se2030.vaccinationportal.appointment.dto;

/**
 * DTO for appointment cancellation request.
 */
public record AppointmentCancellationRequest(
        Long requestingCitizenId,
        Boolean isStaffOverride,
        String staffReason
) {
}
