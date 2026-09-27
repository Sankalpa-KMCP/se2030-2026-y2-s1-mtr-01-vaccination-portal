package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Read-only current and upcoming session schedule for the authenticated staff account.
 */
@Component
public class MyStaffScheduleAssistantTool implements AssistantReadOnlyTool {

    private static final int MAX_ASSIGNMENTS = 10;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Colombo");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter
            .ofPattern("dd MMM uuuu, hh:mm a z", Locale.ENGLISH);
    private static final Pattern TODAY_INTENT = Pattern.compile("\\btoday(?:'s)?\\b", Pattern.CASE_INSENSITIVE);
    private static final List<Pattern> INTENTS = List.of(
            Pattern.compile("\\b(my|show|view|what(?:'s|\\s+is))\\s+(work\\s+)?(schedule|shift|shifts|assignments?|sessions?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwhat\\s+(should|do)\\s+i\\s+do\\s+today\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(my|show)\\s+(today(?:'s)?|upcoming|next)\\s+(work|shift|shifts|assignments?|sessions?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwhen\\s+(does|is)\\s+my\\s+next\\s+(shift|assignment|session)\\b", Pattern.CASE_INSENSITIVE)
    );

    private final StaffAssignmentRepository assignmentRepository;
    private final Clock clock;

    @Autowired
    public MyStaffScheduleAssistantTool(StaffAssignmentRepository assignmentRepository) {
        this(assignmentRepository, Clock.systemUTC());
    }

    MyStaffScheduleAssistantTool(StaffAssignmentRepository assignmentRepository, Clock clock) {
        this.assignmentRepository = assignmentRepository;
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
        List<StaffAssignment> assignments = assignmentRepository
                .findByUserIdOrderByStartAtAscIdAsc(context.getUserId())
                .stream()
                .filter(assignment -> isActiveAssignment(assignment, now, todayOnly))
                .sorted(Comparator.comparing(StaffAssignment::getStartAt))
                .limit(MAX_ASSIGNMENTS)
                .toList();

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
        for (StaffAssignment assignment : assignments) {
            VaccinationSession session = assignment.getSession();
            ZoneId zone = resolveZone(session);
            summary.append("[Session ID: ").append(session.getId())
                    .append(", campaign: ").append(session.getCampaign().getCode())
                    .append(" - ").append(session.getCampaign().getName())
                    .append(", center: ").append(session.getCenter().getCode())
                    .append(" - ").append(session.getCenter().getName())
                    .append(", assigned role: ").append(assignment.getRole())
                    .append(", status: ").append(session.getLifecycleStatus())
                    .append(", starts: ").append(DISPLAY_TIME.format(assignment.getStartAt().atZone(zone)))
                    .append(", ends: ").append(DISPLAY_TIME.format(assignment.getEndAt().atZone(zone)))
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

    private boolean isActiveAssignment(StaffAssignment assignment, Instant now, boolean todayOnly) {
        if (assignment == null || assignment.getSession() == null
                || assignment.getStartAt() == null || assignment.getEndAt() == null) {
            return false;
        }
        SessionLifecycleStatus lifecycleStatus = assignment.getSession().getLifecycleStatus();
        if (lifecycleStatus == SessionLifecycleStatus.CANCELLED || lifecycleStatus == SessionLifecycleStatus.COMPLETED) {
            return false;
        }
        if (!todayOnly) {
            return assignment.getEndAt().isAfter(now);
        }

        ZoneId zone = resolveZone(assignment.getSession());
        LocalDate today = LocalDate.ofInstant(now, zone);
        Instant dayStart = today.atStartOfDay(zone).toInstant();
        Instant nextDayStart = today.plusDays(1).atStartOfDay(zone).toInstant();
        return assignment.getStartAt().isBefore(nextDayStart) && assignment.getEndAt().isAfter(dayStart);
    }

    private ZoneId resolveZone(VaccinationSession session) {
        if (session == null || session.getScheduleTimezone() == null
                || session.getScheduleTimezone().isBlank()) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(session.getScheduleTimezone());
        } catch (RuntimeException ignored) {
            return DEFAULT_ZONE;
        }
    }
}
