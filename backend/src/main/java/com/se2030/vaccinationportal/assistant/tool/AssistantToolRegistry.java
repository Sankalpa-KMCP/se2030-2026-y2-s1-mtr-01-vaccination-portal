package com.se2030.vaccinationportal.assistant.tool;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Registry holding available server-side assistant read-only tools and matching user intents deterministically.
 */
@Component
public class AssistantToolRegistry {

    private final List<AssistantReadOnlyTool> tools;

    public AssistantToolRegistry(List<AssistantReadOnlyTool> tools) {
        this.tools = tools != null ? List.copyOf(tools) : Collections.emptyList();
    }

    public Optional<AssistantReadOnlyTool> findMatchingTool(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }
        for (AssistantReadOnlyTool tool : tools) {
            if (tool.matchesIntent(userMessage)) {
                return Optional.of(tool);
            }
        }
        return Optional.empty();
    }

    public boolean hasToolsAvailable() {
        return !tools.isEmpty();
    }

    public Optional<AssistantReadOnlyTool> getTool(AssistantToolId toolId) {
        return tools.stream()
                .filter(t -> t.getToolId() == toolId)
                .findFirst();
    }

    public List<AssistantReadOnlyTool> getTools() {
        return tools;
    }
}
