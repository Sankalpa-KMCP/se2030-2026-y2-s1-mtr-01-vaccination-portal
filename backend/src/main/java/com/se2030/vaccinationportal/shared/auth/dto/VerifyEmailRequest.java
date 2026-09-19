package com.se2030.vaccinationportal.shared.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank(message = "Token must not be blank")
        String token
) {
    public VerifyEmailRequest {
        token = trimNullable(token);
    }

    private static String trimNullable(String value) {
        return value == null ? null : value.trim();
    }
}
