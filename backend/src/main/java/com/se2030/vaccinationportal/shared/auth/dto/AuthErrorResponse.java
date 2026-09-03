package com.se2030.vaccinationportal.shared.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AuthErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, List<String>> fieldErrors
) {}
