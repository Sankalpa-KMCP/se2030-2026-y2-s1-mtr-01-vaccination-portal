package com.se2030.vaccinationportal.shared.auth.dto;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.CitizenGender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Citizen self-service profile response. The {@code id} is the authenticated UserAccount ID.
 */
public record CitizenProfileResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        AccountStatus accountStatus,
        List<String> roles,
        Instant createdAt,
        LocalDate dateOfBirth,
        CitizenGender gender,
        String address,
        Instant updatedAt
) {}
