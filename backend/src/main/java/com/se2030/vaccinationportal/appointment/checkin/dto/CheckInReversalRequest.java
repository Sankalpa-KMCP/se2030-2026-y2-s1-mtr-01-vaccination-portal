package com.se2030.vaccinationportal.appointment.checkin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Required audit explanation for an authorized check-in reversal. */
public record CheckInReversalRequest(
        @NotBlank(message = "Reversal reason is required")
        @Size(max = 500, message = "Reversal reason must not exceed 500 characters")
        String reason
) {
}
