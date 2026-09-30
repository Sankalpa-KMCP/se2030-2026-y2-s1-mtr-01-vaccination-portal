package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import com.se2030.vaccinationportal.campaign.api.staff.StaffContextQuery;
import com.se2030.vaccinationportal.campaign.api.staff.StaffScheduleSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Read-only current and upcoming session schedule for the authenticated staff account.
 */
@Component
public class MyStaffScheduleAssistantTool implements AssistantReadOnlyTool {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter
            .ofPattern("dd MMM uuuu, hh:mm a z", Locale.ENGLISH);
    private static final Pattern TODAY_INTENT = Pattern.compile("\\btoday(?:'s)?\\b", Pattern.CASE_INSENSITIVE);
    private static final List<Pattern> INTENTS = List.of(
            Pattern.compile("\\b(my|show|view|what(?:'s|\\s+is))\\s+(work\\s+)?(schedule|shift|shifts|assignments?|sessions?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwhat\\s+(should|do)\\s+i\\s+do\\s+today\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(my|show)\\s+(today(?:'s)?|upcoming|next)\\s+(work|shift|shifts|assignments?|sessions?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwhen\\s+(does|is)\\s+my\\s+next\\s+(shift|assignment|session)\\b", Pattern.CASE_INSENSITIVE)
    );

    private final StaffContextQuery staffQuery;
    private final Clock clock;

    @Autowired
    public MyStaffScheduleAssistantTool(StaffContextQuery staffQuery) {
        this(staffQuery, Clock.systemUTC());
    }

    MyStaffScheduleAssistantTool(StaffContextQuery staffQuery, Clock clock) {
        this.staffQuery = staffQuery;
        this.clock = clock;
    }

    @Override
    public AssistantToolId getToolId() {
        return AssistantToolId.MY_STAFF_SCHEDULE;
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
        return execute(context, "");
    }

    @Override
    @Transactional(readOnly = true)
    public AssistantToolResult execute(AssistantToolContext context, String userMessage) {
        if (!supports(context)) {
            throw new IllegalArgumentException("Staff schedule tools require an authenticated staff principal.");
        }

        Instant now = clock.instant();
        boolean todayOnly = userMessage != null && TODAY_INTENT.matcher(userMessage).find();
        List<StaffScheduleSummary> assignments = staffQuery.scheduleForUser(context.principal(), now, todayOnly);

        List<String> suggestedRoutes = new ArrayList<>();
        if (AssistantRouteCatalog.isAuthorizedRoute("/app/staff", context.authorities())) {
            suggestedRoutes.add("/app/staff");
        }
        if (AssistantRouteCatalog.isAuthorizedRoute("/app/sessions", context.authorities())) {
            suggestedRoutes.add("/app/sessions");
        }
        if (assignments.isEmpty()) {
            String period = todayOnly ? "today" : "current or upcoming";
            return AssistantToolResult.empty(
                    AssistantToolId.MY_STAFF_SCHEDULE,
                    "No " + period + " session assignment was found for the verified signed-in staff account. "
                            + "This does not change the staff member's account or center-roster status.",
                    suggestedRoutes
            );
        }

        StringBuilder summary = new StringBuilder(todayOnly
                ? "Today's verified session assignments: "
                : "Current and upcoming verified session assignments: ");
        for (StaffScheduleSummary assignment : assignments) {
            ZoneId zone = ZoneId.of(assignment.timezone());
            summary.append("[Session ID: ").append(assignment.sessionId())
                    .append(", campaign: ").append(assignment.campaignCode())
                    .append(" - ").append(assignment.campaignName())
                    .append(", center: ").append(assignment.centerCode())
                    .append(" - ").append(assignment.centerName())
                    .append(", assigned role: ").append(assignment.role())
                    .append(", status: ").append(assignment.sessionStatus())
                    .append(", starts: ").append(DISPLAY_TIME.format(assignment.startAt().atZone(zone)))
                    .append(", ends: ").append(DISPLAY_TIME.format(assignment.endAt().atZone(zone)))
                    .append(", timezone: ").append(zone.getId())
                    .append("] ");
        }
        summary.append("This schedule is read-only and belongs only to the verified signed-in staff account.");

        return new AssistantToolResult(
                AssistantToolId.MY_STAFF_SCHEDULE,
                true,
                assignments.size(),
                summary.toString(),
                suggestedRoutes
        );
    }

}
