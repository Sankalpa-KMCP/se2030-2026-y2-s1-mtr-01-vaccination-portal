package com.se2030.vaccinationportal.vaccination.dto;

import java.time.Instant;
import java.util.List;

/**
 * Structured error payload for MF-05 vaccination endpoints.
 */
public record VaccinationApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorItem> fieldErrors
) {
    public record FieldErrorItem(String field, String message) {
    }
}
