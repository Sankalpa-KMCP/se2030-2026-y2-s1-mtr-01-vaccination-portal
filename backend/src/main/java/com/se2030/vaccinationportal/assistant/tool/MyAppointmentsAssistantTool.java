package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Server-side read-only tool providing bounded appointment summary for the authenticated citizen.
 * Strictly uses principal.getUserId() and ignores any citizen identifier in the user message.
 */
@Component
public class MyAppointmentsAssistantTool implements AssistantReadOnlyTool {

    private final AppointmentRepository appointmentRepository;

    private static final List<Pattern> INTENT_PATTERNS = List.of(
            Pattern.compile("\\b(what\\s+is|what'?s|check|show|view|tell\\s+me|get|find|list|see)\\s+(the\\s+status\\s+of\\s+)?(my|our)\\s+appointment(s)?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(my\\s+appointment(s)?\\s+status|my\\s+appointment(s)?|my\\s+booking(s)?\\s+status|my\\s+booking(s)?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(do\\s+i\\s+have|have\\s+i\\s+got)\\s+(an?|any|a\\s+confirmed)?\\s*(appointment(s)?|booking(s)?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(was\\s+i|am\\s+i|did\\s+i\\s+get|have\\s+i\\s+been)\\s+checked\\s*[- ]?in\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcheck[- ]in\\s+status\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(show|view|find|get|list|check)\\s+appointments?\\s+for\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(appointments?\\s+for\\s+(citizen|user|me|\\d+))\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(wife'?s|husband'?s|child'?s|kid'?s|friend'?s)\\s+appointment(s)?\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> NEGATIVE_PATTERNS = List.of(
            Pattern.compile("\\bhow\\s+(do|can|to)\\s+i\\s+(book|schedule)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bhow\\s+to\\s+(book|schedule)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwhere\\s+(can|do)\\s+i\\s+(book|schedule)\\b", Pattern.CASE_INSENSITIVE)
    );

    public MyAppointmentsAssistantTool(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public AssistantToolId getToolId() {
        return AssistantToolId.MY_APPOINTMENTS;
    }

    @Override
    public boolean matchesIntent(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        String normalized = userMessage.trim();
        for (Pattern negative : NEGATIVE_PATTERNS) {
            if (negative.matcher(normalized).find()) {
                return false;
            }
        }
        for (Pattern pattern : INTENT_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public AssistantToolResult execute(AssistantToolContext context) {
        if (context == null || context.principal() == null || !context.isCitizen()) {
            throw new IllegalArgumentException("Personalized tools require an authenticated citizen principal.");
        }

        Long citizenId = context.getCitizenId();
        List<Appointment> allAppointments = appointmentRepository.findByCitizenIdOrderByBookedAtDesc(citizenId);
        List<String> routes = List.of("/app/appointments/mine");

        if (allAppointments == null || allAppointments.isEmpty()) {
            return AssistantToolResult.empty(
                    AssistantToolId.MY_APPOINTMENTS,
                    "No appointment records were found for the signed-in citizen.",
                    routes
            );
        }

        List<Appointment> bounded = allAppointments.stream().limit(5).toList();
        StringBuilder sb = new StringBuilder();
        sb.append("Recent appointment records for the authenticated citizen (showing ")
                .append(bounded.size())
                .append(" most recent, bounded to at most 5):\n");

        int index = 1;
        for (Appointment appt : bounded) {
            sb.append("- Appointment Record ").append(index++).append(": ")
                    .append("Status: ").append(appt.getStatus() != null ? appt.getStatus().name() : "UNKNOWN")
                    .append(", Campaign ID: ").append(appt.getCampaignId())
                    .append(", Slot ID: ").append(appt.getSlotId())
                    .append("\n");
        }
        sb.append("Note: The repository provides status, campaign ID, and slot ID. It does not provide an authoritative scheduled appointment timestamp. Never describe record creation timestamps as the scheduled appointment time. Advise the user to view their full appointment schedule under /app/appointments/mine.");

        return new AssistantToolResult(
                AssistantToolId.MY_APPOINTMENTS,
                true,
                bounded.size(),
                sb.toString(),
                routes
        );
    }
}
