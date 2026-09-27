package com.se2030.vaccinationportal.assistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.se2030.vaccinationportal.assistant.policy.SafeRoute;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.List;

/**
 * Incoming chat request payload.
 * Strictly prevents client-supplied identity, authorization, or role claims.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record AssistantChatRequest(
        @JsonProperty("message")
        @NotBlank(message = "Message is required and cannot be blank")
        @Size(max = 1500, message = "Message exceeds the maximum limit of 1500 characters")
        String message,

        @JsonProperty("history")
        @Size(max = 10, message = "History cannot contain more than 10 messages")
        List<@Valid AssistantMessageDto> history,

        @JsonProperty("currentPath")
        @SafeRoute
        String currentPath
) {
    public AssistantChatRequest {
        if (message != null) {
            message = message.trim();
        }
        if (history == null) {
            history = Collections.emptyList();
        } else {
            history = List.copyOf(history);
        }
        if (currentPath != null) {
            currentPath = currentPath.trim();
        }
    }

    public AssistantChatRequest(String message, List<AssistantMessageDto> history) {
        this(message, history, null);
    }
}
