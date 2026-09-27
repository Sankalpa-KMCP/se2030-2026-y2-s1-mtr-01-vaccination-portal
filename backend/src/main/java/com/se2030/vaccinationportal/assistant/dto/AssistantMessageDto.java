package com.se2030.vaccinationportal.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Message history item exchanged with the assistant.
 * Roles are strictly restricted to 'user' or 'assistant'.
 */
public record AssistantMessageDto(
        @JsonProperty("role")
        @NotBlank(message = "History message role is required")
        @Pattern(regexp = "^(user|assistant)$", message = "Role must be 'user' or 'assistant'")
        String role,

        @JsonProperty("content")
        @NotBlank(message = "History message content cannot be blank")
        @Size(max = 1500, message = "History message content exceeds maximum allowed characters")
        String content
) {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    public AssistantMessageDto {
        if (role != null) {
            role = role.trim().toLowerCase();
        }
        if (content != null) {
            content = content.trim();
        }
    }
}
