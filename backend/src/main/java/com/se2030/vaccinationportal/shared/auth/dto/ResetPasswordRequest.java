package com.se2030.vaccinationportal.shared.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Token must not be blank")
        String token,

        @NotBlank(message = "New password must not be blank")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String newPassword
) {
    public ResetPasswordRequest {
        token = trimNullable(token);
    }

    private static String trimNullable(String value) {
        return value == null ? null : value.trim();
    }
}
