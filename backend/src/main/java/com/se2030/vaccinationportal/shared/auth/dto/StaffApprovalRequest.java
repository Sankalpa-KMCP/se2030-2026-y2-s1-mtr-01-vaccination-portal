package com.se2030.vaccinationportal.shared.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffApprovalRequest(
        @NotBlank(message = "Approved role code must not be blank")
        @Size(max = 64, message = "Approved role code must not exceed 64 characters")
        String roleCode
) {}
