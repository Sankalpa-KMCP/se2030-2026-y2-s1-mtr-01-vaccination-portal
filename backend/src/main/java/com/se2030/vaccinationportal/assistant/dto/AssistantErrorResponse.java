package com.se2030.vaccinationportal.assistant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response for Assistant API endpoints.
 * Never includes upstream stack traces, provider details, or internal prompts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssistantErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, List<String>> fieldErrors
) {
    public static AssistantErrorResponse of(int status, String error, String code, String message, String path) {
        return new AssistantErrorResponse(Instant.now(), status, error, code, message, path, null);
    }

    public static AssistantErrorResponse ofValidation(
            int status,
            String error,
            String code,
            String message,
            String path,
            Map<String, List<String>> fieldErrors
    ) {
        return new AssistantErrorResponse(Instant.now(), status, error, code, message, path, fieldErrors);
    }
}
