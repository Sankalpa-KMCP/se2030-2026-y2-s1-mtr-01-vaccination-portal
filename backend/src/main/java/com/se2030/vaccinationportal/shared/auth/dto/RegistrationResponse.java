package com.se2030.vaccinationportal.shared.auth.dto;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;

import java.time.Instant;
import java.util.List;

public record RegistrationResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        AccountStatus accountStatus,
        List<String> roles,
        String requestedRoleCode,
        Instant createdAt
) {}
