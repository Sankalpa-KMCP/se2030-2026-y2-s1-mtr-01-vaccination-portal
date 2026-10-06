package com.se2030.vaccinationportal.campaign.session.planning.adapter;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;


import com.se2030.vaccinationportal.campaign.api.availability.SlotAvailabilityPort;
import com.se2030.vaccinationportal.campaign.api.availability.AvailableSessionSummary;

import com.se2030.vaccinationportal.appointment.booking.dto.AvailableSessionResponse;
import com.se2030.vaccinationportal.appointment.query.service.AppointmentSearchService;
import com.se2030.vaccinationportal.campaign.session.planning.service.TimeSlotService;
import com.se2030.vaccinationportal.campaign.session.planning.service.VaccinationSessionService;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/** Real H2-backed MF-02 reads through the production MF-03 adapter, without port doubles. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionAvailabilityAdapterTests {
    @Autowired EntityManager em;
    @Autowired SlotAvailabilityPort productionPort;
    @Autowired AppointmentSearchService search;
    @Autowired VaccinationSessionService sessions;
    @Autowired TimeSlotService slots;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private SessionAvailabilityAdapter adapter;
    private final Instant now = Instant.parse("2030-01-01T00:00:00Z");
    private final LocalDate date = LocalDate.of(2030, 1, 2);

    @BeforeEach
    void setup() {
        VaccineType vaccine = new VaccineType("MF03-AV-V", "Availability vaccine");
        TargetGroup group = new TargetGroup("MF03-AV-G", "Availability group");
        em.persist(vaccine);
        em.persist(group);
        campaign = new VaccinationCampaign("MF03-AV", "Availability campaign", "Synthetic test",
                vaccine, group, date.minusDays(1), date.plusDays(5), now.minusSeconds(60), now.plusSeconds(86400 * 5));
        campaign.publish();
        em.persist(campaign);
        center = new VaccinationCenter("MF03-AV-C", "Availability center", "Test address", "12345",
                CenterStatus.ACTIVE, 10, 20, "Asia/Colombo");
        em.persist(center);
        adapter = new SessionAvailabilityAdapter(sessions, slots, Clock.fixed(now, ZoneOffset.UTC));
    }

    private VaccinationSession session(Instant start) {
        VaccinationSession session = new VaccinationSession(campaign, center, start, start.plusSeconds(7200),
                start.minusSeconds(3600), "Asia/Colombo");
        session.schedule();
        em.persist(session);
        return session;
    }

    private TimeSlot slot(VaccinationSession session, int offset, int reserved) {
        TimeSlot slot = new TimeSlot(session, session.getStartAt().plusSeconds(offset),
                session.getStartAt().plusSeconds(offset + 600), 4);
        slot.setReservedCount(reserved);
        em.persist(slot);
        return slot;
    }

    private List<AvailableSessionSummary> results() {
        em.flush();
        return adapter.findAvailableSessions(campaign.getId(), center.getId(), date, date);
    }

    @Test
    void readsRealSlotsInStableOrderWithSeparateSessionIdentityAndRemainingCapacity() {
        VaccinationSession session = session(Instant.parse("2030-01-02T02:00:00Z"));
        slot(session, 0, 4); // Full slot is not advertised.
        TimeSlot later = slot(session, 1200, 1);
        TimeSlot earlier = slot(session, 600, 2);

        var result = results();
        assertEquals(List.of(earlier.getId(), later.getId()), result.stream().map(AvailableSessionSummary::slotId).toList());
        assertEquals(2, result.getFirst().remainingCapacity());
        assertEquals(session.getId(), result.getFirst().sessionId());
        assertNotEquals(session.getId(), earlier.getId());
        var response = AvailableSessionResponse.fromSummary(result.getFirst());
        assertEquals(earlier.getId(), response.slotId());
        assertEquals(session.getId(), response.sessionId());
        assertEquals(earlier.getStartTime(), response.startAt());
        assertEquals("Availability center", response.centerName());
        assertEquals(1, later.getReservedCount(), "Searching must not reserve capacity");
    }

    @Test
    void respectsScheduleTimezoneAndInclusiveDatesAndDelegatesCampaignCenterFilters() {
        // January 1 UTC, but January 2 in the session's timezone.
        var session = session(Instant.parse("2030-01-01T20:00:00Z"));
        slot(session, 0, 0);
        assertEquals(1, results().size());
        assertTrue(adapter.findAvailableSessions(campaign.getId(), center.getId(), date.minusDays(1), date.minusDays(1)).isEmpty());
        assertTrue(adapter.findAvailableSessions(campaign.getId(), Long.MAX_VALUE, date, date).isEmpty());
        assertTrue(adapter.findAvailableSessions(Long.MAX_VALUE, null, date, date).isEmpty());
        assertEquals(1, adapter.findAvailableSessions(campaign.getId(), null, date, date).size());
    }

    @Test
    void excludesSuspendedCancelledAndClosedSessions() {
        var suspended = session(Instant.parse("2030-01-02T02:00:00Z"));
        slot(suspended, 0, 0);
        suspended.suspendBooking();
        var cancelled = session(Instant.parse("2030-01-02T03:00:00Z"));
        slot(cancelled, 0, 0);
        cancelled.cancel();
        var closed = new VaccinationSession(campaign, center, Instant.parse("2030-01-02T04:00:00Z"),
                Instant.parse("2030-01-02T06:00:00Z"), now, "Asia/Colombo");
        closed.schedule();
        em.persist(closed);
        slot(closed, 0, 0);
        assertTrue(results().isEmpty());
    }

    @Test
    void honorsAuthoritativeCampaignBookingWindow() {
        slot(session(Instant.parse("2030-01-02T02:00:00Z")), 0, 0);
        campaign.setBookingOpenAt(now.plusSeconds(60));
        assertTrue(results().isEmpty());
    }

    @Test
    void productionSearchIsWiredWithoutAvailabilityDouble() {
        assertInstanceOf(SessionAvailabilityAdapter.class, productionPort);
        Instant start = Instant.now().plusSeconds(86400 * 2);
        campaign.setBookingOpenAt(Instant.now().minusSeconds(60));
        campaign.setBookingCloseAt(start.plusSeconds(86400));
        var session = session(start);
        var slot = slot(session, 0, 0);
        em.flush();
        LocalDate day = start.atZone(ZoneId.of("Asia/Colombo")).toLocalDate();
        var result = search.searchAvailableSessions(987654L, campaign.getId(), center.getId(), day, day);
        assertEquals(1, result.size());
        assertEquals(slot.getId(), result.getFirst().slotId());
    }
}
