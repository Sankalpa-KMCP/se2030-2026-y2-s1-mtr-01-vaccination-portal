package com.se2030.vaccinationportal.shared.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CitizenRegistrationRequest(
        @NotBlank(message = "Full name must not be blank")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a well-formed email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Phone number must not be blank")
        @Size(max = 32, message = "Phone number must not exceed 32 characters")
        String phoneNumber,

        @NotBlank(message = "Password must not be blank")
        @Size(max = 128, message = "Password must not exceed 128 characters")
        String password
) {
    public CitizenRegistrationRequest {
        fullName = trimNullable(fullName);
        email = trimNullable(email);
        phoneNumber = trimNullable(phoneNumber);
    }

    private static String trimNullable(String value) {
        return value == null ? null : value.trim();
    }
}
