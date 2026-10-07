package com.se2030.vaccinationportal.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Suggested navigation action recommended by the assistant.
 * Must only link to verified internal application routes.
 */
public record AssistantActionDto(
        @JsonProperty("label")
        @NotBlank
        String label,

        @JsonProperty("path")
        @NotBlank
        String path
) {}
