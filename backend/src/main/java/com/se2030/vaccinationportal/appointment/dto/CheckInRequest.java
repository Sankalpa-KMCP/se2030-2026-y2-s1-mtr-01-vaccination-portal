package com.se2030.vaccinationportal.appointment.dto;

/**
 * DTO for appointment check-in request.
 */
public record CheckInRequest(
        Boolean isStaffOverride,
        String staffReason
) {
}
