package com.se2030.vaccinationportal.shared.auth.dto;

import com.se2030.vaccinationportal.shared.auth.model.CitizenGender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateCitizenProfileRequest(
        @NotBlank(message = "Full name must not be blank")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @NotBlank(message = "Phone number must not be blank")
        @Size(max = 32, message = "Phone number must not exceed 32 characters")
        String phoneNumber,

        @PastOrPresent(message = "Date of birth must not be in the future")
        LocalDate dateOfBirth,

        CitizenGender gender,

        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address
) {
    public UpdateCitizenProfileRequest {
        fullName = trimNullable(fullName);
        phoneNumber = trimNullable(phoneNumber);
        address = trimToNull(address);
    }

    private static String trimNullable(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trimNullable(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
