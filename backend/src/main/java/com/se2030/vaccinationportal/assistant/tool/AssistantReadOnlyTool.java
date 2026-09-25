package com.se2030.vaccinationportal.assistant.tool;

/**
 * Interface contract for server-side authorized read-only assistant tools.
 */
public interface AssistantReadOnlyTool {

    AssistantToolId getToolId();

    boolean matchesIntent(String userMessage);

    AssistantToolResult execute(AssistantToolContext context);
}
