package com.se2030.vaccinationportal.assistant.tool;

import java.util.List;

/**
 * Authorized personalized prompt context and its independently validated navigation routes.
 */
public record AssistantPersonalizedContextResult(
        String promptContext,
        List<String> authorizedRoutes
) {
    public AssistantPersonalizedContextResult {
        authorizedRoutes = authorizedRoutes == null ? List.of() : List.copyOf(authorizedRoutes);
    }
}
