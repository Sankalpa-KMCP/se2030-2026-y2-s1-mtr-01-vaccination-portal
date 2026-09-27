package com.se2030.vaccinationportal.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Clean, safe chat response returned to the client.
 * Strictly excludes provider names, models, reasoning, tools executed, or internal prompts.
 */
public record AssistantChatResponse(
        @JsonProperty("answer")
        String answer,

        @JsonProperty("suggestedActions")
        List<AssistantActionDto> suggestedActions,

        @JsonProperty("requestId")
        String requestId
) {
    public AssistantChatResponse {
        if (suggestedActions == null) {
            suggestedActions = Collections.emptyList();
        } else {
            suggestedActions = List.copyOf(suggestedActions);
        }
    }
}
