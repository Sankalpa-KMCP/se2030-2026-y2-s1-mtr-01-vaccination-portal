package com.se2030.vaccinationportal.assistant.tool;

/**
 * Interface contract for server-side authorized read-only assistant tools.
 */
public interface AssistantReadOnlyTool {

    AssistantToolId getToolId();

    boolean matchesIntent(String userMessage);

    /**
     * Citizen ownership is the safe default for the original personalized tools.
     * Tools for other actor types must opt in explicitly.
     */
    default boolean supports(AssistantToolContext context) {
        return context != null && context.isCitizen();
    }

    AssistantToolResult execute(AssistantToolContext context);

    /**
     * Executes a tool with the matched user message available for intent-specific filtering.
     * Existing tools retain their original behavior through the default implementation.
     */
    default AssistantToolResult execute(AssistantToolContext context, String userMessage) {
        return execute(context);
    }
}
