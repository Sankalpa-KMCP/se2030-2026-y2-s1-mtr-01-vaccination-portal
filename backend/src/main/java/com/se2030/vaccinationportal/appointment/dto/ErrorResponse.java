package com.se2030.vaccinationportal.appointment.dto;

import java.time.Instant;

/**
 * Standardized error response DTO for API error representation across MF-03 endpoints.
 * Conforms to RFC 7807 problem details pattern with HTTP status, machine error code, message, and timestamp.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, Instant.now());
    }
}
