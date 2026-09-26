package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Builds the delimited authorized personalized context block for system prompt injection.
 * Coordinates deterministic tool selection, safe logging, and privacy boundary enforcement.
 */
@Component
public class AssistantPersonalizedContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(AssistantPersonalizedContextBuilder.class);

    public static final String DELIMITER_START = "=== AUTHORIZED READ-ONLY PERSONALIZED CONTEXT ===";
    public static final String DELIMITER_END = "=== END AUTHORIZED READ-ONLY PERSONALIZED CONTEXT ===";

    private final AssistantToolRegistry toolRegistry;

    public AssistantPersonalizedContextBuilder(AssistantToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public Optional<String> buildPersonalizedContext(String userMessage, AssistantToolContext context) {
        return buildPersonalizedResult(userMessage, context)
                .map(AssistantPersonalizedContextResult::promptContext);
    }

    public Optional<AssistantPersonalizedContextResult> buildPersonalizedResult(
            String userMessage,
            AssistantToolContext context
    ) {
        if (userMessage == null || userMessage.isBlank() || context == null || !context.isCitizen()) {
            return Optional.empty();
        }

        Optional<AssistantReadOnlyTool> matchingToolOpt = toolRegistry.findMatchingTool(userMessage);
        if (matchingToolOpt.isEmpty()) {
            return Optional.empty();
        }

        AssistantReadOnlyTool tool = matchingToolOpt.get();
        long startTime = System.currentTimeMillis();
        AssistantToolResult result;

        try {
            result = tool.execute(context, userMessage);
            long latencyMs = System.currentTimeMillis() - startTime;
            // Privacy-conscious metric log: strictly excludes question, SQL, and personalized record details
            log.info("Personalized tool executed: requestId={}, toolId={}, success={}, resultCount={}, latencyMs={}",
                    context.requestId(), tool.getToolId(), result.success(), result.resultCount(), latencyMs);
        } catch (Exception ex) {
            long latencyMs = System.currentTimeMillis() - startTime;
            // Never log database stack traces, SQL syntax, or user parameters
            log.warn("Personalized tool execution failed: requestId={}, toolId={}, success=false, latencyMs={}",
                    context.requestId(), tool.getToolId(), latencyMs);
            result = AssistantToolResult.unavailable(tool.getToolId(), List.of());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(DELIMITER_START).append("\n");
        sb.append("Tool: ").append(result.toolId().name()).append("\n");
        sb.append("The following bounded data belongs to the authenticated citizen represented by the verified server principal.\n");
        sb.append(result.contextSnippet()).append("\n\n");

        List<String> authorizedRoutes = result.suggestedRoutes().stream()
                .filter(route -> AssistantRouteCatalog.isAuthorizedRoute(route, context.authorities()))
                .toList();

        if (!authorizedRoutes.isEmpty()) {
            sb.append("Authorized Navigation: ").append(String.join(", ", authorizedRoutes)).append("\n");
        }

        sb.append("PERSONALIZED CONTEXT RULES:\n");
        sb.append("1. Answer using only the authorized personal record snapshot above and allowlisted navigation.\n");
        sb.append("2. This data is strictly read-only. Never claim or perform data mutations.\n");
        sb.append("3. Absence of records must not be interpreted as ineligibility, cancellation, or a medical conclusion.\n");
        sb.append("4. Adhere strictly to authorized portal navigation routes.\n");
        sb.append(DELIMITER_END);

        return Optional.of(new AssistantPersonalizedContextResult(sb.toString(), authorizedRoutes));
    }
}
