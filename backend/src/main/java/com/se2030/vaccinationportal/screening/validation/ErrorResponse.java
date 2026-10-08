package com.se2030.vaccinationportal.screening.validation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        Map<String, List<String>> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, Instant.now(), null);
    }

    public static ErrorResponse of(int status, String error, String message, Map<String, List<String>> fieldErrors) {
        return new ErrorResponse(status, error, message, Instant.now(), fieldErrors);
    }
}
