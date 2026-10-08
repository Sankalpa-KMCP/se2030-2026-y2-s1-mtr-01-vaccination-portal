package com.se2030.vaccinationportal.shared.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffRejectionRequest(
        @NotBlank(message = "Rejection reason must not be blank")
        @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
        String reason
) {
    public StaffRejectionRequest {
        reason = reason == null ? null : reason.trim();
    }
}
