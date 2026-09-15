package com.se2030.vaccinationportal.shared.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CitizenRegistrationRequest(
        @NotBlank(message = "Full name must not be blank")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @NotBlank(message = "Email must not be blank")
        @Email(
                regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
                message = "Enter a valid email address."
        )
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Phone number must not be blank")
        @Pattern(
                regexp = "^[0-9]{10}$",
                message = "Phone number must contain exactly 10 digits."
        )
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
