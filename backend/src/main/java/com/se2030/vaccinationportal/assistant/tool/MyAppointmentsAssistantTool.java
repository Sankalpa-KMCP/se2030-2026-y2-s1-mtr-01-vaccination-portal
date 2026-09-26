package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final WaitlistRepository waitlistRepository;

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

    public MyAppointmentsAssistantTool(
            AppointmentRepository appointmentRepository,
            TimeSlotRepository timeSlotRepository,
            WaitlistRepository waitlistRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.waitlistRepository = waitlistRepository;
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
        return isSinhalaAppointmentIntent(normalized) || isTamilAppointmentIntent(normalized);
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
        Long citizenId = context.getCitizenId();
        boolean needsAppointments = intent != AppointmentIntent.WAITLIST;
        boolean needsWaitlist = intent == AppointmentIntent.ALL || intent == AppointmentIntent.WAITLIST;
        List<Appointment> appointments = needsAppointments
                ? nullSafe(appointmentRepository.findByCitizenIdOrderByBookedAtDesc(citizenId)) : List.of();
        List<WaitlistEntry> waitlistEntries = needsWaitlist
                ? nullSafe(waitlistRepository.findByCitizenIdOrderByJoinedAtDesc(citizenId)).stream()
                    .limit(MAX_WAITLIST_RESULTS).toList() : List.of();

        Map<Long, TimeSlot> slotsById = loadSlotsById(appointments);
        Instant now = Instant.now();
        List<AppointmentView> activeOrUpcoming = classifyAppointments(appointments, slotsById, now, true);
        List<AppointmentView> pastOrInactive = classifyAppointments(appointments, slotsById, now, false);
        StringBuilder sb = new StringBuilder("Read-only information for the signed-in citizen only.\n");
        int resultCount = 0;
        List<String> routes;

        switch (intent) {
            case UPCOMING -> {
                routes = List.of("/app/appointments/mine");
                resultCount += appendAppointmentSection(sb, "Active or upcoming appointment records", activeOrUpcoming, now);
                if (activeOrUpcoming.isEmpty()) {
                    sb.append("No active or upcoming appointment was found. The citizen can search available slots using the authorized Book Appointment page.\n");
                    routes = List.of("/app/appointments/mine", "/app/appointments/book");
                }
            }
            case PAST -> {
                routes = List.of("/app/appointments/mine");
                resultCount += appendAppointmentSection(sb, "Past or inactive appointment records", pastOrInactive, now);
                if (pastOrInactive.isEmpty()) sb.append("No past or inactive appointment record was found.\n");
            }
            case CHECK_IN -> {
                routes = List.of("/app/appointments/mine");
                List<AppointmentView> checkedIn = appointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.CHECKED_IN)
                        .map(a -> new AppointmentView(a, slotsById.get(a.getSlotId())))
                        .limit(MAX_APPOINTMENT_RESULTS).toList();
                resultCount += appendAppointmentSection(sb, "Checked-in appointment records", checkedIn, now);
                if (checkedIn.isEmpty()) sb.append("No appointment currently has the recorded status CHECKED_IN.\n");
            }
            case WAITLIST -> {
                routes = List.of("/app/appointments/waitlist");
                resultCount += appendWaitlistSection(sb, waitlistEntries);
                if (waitlistEntries.isEmpty()) sb.append("No waitlist record was found for the signed-in citizen.\n");
            }
            default -> {
                routes = List.of("/app/appointments/mine", "/app/appointments/waitlist");
                resultCount += appendAppointmentSection(sb, "Active or upcoming appointment records", activeOrUpcoming, now);
                resultCount += appendAppointmentSection(sb, "Past or inactive appointment records", pastOrInactive, now);
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

    private List<AppointmentView> classifyAppointments(List<Appointment> appointments, Map<Long, TimeSlot> slotsById,
                                                       Instant now, boolean active) {
        Comparator<AppointmentView> comparator = active
                ? Comparator.comparing(MyAppointmentsAssistantTool::scheduledStart, Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(MyAppointmentsAssistantTool::scheduledStart, Comparator.nullsLast(Comparator.reverseOrder()));
        return appointments.stream()
                .map(a -> new AppointmentView(a, slotsById.get(a.getSlotId())))
                .filter(view -> isActiveOrUpcoming(view, now) == active)
                .sorted(comparator).limit(MAX_APPOINTMENT_RESULTS).toList();
    }

    private Map<Long, TimeSlot> loadSlotsById(List<Appointment> appointments) {
        List<Long> slotIds = appointments.stream().map(Appointment::getSlotId)
                .filter(Objects::nonNull).distinct().toList();
        if (slotIds.isEmpty()) return Map.of();
        List<TimeSlot> slots = timeSlotRepository.findByIdIn(slotIds);
        if (slots == null || slots.isEmpty()) return Map.of();
        return slots.stream().filter(slot -> slot.getId() != null)
                .collect(Collectors.toMap(TimeSlot::getId, Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));
    }

    private static boolean isActiveOrUpcoming(AppointmentView view, Instant now) {
        AppointmentStatus status = view.appointment().getStatus();
        if (status != AppointmentStatus.CONFIRMED && status != AppointmentStatus.CHECKED_IN) return false;
        return status == AppointmentStatus.CHECKED_IN || view.slot() == null
                || view.slot().getEndTime() == null || !view.slot().getEndTime().isBefore(now);
    }

    private static Instant scheduledStart(AppointmentView view) {
        return view.slot() == null ? null : view.slot().getStartTime();
    }

    private static int appendAppointmentSection(StringBuilder sb, String heading,
                                                List<AppointmentView> records, Instant now) {
        if (records.isEmpty()) return 0;
        sb.append(heading).append(" (bounded to at most ").append(MAX_APPOINTMENT_RESULTS).append("):\n");
        int index = 1;
        for (AppointmentView view : records) {
            Appointment appointment = view.appointment();
            sb.append("- Appointment Record ").append(index++).append(": Status: ")
                    .append(appointment.getStatus() != null ? appointment.getStatus().name() : "UNKNOWN")
                    .append(", Campaign ID: ").append(appointment.getCampaignId())
                    .append(", Slot ID: ").append(appointment.getSlotId());
            appendFriendlySchedule(sb, view.slot());
            if (appointment.getStatus() == AppointmentStatus.CONFIRMED && view.slot() != null
                    && view.slot().getEndTime() != null && view.slot().getEndTime().isBefore(now)) {
                sb.append(", Timing Note: scheduled time has passed but the recorded status remains CONFIRMED");
            }
            sb.append("\n");
        }
        return records.size();
    }

    private static void appendFriendlySchedule(StringBuilder sb, TimeSlot slot) {
        if (slot == null) {
            sb.append(", Schedule: unavailable from the linked time slot");
            return;
        }
        VaccinationSession session = slot.getSession();
        if (session != null && session.getCampaign() != null && hasText(session.getCampaign().getName()))
            sb.append(", Campaign: ").append(session.getCampaign().getName().trim());
        if (session != null && session.getCenter() != null && hasText(session.getCenter().getName()))
            sb.append(", Center: ").append(session.getCenter().getName().trim());
        if (session != null && session.getCenter() != null && hasText(session.getCenter().getAddress()))
            sb.append(", Center Address: ").append(session.getCenter().getAddress().trim());
        sb.append(", Scheduled Start: ").append(formatTime(slot.getStartTime()))
                .append(", Scheduled End: ").append(formatTime(slot.getEndTime()));
    }

    private int appendWaitlistSection(StringBuilder sb, List<WaitlistEntry> entries) {
        if (entries.isEmpty()) return 0;
        sb.append("Waitlist records (newest first, bounded to at most ").append(MAX_WAITLIST_RESULTS).append("):\n");
        int index = 1;
        for (WaitlistEntry entry : entries) {
            sb.append("- Waitlist Record ").append(index++).append(": Status: ")
                    .append(entry.getStatus() != null ? entry.getStatus().name() : "UNKNOWN")
                    .append(", Campaign ID: ").append(entry.getCampaignId())
                    .append(", Session ID: ").append(entry.getSessionId());
            Integer position = findQueuePosition(entry);
            if (position != null) sb.append(", Current Queue Position: ").append(position);
            sb.append("\n");
        }
        return entries.size();
    }

    private Integer findQueuePosition(WaitlistEntry entry) {
        if (entry.getStatus() != WaitlistStatus.WAITING || entry.getSessionId() == null
                || entry.getJoinedAt() == null || entry.getId() == null) return null;
        long position = waitlistRepository.countQueuePosition(
                entry.getSessionId(), WaitlistStatus.WAITING, entry.getJoinedAt(), entry.getId());
        return position > 0 && position <= Integer.MAX_VALUE ? (int) position : null;
    }

    private static AppointmentIntent detectIntent(String message) {
        if (message == null || message.isBlank()) return AppointmentIntent.ALL;
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("waitlist") || normalized.contains("queue position")
                || normalized.contains("පොරොත්තු") || normalized.contains("පෝලිමේ")
                || normalized.contains("காத்திருப்பு") || normalized.contains("வரிசை நிலை")) return AppointmentIntent.WAITLIST;
        if (normalized.contains("checked in") || normalized.contains("check-in") || normalized.contains("check in"))
            return AppointmentIntent.CHECK_IN;
        if (normalized.contains("upcoming") || normalized.contains("future")
                || normalized.contains("ඉදිරි") || normalized.contains("வரவிருக்கும்")) return AppointmentIntent.UPCOMING;
        if (normalized.contains("past") || normalized.contains("previous") || normalized.contains("completed")
                || normalized.contains("cancelled") || normalized.contains("පසුගිය") || normalized.contains("கடந்த"))
            return AppointmentIntent.PAST;
        return AppointmentIntent.ALL;
    }

    private static boolean isSinhalaAppointmentIntent(String text) {
        return (text.contains("මගේ") || text.contains("මාගේ"))
                && (text.contains("හමුවීම්") || text.contains("වෙන්කිරීම්")
                || text.contains("පොරොත්තු") || text.contains("පෝලිමේ"));
    }

    private static boolean isTamilAppointmentIntent(String text) {
        return (text.contains("எனது") || text.contains("என்"))
                && (text.contains("சந்திப்பு") || text.contains("காத்திருப்பு") || text.contains("வரிசை"));
    }

    private static String formatTime(Instant instant) {
        return instant == null ? "unavailable" : DISPLAY_TIME.format(instant);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private enum AppointmentIntent { ALL, UPCOMING, PAST, WAITLIST, CHECK_IN }

    private record AppointmentView(Appointment appointment, TimeSlot slot) {}
}
