package com.se2030.vaccinationportal.shared.auth.dto;

public record LoginResponse(
        String token,
        UserProfileResponse user
) {}
