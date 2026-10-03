package com.se2030.vaccinationportal.shared.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailOtpRequest(
        @NotBlank(message = "Verification code must not be blank")
        String otp
) {
    public VerifyEmailOtpRequest {
        otp = trimNullable(otp);
    }

    private static String trimNullable(String value) {
        return value == null ? null : value.trim();
    }
}
