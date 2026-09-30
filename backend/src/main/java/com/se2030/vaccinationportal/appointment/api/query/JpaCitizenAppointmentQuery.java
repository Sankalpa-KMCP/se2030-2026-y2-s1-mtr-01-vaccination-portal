package com.se2030.vaccinationportal.appointment.api.query;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import com.se2030.vaccinationportal.campaign.api.appointment.TimeSlotDisplayReader;
import com.se2030.vaccinationportal.campaign.api.appointment.TimeSlotDisplaySnapshot;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.springframework.stereotype.Component;

@Component
public class JpaCitizenAppointmentQuery implements CitizenAppointmentQuery {
    private static final int MAX_RESULTS = 5;
    private final AppointmentRepository appointments;
    private final WaitlistRepository waitlist;
    private final TimeSlotDisplayReader slots;
    private final Clock clock;

    public JpaCitizenAppointmentQuery(AppointmentRepository appointments, WaitlistRepository waitlist,
            TimeSlotDisplayReader slots, Clock clock) {
        this.appointments = appointments;
        this.waitlist = waitlist;
        this.slots = slots;
        this.clock = clock;
    }

    @Override
    public CitizenAppointmentOverview appointmentsForCitizen(UserPrincipal principal) {
        Long citizenId = requiredUserId(principal);
        List<Appointment> owned = nullSafe(appointments.findByCitizenIdOrderByBookedAtDesc(citizenId));
        return classifyAppointments(owned);
    }

    @Override
    public CitizenAppointmentAndWaitlistOverview appointmentsAndWaitlistForCitizen(UserPrincipal principal) {
        Long citizenId = requiredUserId(principal);
        List<Appointment> owned = nullSafe(appointments.findByCitizenIdOrderByBookedAtDesc(citizenId));
        List<WaitlistEntry> entries = waitlistRows(citizenId);
        // Finish temporal classification before queue-count work can cross a deadline.
        CitizenAppointmentOverview overview = classifyAppointments(owned);
        return new CitizenAppointmentAndWaitlistOverview(overview, summarizeWaitlist(entries));
    }

    private CitizenAppointmentOverview classifyAppointments(List<Appointment> owned) {
        List<Long> ids = owned.stream().map(Appointment::getSlotId).filter(Objects::nonNull).distinct().toList();
        List<TimeSlotDisplaySnapshot> found = ids.isEmpty() ? List.of() : nullSafe(slots.findByIds(ids));
        Map<Long, TimeSlotDisplaySnapshot> byId = found.stream().filter(slot -> slot.id() != null)
                .collect(Collectors.toMap(TimeSlotDisplaySnapshot::id, Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));
        // Classify against time observed after appointment and linked schedule retrieval.
        Instant now = clock.instant();
        List<View> views = owned.stream().map(a -> new View(a, byId.get(a.getSlotId()))).toList();
        Comparator<View> upcomingOrder = Comparator.comparing(JpaCitizenAppointmentQuery::start,
                Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<View> pastOrder = Comparator.comparing(JpaCitizenAppointmentQuery::start,
                Comparator.nullsLast(Comparator.reverseOrder()));
        return new CitizenAppointmentOverview(
                views.stream().filter(v -> active(v, now)).sorted(upcomingOrder).limit(MAX_RESULTS)
                        .map(v -> summary(v, now)).toList(),
                views.stream().filter(v -> !active(v, now)).sorted(pastOrder).limit(MAX_RESULTS)
                        .map(v -> summary(v, now)).toList(),
                views.stream().filter(v -> v.appointment().getStatus() == AppointmentStatus.CHECKED_IN)
                        .limit(MAX_RESULTS).map(v -> summary(v, now)).toList());
    }

    @Override
    public List<WaitlistSummary> waitlistForCitizen(UserPrincipal principal) {
        Long citizenId = requiredUserId(principal);
        return summarizeWaitlist(waitlistRows(citizenId));
    }

    private List<WaitlistEntry> waitlistRows(Long citizenId) {
        return nullSafe(waitlist.findByCitizenIdOrderByJoinedAtDesc(citizenId)).stream().limit(MAX_RESULTS).toList();
    }

    private List<WaitlistSummary> summarizeWaitlist(List<WaitlistEntry> entries) {
        return entries.stream()
                .map(entry -> new WaitlistSummary(status(entry.getStatus()), entry.getCampaignId(),
                        entry.getSessionId(), queuePosition(entry))).toList();
    }

    private Integer queuePosition(WaitlistEntry entry) {
        if (entry.getStatus() != WaitlistStatus.WAITING || entry.getSessionId() == null
                || entry.getJoinedAt() == null || entry.getId() == null) return null;
        long position = waitlist.countQueuePosition(entry.getSessionId(), WaitlistStatus.WAITING,
                entry.getJoinedAt(), entry.getId());
        return position > 0 && position <= Integer.MAX_VALUE ? (int) position : null;
    }

    private static boolean active(View view, Instant now) {
        AppointmentStatus status = view.appointment().getStatus();
        if (status != AppointmentStatus.CONFIRMED && status != AppointmentStatus.CHECKED_IN) return false;
        return status == AppointmentStatus.CHECKED_IN || view.slot() == null
                || view.slot().endTime() == null || !view.slot().endTime().isBefore(now);
    }

    private static Instant start(View view) { return view.slot() == null ? null : view.slot().startTime(); }

    private static AppointmentSummary summary(View view, Instant now) {
        Appointment a = view.appointment();
        TimeSlotDisplaySnapshot slot = view.slot();
        return new AppointmentSummary(status(a.getStatus()), a.getCampaignId(), a.getSlotId(), slot != null,
                slot == null ? null : slot.startTime(), slot == null ? null : slot.endTime(),
                slot == null ? null : slot.campaignName(), slot == null ? null : slot.centerName(),
                slot == null ? null : slot.centerAddress(), a.getStatus() == AppointmentStatus.CONFIRMED
                        && slot != null && slot.endTime() != null && slot.endTime().isBefore(now));
    }

    private static String status(Enum<?> status) { return status == null ? "UNKNOWN" : status.name(); }
    private static Long requiredUserId(UserPrincipal principal) {
        if (principal == null) throw new IllegalArgumentException("Authenticated citizen principal is required");
        return principal.getUserId();
    }
    private static <T> List<T> nullSafe(List<T> values) { return values == null ? List.of() : values; }
    private record View(Appointment appointment, TimeSlotDisplaySnapshot slot) {}
}
