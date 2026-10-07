package com.se2030.vaccinationportal.shared.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendVerificationRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(
                regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
                message = "Enter a valid email address."
        )
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email
) {
    public ResendVerificationRequest {
        email = trimNullable(email);
    }

    private static String trimNullable(String value) {
        return value == null ? null : value.trim();
    }
}
