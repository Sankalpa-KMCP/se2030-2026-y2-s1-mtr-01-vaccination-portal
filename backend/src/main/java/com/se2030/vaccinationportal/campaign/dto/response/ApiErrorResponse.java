package com.se2030.vaccinationportal.campaign.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Standard structured API error response.
 */
public record ApiErrorResponse(
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
