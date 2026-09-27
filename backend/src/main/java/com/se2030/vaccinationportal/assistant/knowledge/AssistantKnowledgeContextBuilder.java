package com.se2030.vaccinationportal.assistant.knowledge;

import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;

import java.util.List;
import java.util.Set;

/**
 * Formats retrieved knowledge entries into a clearly delimited context block
 * for injection into the assistant's system instructions.
 */
public final class AssistantKnowledgeContextBuilder {

    private AssistantKnowledgeContextBuilder() {
        // utility
    }

    public static final String DELIMITER_START = "=== VERIFIED PORTAL KNOWLEDGE ===";
    public static final String DELIMITER_END = "=== END VERIFIED PORTAL KNOWLEDGE ===";

    /**
     * Builds the formatted knowledge block for system prompt injection.
     *
     * @param entries List of verified knowledge entries relevant to the caller's query
     * @param callerAuthorities Caller authorities used to filter visible routes
     * @return Formatted knowledge instruction string
     */
    public static String buildKnowledgeContext(
            List<AssistantKnowledgeEntry> entries,
            Set<String> callerAuthorities
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(DELIMITER_START).append("\n");

        if (entries == null || entries.isEmpty()) {
            sb.append("No specific knowledge catalogue topics were retrieved for this inquiry.\n");
            sb.append("Rely strictly on portal workflow guidelines and allowlisted routes. If specific information is unavailable, admit uncertainty clearly and guide the user to the appropriate portal area.\n");
        } else {
            for (AssistantKnowledgeEntry entry : entries) {
                sb.append("[Topic: ").append(entry.title()).append("] (Module: ").append(entry.module()).append(")\n");
                sb.append(entry.explanation()).append("\n");

                List<String> authorizedRoutes = entry.relevantRoutes().stream()
                        .filter(route -> AssistantRouteCatalog.isAuthorizedRoute(route, callerAuthorities))
                        .toList();

                if (!authorizedRoutes.isEmpty()) {
                    sb.append("Authorized Navigation: ").append(String.join(", ", authorizedRoutes)).append("\n");
                }
                sb.append("\n");
            }

            sb.append("KNOWLEDGE UTILIZATION RULES:\n");
            sb.append("1. Answer portal questions using only the verified operational facts above and allowlisted route metadata.\n");
            sb.append("2. If the catalogue does not contain sufficient details for the inquiry, state that the information is currently unavailable and suggest portal navigation.\n");
            sb.append("3. Final clinical eligibility is determined solely by an authorized Healthcare Officer. Never infer or determine eligibility.\n");
            sb.append("4. Never disclose Java file paths, internal class names, database tables, or developer evidence.\n");
            sb.append("5. Never invent vaccination schedules, clinical timing rules, contraindications, or brand recommendations.\n");
        }

        sb.append(DELIMITER_END);
        return sb.toString();
    }
}
