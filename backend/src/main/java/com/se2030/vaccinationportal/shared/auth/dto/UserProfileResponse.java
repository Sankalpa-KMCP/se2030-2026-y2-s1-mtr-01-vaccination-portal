package com.se2030.vaccinationportal.shared.auth.dto;

import java.util.List;

public record UserProfileResponse(
        Long id,
        String email,
        List<String> roles
) {}
