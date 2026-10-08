package com.se2030.vaccinationportal.assistant.tool;

import java.util.Collections;
import java.util.List;

/**
 * Immutable execution result produced by an authorized assistant read-only tool.
 */
public record AssistantToolResult(
        AssistantToolId toolId,
        boolean success,
        int resultCount,
        String contextSnippet,
        List<String> suggestedRoutes
) {
    public AssistantToolResult {
        if (suggestedRoutes == null) {
            suggestedRoutes = Collections.emptyList();
        }
    }

    public static AssistantToolResult empty(AssistantToolId toolId, String message, List<String> suggestedRoutes) {
        return new AssistantToolResult(toolId, true, 0, message, suggestedRoutes);
    }

    public static AssistantToolResult unavailable(AssistantToolId toolId, List<String> suggestedRoutes) {
        return new AssistantToolResult(
                toolId,
                false,
                0,
                "Personalized information is temporarily unavailable. Please use the portal navigation links.",
                suggestedRoutes
        );
    }
}
