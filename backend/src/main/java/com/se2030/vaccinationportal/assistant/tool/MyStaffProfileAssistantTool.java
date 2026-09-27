package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Returns a bounded profile and center-roster summary for the authenticated staff account.
 * Identity is always taken from the verified server principal; user-supplied IDs are ignored.
 */
@Component
public class MyStaffProfileAssistantTool implements AssistantReadOnlyTool {

    private static final int MAX_ROSTER_ENTRIES = 10;

    private static final List<Pattern> INTENTS = List.of(
            Pattern.compile("\\b(my|show|view|check|what(?:'s|\\s+is))\\s+(staff\\s+)?(profile|id|account|role|roles|permissions?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(where\\s+am\\s+i|am\\s+i)\\s+(assigned|working)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwhich\\s+cent(?:er|re)\\s+am\\s+i\\s+assigned\\s+to\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(my|show)\\s+(assigned\\s+)?(center|centers|workplace)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bstaff\\s*(id|number)\\s*[:=#-]?\\s*\\d+\\b", Pattern.CASE_INSENSITIVE)
    );

    private final OperationalStaffRosterRepository rosterRepository;

    public MyStaffProfileAssistantTool(OperationalStaffRosterRepository rosterRepository) {
        this.rosterRepository = rosterRepository;
    }

    @Override
    public AssistantToolId getToolId() {
        return AssistantToolId.MY_STAFF_PROFILE_AND_ASSIGNMENTS;
    }

    @Override
    public boolean matchesIntent(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        return INTENTS.stream().anyMatch(pattern -> pattern.matcher(userMessage.trim()).find());
    }

    @Override
    public boolean supports(AssistantToolContext context) {
        return context != null && context.isStaff();
    }

    @Override
    @Transactional(readOnly = true)
    public AssistantToolResult execute(AssistantToolContext context) {
        if (!supports(context)) {
            throw new IllegalArgumentException("Staff profile tools require an authenticated staff principal.");
        }

        Long verifiedUserId = context.getUserId();
        List<OperationalStaffRoster> rosterEntries =
                rosterRepository.findByUserIdOrderByCenterIdAsc(verifiedUserId);

        List<String> sortedAuthorities = context.authorities().stream().sorted().toList();
        StringBuilder summary = new StringBuilder()
                .append("Verified staff account ID: ").append(verifiedUserId).append(". ")
                .append("Authenticated portal roles: ").append(String.join(", ", sortedAuthorities)).append(". ");

        if (rosterEntries.isEmpty()) {
            summary.append("No vaccination-center roster assignment is currently linked to this staff account. ")
                    .append("Ask an administrator or center manager to verify the staff roster association.");
        } else {
            summary.append("Center roster assignments: ");
            rosterEntries.stream().limit(MAX_ROSTER_ENTRIES).forEach(entry -> summary
                    .append("[Center code: ").append(entry.getCenter().getCode())
                    .append(", name: ").append(entry.getCenter().getName())
                    .append(", operational role: ").append(entry.getRole())
                    .append(", roster status: ").append(entry.getStatus())
                    .append("] "));
            if (rosterEntries.size() > MAX_ROSTER_ENTRIES) {
                summary.append("Only the first ").append(MAX_ROSTER_ENTRIES)
                        .append(" assignments are included in this bounded snapshot. ");
            }
        }

        summary.append("Any staff ID written in the chat message was ignored; this result uses only the signed-in account identity.");

        List<String> suggestedRoutes = new ArrayList<>();
        if (AssistantRouteCatalog.isAuthorizedRoute("/app/staff", context.authorities())) {
            suggestedRoutes.add("/app/staff");
        }
        if (AssistantRouteCatalog.isAuthorizedRoute("/app/center-operations", context.authorities())) {
            suggestedRoutes.add("/app/center-operations");
        }
        if (AssistantRouteCatalog.isAuthorizedRoute("/app/sessions", context.authorities())) {
            suggestedRoutes.add("/app/sessions");
        }

        return new AssistantToolResult(
                AssistantToolId.MY_STAFF_PROFILE_AND_ASSIGNMENTS,
                true,
                rosterEntries.size(),
                summary.toString(),
                suggestedRoutes
        );
    }
}
