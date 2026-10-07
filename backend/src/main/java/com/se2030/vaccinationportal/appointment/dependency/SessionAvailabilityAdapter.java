package com.se2030.vaccinationportal.appointment.dependency;

import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.service.TimeSlotService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * MF-03 read adapter for the existing MF-02 session and slot services.
 * Does not access MF-02 repositories, mutate capacity, or reserve places.
 * Results are snapshots; the booking service still reserves capacity atomically.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Search available vaccination appointment slots at nearby vaccination centers.</li>
 * </ul>
 *
 * <p><b>Viva / Architectural Separation Note:</b><br>
 * This adapter is deliberately read-only and decoupled from {@link SlotCapacityPort}.
 * Keeping the search path separate from the booking/reservation mutation path prevents read operations
 * from locking capacity rows and enables independent testability with lightweight doubles.
 * </p>
 */
@Component
@Transactional(readOnly = true)
public class SessionAvailabilityAdapter implements SlotAvailabilityPort {
    private final VaccinationSessionService sessions;
    private final TimeSlotService slots;
    private final Clock clock;

    @Autowired
    public SessionAvailabilityAdapter(VaccinationSessionService sessions, TimeSlotService slots) {
        this(sessions, slots, Clock.systemUTC());
    }

    SessionAvailabilityAdapter(VaccinationSessionService sessions, TimeSlotService slots, Clock clock) {
        this.sessions = sessions;
        this.slots = slots;
        this.clock = clock;
    }

    @Override
    public List<AvailableSessionSummary> findAvailableSessions(
            Long campaignId, Long centerId, LocalDate fromDate, LocalDate toDate) {
        if (campaignId == null || fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Campaign and a valid inclusive date range are required");
        }
        Instant now = clock.instant();
        return sessions.getSessions(campaignId, centerId, SessionLifecycleStatus.SCHEDULED).stream()
                .filter(session -> acceptsBooking(session, now))
                .filter(session -> isWithinDateRange(session, fromDate, toDate))
                .flatMap(session -> slots.getSlotsForSession(session.getId()).stream()
                        .filter(slot -> slot.hasAvailableCapacity() && slot.getStartTime().isAfter(now))
                        .map(slot -> new AvailableSessionSummary(
                                session.getId(), slot.getId(), session.getCampaign().getId(),
                                session.getCenter().getId(), session.getCenter().getName(),
                                session.getCenter().getAddress(), slot.getStartTime(), slot.getEndTime(),
                                session.getBookingCloseAt(), slot.getAvailableCapacity())))
                .sorted(Comparator.comparing(AvailableSessionSummary::startAt)
                        .thenComparing(AvailableSessionSummary::sessionId)
                        .thenComparing(AvailableSessionSummary::slotId))
                .toList();
    }

    private boolean isWithinDateRange(VaccinationSession session, LocalDate fromDate, LocalDate toDate) {
        // Match dates in the session's scheduling timezone, including both range endpoints.
        LocalDate date = session.getStartAt().atZone(ZoneId.of(session.getScheduleTimezone())).toLocalDate();
        return !date.isBefore(fromDate) && !date.isAfter(toDate);
    }

    private boolean acceptsBooking(VaccinationSession session, Instant now) {
        return session.getLifecycleStatus() == SessionLifecycleStatus.SCHEDULED
                && session.getCampaign().isBookingWindowOpenAt(now)
                && !session.isBookingSuspended()
                && now.isBefore(session.getBookingCloseAt());
    }
}
