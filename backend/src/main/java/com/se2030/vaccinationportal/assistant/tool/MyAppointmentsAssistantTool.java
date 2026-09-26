package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Server-side read-only tool providing bounded appointment summary for the authenticated citizen.
 * Strictly uses principal.getUserId() and ignores any citizen identifier in the user message.
 */
@Component
public class MyAppointmentsAssistantTool implements AssistantReadOnlyTool {

    private static final int MAX_APPOINTMENT_RESULTS = 5;
    private static final int MAX_WAITLIST_RESULTS = 5;

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
        List<WaitlistEntry> allWaitlistEntries = waitlistRepository.findByCitizenIdOrderByJoinedAtDesc(citizenId);
        List<String> routes = List.of("/app/appointments/mine", "/app/appointments/waitlist");

        if ((allAppointments == null || allAppointments.isEmpty())
                && (allWaitlistEntries == null || allWaitlistEntries.isEmpty())) {
            return AssistantToolResult.empty(
                    AssistantToolId.MY_APPOINTMENTS,
                    "No appointment or waitlist records were found for the signed-in citizen.",
                    routes
            );
        }

        List<Appointment> appointments = allAppointments == null ? List.of() : allAppointments;
        Map<Long, TimeSlot> slotsById = loadSlotsById(appointments);
        Instant now = Instant.now();

        List<AppointmentView> activeOrUpcoming = appointments.stream()
                .map(appointment -> new AppointmentView(appointment, slotsById.get(appointment.getSlotId())))
                .filter(view -> isActiveOrUpcoming(view, now))
                .sorted(Comparator.comparing(
                        MyAppointmentsAssistantTool::scheduledStart,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_APPOINTMENT_RESULTS)
                .toList();

        List<AppointmentView> pastOrInactive = appointments.stream()
                .map(appointment -> new AppointmentView(appointment, slotsById.get(appointment.getSlotId())))
                .filter(view -> !isActiveOrUpcoming(view, now))
                .sorted(Comparator.comparing(
                        MyAppointmentsAssistantTool::scheduledStart,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_APPOINTMENT_RESULTS)
                .toList();

        List<WaitlistEntry> waitlistEntries = (allWaitlistEntries == null ? List.<WaitlistEntry>of() : allWaitlistEntries)
                .stream()
                .limit(MAX_WAITLIST_RESULTS)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Read-only appointment snapshot for the authenticated citizen.\n");
        appendAppointmentSection(sb, "Active or upcoming appointment records", activeOrUpcoming, now);
        appendAppointmentSection(sb, "Past or inactive appointment records", pastOrInactive, now);

        if (!waitlistEntries.isEmpty()) {
            sb.append("Waitlist records (newest first, bounded to at most ")
                    .append(MAX_WAITLIST_RESULTS).append("):\n");
            int index = 1;
            for (WaitlistEntry entry : waitlistEntries) {
                sb.append("- Waitlist Record ").append(index++).append(": ")
                        .append("Status: ").append(entry.getStatus() != null ? entry.getStatus().name() : "UNKNOWN")
                        .append(", Campaign ID: ").append(entry.getCampaignId())
                        .append(", Session ID: ").append(entry.getSessionId());
                Integer position = findQueuePosition(entry, citizenId);
                if (position != null) {
                    sb.append(", Current Queue Position: ").append(position);
                }
                sb.append("\n");
            }
        }
        sb.append("Use only these recorded statuses and MF-02 time-slot timestamps. A past scheduled time with status CONFIRMED is not proof of a no-show. Never describe record creation timestamps as the scheduled appointment time.");

        return new AssistantToolResult(
                AssistantToolId.MY_APPOINTMENTS,
                true,
                activeOrUpcoming.size() + pastOrInactive.size() + waitlistEntries.size(),
                sb.toString(),
                routes
        );
    }

    private Map<Long, TimeSlot> loadSlotsById(List<Appointment> appointments) {
        List<Long> slotIds = appointments.stream()
                .map(Appointment::getSlotId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (slotIds.isEmpty()) {
            return Map.of();
        }
        List<TimeSlot> slots = timeSlotRepository.findAllById(slotIds);
        if (slots == null || slots.isEmpty()) {
            return Map.of();
        }
        return slots.stream()
                .filter(slot -> slot.getId() != null)
                .collect(Collectors.toMap(
                        TimeSlot::getId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    private static boolean isActiveOrUpcoming(AppointmentView view, Instant now) {
        AppointmentStatus status = view.appointment().getStatus();
        if (status != AppointmentStatus.CONFIRMED && status != AppointmentStatus.CHECKED_IN) {
            return false;
        }
        if (status == AppointmentStatus.CHECKED_IN || view.slot() == null || view.slot().getEndTime() == null) {
            return true;
        }
        return !view.slot().getEndTime().isBefore(now);
    }

    private static Instant scheduledStart(AppointmentView view) {
        return view.slot() == null ? null : view.slot().getStartTime();
    }

    private static void appendAppointmentSection(
            StringBuilder sb,
            String heading,
            List<AppointmentView> records,
            Instant now
    ) {
        if (records.isEmpty()) {
            return;
        }
        sb.append(heading).append(" (bounded to at most ")
                .append(MAX_APPOINTMENT_RESULTS).append("):\n");
        int index = 1;
        for (AppointmentView view : records) {
            Appointment appointment = view.appointment();
            sb.append("- Appointment Record ").append(index++).append(": ")
                    .append("Status: ").append(appointment.getStatus() != null ? appointment.getStatus().name() : "UNKNOWN")
                    .append(", Campaign ID: ").append(appointment.getCampaignId())
                    .append(", Slot ID: ").append(appointment.getSlotId());
            if (view.slot() != null) {
                sb.append(", Scheduled Start: ").append(view.slot().getStartTime())
                        .append(", Scheduled End: ").append(view.slot().getEndTime());
                if (appointment.getStatus() == AppointmentStatus.CONFIRMED
                        && view.slot().getEndTime() != null
                        && view.slot().getEndTime().isBefore(now)) {
                    sb.append(", Timing Note: scheduled time has passed but the recorded status remains CONFIRMED");
                }
            } else {
                sb.append(", Schedule: unavailable from the linked time slot");
            }
            sb.append("\n");
        }
    }

    private Integer findQueuePosition(WaitlistEntry entry, Long citizenId) {
        if (entry.getStatus() != WaitlistStatus.WAITING || entry.getSessionId() == null) {
            return null;
        }
        List<WaitlistEntry> queue = waitlistRepository.findBySessionIdAndStatusOrderByJoinedAtAsc(
                entry.getSessionId(), WaitlistStatus.WAITING);
        if (queue == null) {
            return null;
        }
        for (int i = 0; i < queue.size(); i++) {
            WaitlistEntry queued = queue.get(i);
            if (Objects.equals(queued.getCitizenId(), citizenId)
                    && Objects.equals(queued.getCampaignId(), entry.getCampaignId())) {
                return i + 1;
            }
        }
        return null;
    }

    private record AppointmentView(Appointment appointment, TimeSlot slot) {
    }
}
