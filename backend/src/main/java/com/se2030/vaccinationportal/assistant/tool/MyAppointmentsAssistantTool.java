package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.appointment.api.query.AppointmentSummary;
import com.se2030.vaccinationportal.appointment.api.query.CitizenAppointmentOverview;
import com.se2030.vaccinationportal.appointment.api.query.CitizenAppointmentQuery;
import com.se2030.vaccinationportal.appointment.api.query.WaitlistSummary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Read-only appointment and waitlist summaries for the authenticated citizen.
 * The citizen identifier always comes from the verified principal.
 */
@Component
public class MyAppointmentsAssistantTool implements AssistantReadOnlyTool {

    private static final int MAX_APPOINTMENT_RESULTS = 5;
    private static final int MAX_WAITLIST_RESULTS = 5;
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Colombo");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter
            .ofPattern("dd MMM uuuu, hh:mm a 'Sri Lanka time'", Locale.ENGLISH)
            .withZone(DISPLAY_ZONE);

    private final CitizenAppointmentQuery appointmentQuery;

    private static final List<Pattern> INTENT_PATTERNS = List.of(
            Pattern.compile("\\b(what\\s+is|what'?s|check|show|view|tell\\s+me|get|find|list|see)\\s+(the\\s+status\\s+of\\s+)?(my|our)\\s+appointment(s)?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(my\\s+appointment(s)?\\s+status|my\\s+appointment(s)?|my\\s+booking(s)?\\s+status|my\\s+booking(s)?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(do\\s+i\\s+have|have\\s+i\\s+got)\\s+(an?|any|a\\s+confirmed)?\\s*(appointment(s)?|booking(s)?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(was\\s+i|am\\s+i|did\\s+i\\s+get|have\\s+i\\s+been)\\s+checked\\s*[- ]?in\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcheck[- ]in\\s+status\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(my|show\\s+me\\s+my|list\\s+my|view\\s+my)\\s+(upcoming|future|past|previous|cancelled|completed)\\s+(appointment(s)?|booking(s)?|record(s)?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(my\\s+waitlist|my\\s+queue\\s+position|am\\s+i\\s+on\\s+(the\\s+)?waitlist|what\\s+is\\s+my\\s+queue\\s+position)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(show|view|find|get|list|check)\\s+appointments?\\s+for\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(appointments?\\s+for\\s+(citizen|user|me|\\d+))\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(wife'?s|husband'?s|child'?s|kid'?s|friend'?s)\\s+appointment(s)?\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> NEGATIVE_PATTERNS = List.of(
            Pattern.compile("\\bhow\\s+(do|can|to)\\s+i\\s+(book|schedule)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bhow\\s+to\\s+(book|schedule)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwhere\\s+(can|do)\\s+i\\s+(book|schedule)\\b", Pattern.CASE_INSENSITIVE)
    );

    public MyAppointmentsAssistantTool(CitizenAppointmentQuery appointmentQuery) {
        this.appointmentQuery = appointmentQuery;
    }

    @Override
    public AssistantToolId getToolId() {
        return AssistantToolId.MY_APPOINTMENTS;
    }

    @Override
    public boolean matchesIntent(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return false;
        String normalized = userMessage.trim();
        for (Pattern negative : NEGATIVE_PATTERNS) {
            if (negative.matcher(normalized).find()) return false;
        }
        for (Pattern pattern : INTENT_PATTERNS) {
            if (pattern.matcher(normalized).find()) return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public AssistantToolResult execute(AssistantToolContext context) {
        return execute(context, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AssistantToolResult execute(AssistantToolContext context, String userMessage) {
        if (context == null || context.principal() == null || !context.isCitizen()) {
            throw new IllegalArgumentException("Personalized tools require an authenticated citizen principal.");
        }

        AppointmentIntent intent = detectIntent(userMessage);
        boolean needsAppointments = intent != AppointmentIntent.WAITLIST;
        boolean needsWaitlist = intent == AppointmentIntent.ALL || intent == AppointmentIntent.WAITLIST;
        CitizenAppointmentOverview overview;
        List<WaitlistSummary> waitlistEntries;
        if (needsAppointments && needsWaitlist) {
            var combined = appointmentQuery.appointmentsAndWaitlistForCitizen(context.principal());
            overview = combined.appointments();
            waitlistEntries = combined.waitlist();
        } else {
            overview = needsAppointments ? appointmentQuery.appointmentsForCitizen(context.principal())
                    : new CitizenAppointmentOverview(List.of(), List.of(), List.of());
            waitlistEntries = needsWaitlist ? appointmentQuery.waitlistForCitizen(context.principal()) : List.of();
        }
        List<AppointmentSummary> activeOrUpcoming = overview.activeOrUpcoming();
        List<AppointmentSummary> pastOrInactive = overview.pastOrInactive();
        StringBuilder sb = new StringBuilder("Read-only information for the signed-in citizen only.\n");
        int resultCount = 0;
        List<String> routes;

        switch (intent) {
            case UPCOMING -> {
                routes = List.of("/app/appointments/mine");
                resultCount += appendAppointmentSection(sb, "Active or upcoming appointment records", activeOrUpcoming);
                if (activeOrUpcoming.isEmpty()) {
                    sb.append("No active or upcoming appointment was found. The citizen can search available slots using the authorized Book Appointment page.\n");
                    routes = List.of("/app/appointments/mine", "/app/appointments/book");
                }
            }
            case PAST -> {
                routes = List.of("/app/appointments/mine");
                resultCount += appendAppointmentSection(sb, "Past or inactive appointment records", pastOrInactive);
                if (pastOrInactive.isEmpty()) sb.append("No past or inactive appointment record was found.\n");
            }
            case CHECK_IN -> {
                routes = List.of("/app/appointments/mine");
                List<AppointmentSummary> checkedIn = overview.checkedIn();
                resultCount += appendAppointmentSection(sb, "Checked-in appointment records", checkedIn);
                if (checkedIn.isEmpty()) sb.append("No appointment currently has the recorded status CHECKED_IN.\n");
            }
            case WAITLIST -> {
                routes = List.of("/app/appointments/waitlist");
                resultCount += appendWaitlistSection(sb, waitlistEntries);
                if (waitlistEntries.isEmpty()) sb.append("No waitlist record was found for the signed-in citizen.\n");
            }
            default -> {
                routes = List.of("/app/appointments/mine", "/app/appointments/waitlist");
                resultCount += appendAppointmentSection(sb, "Active or upcoming appointment records", activeOrUpcoming);
                resultCount += appendAppointmentSection(sb, "Past or inactive appointment records", pastOrInactive);
                resultCount += appendWaitlistSection(sb, waitlistEntries);
                if (resultCount == 0) {
                    sb.append("No appointment or waitlist record was found. The citizen can search available slots using the authorized Book Appointment page.\n");
                    routes = List.of("/app/appointments/mine", "/app/appointments/waitlist", "/app/appointments/book");
                }
            }
        }
        sb.append("Use only these recorded statuses and linked MF-02 schedule details. A past scheduled time with status CONFIRMED is not proof of a no-show. Never describe record creation timestamps as the scheduled appointment time.");
        return new AssistantToolResult(AssistantToolId.MY_APPOINTMENTS, true, resultCount, sb.toString(), routes);
    }

    private static int appendAppointmentSection(StringBuilder sb, String heading,
                                                List<AppointmentSummary> records) {
        if (records.isEmpty()) return 0;
        sb.append(heading).append(" (bounded to at most ").append(MAX_APPOINTMENT_RESULTS).append("):\n");
        int index = 1;
        for (AppointmentSummary appointment : records) {
            sb.append("- Appointment Record ").append(index++).append(": Status: ")
                    .append(appointment.status())
                    .append(", Campaign ID: ").append(appointment.campaignId())
                    .append(", Slot ID: ").append(appointment.slotId());
            appendFriendlySchedule(sb, appointment);
            if (appointment.confirmedPastTime()) {
                sb.append(", Timing Note: scheduled time has passed but the recorded status remains CONFIRMED");
            }
            sb.append("\n");
        }
        return records.size();
    }

    private static void appendFriendlySchedule(StringBuilder sb, AppointmentSummary appointment) {
        if (!appointment.linkedSlotPresent()) {
            sb.append(", Schedule: unavailable from the linked time slot");
            return;
        }
        if (hasText(appointment.campaignName())) sb.append(", Campaign: ").append(appointment.campaignName().trim());
        if (hasText(appointment.centerName())) sb.append(", Center: ").append(appointment.centerName().trim());
        if (hasText(appointment.centerAddress())) sb.append(", Center Address: ").append(appointment.centerAddress().trim());
        sb.append(", Scheduled Start: ").append(formatTime(appointment.scheduledStart()))
                .append(", Scheduled End: ").append(formatTime(appointment.scheduledEnd()));
    }

    private int appendWaitlistSection(StringBuilder sb, List<WaitlistSummary> entries) {
        if (entries.isEmpty()) return 0;
        sb.append("Waitlist records (newest first, bounded to at most ").append(MAX_WAITLIST_RESULTS).append("):\n");
        int index = 1;
        for (WaitlistSummary entry : entries) {
            sb.append("- Waitlist Record ").append(index++).append(": Status: ")
                    .append(entry.status())
                    .append(", Campaign ID: ").append(entry.campaignId())
                    .append(", Session ID: ").append(entry.sessionId());
            if (entry.queuePosition() != null) sb.append(", Current Queue Position: ").append(entry.queuePosition());
            sb.append("\n");
        }
        return entries.size();
    }

    private static AppointmentIntent detectIntent(String message) {
        if (message == null || message.isBlank()) return AppointmentIntent.ALL;
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("waitlist") || normalized.contains("queue position")) return AppointmentIntent.WAITLIST;
        if (normalized.contains("checked in") || normalized.contains("check-in") || normalized.contains("check in"))
            return AppointmentIntent.CHECK_IN;
        if (normalized.contains("upcoming") || normalized.contains("future")) return AppointmentIntent.UPCOMING;
        if (normalized.contains("past") || normalized.contains("previous") || normalized.contains("completed")
                || normalized.contains("cancelled"))
            return AppointmentIntent.PAST;
        return AppointmentIntent.ALL;
    }

    private static String formatTime(Instant instant) {
        return instant == null ? "unavailable" : DISPLAY_TIME.format(instant);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private enum AppointmentIntent { ALL, UPCOMING, PAST, WAITLIST, CHECK_IN }
}
