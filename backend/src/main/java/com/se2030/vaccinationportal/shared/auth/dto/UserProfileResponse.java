package com.se2030.vaccinationportal.shared.auth.dto;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;

import java.util.List;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        AccountStatus accountStatus,
        List<String> roles
) {}
