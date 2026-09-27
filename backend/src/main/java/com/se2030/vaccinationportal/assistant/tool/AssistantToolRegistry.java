package com.se2030.vaccinationportal.assistant.tool;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry holding available server-side assistant read-only tools and matching user intents deterministically.
 */
@Component
public class AssistantToolRegistry {

    private final List<AssistantReadOnlyTool> tools;

    public AssistantToolRegistry(List<AssistantReadOnlyTool> tools) {
        if (tools == null || tools.isEmpty()) {
            this.tools = Collections.emptyList();
        } else {
            List<AssistantReadOnlyTool> sorted = tools.stream()
                    .filter(Objects::nonNull)
                    .filter(t -> t.getToolId() != null)
                    .sorted(Comparator.comparing(AssistantReadOnlyTool::getToolId))
                    .toList();
            this.tools = List.copyOf(sorted);
        }
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

    public Optional<AssistantReadOnlyTool> findMatchingTool(
            String userMessage,
            AssistantToolContext context
    ) {
        if (userMessage == null || userMessage.isBlank() || context == null) {
            return Optional.empty();
        }
        return tools.stream()
                .filter(tool -> tool.supports(context))
                .filter(tool -> tool.matchesIntent(userMessage))
                .findFirst();
    }

    public boolean hasToolsAvailable() {
        return !tools.isEmpty();
    }

    public boolean hasAuthorizedTool(AssistantToolContext context) {
        return context != null && tools.stream().anyMatch(tool -> tool.supports(context));
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
