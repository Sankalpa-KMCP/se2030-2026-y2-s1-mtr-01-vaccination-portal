package com.se2030.vaccinationportal.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Public status response for the AI Assistant.
 * Exposes service availability and disclaimer without leaking internal providers, models, or credentials.
 */
public record AssistantStatusResponse(
        @JsonProperty("available")
        boolean available,

        @JsonProperty("personalizedFeaturesAvailable")
        boolean personalizedFeaturesAvailable,

        @JsonProperty("disclaimer")
        String disclaimer
) {
    public static final String DEFAULT_DISCLAIMER =
            "This assistant explains portal services. It does not provide medical advice or emergency care.";

    public static AssistantStatusResponse of(boolean available) {
        return new AssistantStatusResponse(available, false, DEFAULT_DISCLAIMER);
    }

    public static AssistantStatusResponse of(boolean available, boolean personalizedFeaturesAvailable) {
        return new AssistantStatusResponse(available, personalizedFeaturesAvailable, DEFAULT_DISCLAIMER);
    }
}
