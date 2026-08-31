package com.se2030.vaccinationportal.shared.auth.dto;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;

import java.time.Instant;
import java.util.List;

public record StaffRequestResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        String requestedRoleCode,
        AccountStatus accountStatus,
        List<String> roles,
        Instant createdAt,
        Instant reviewedAt,
        Long reviewedByUserId,
        String reviewedByEmail,
        String rejectionReason
) {}
