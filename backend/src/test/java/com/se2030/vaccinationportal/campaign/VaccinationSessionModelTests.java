package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaccinationSessionModelTests {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");
    private static final Instant END = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant BOOKING_CLOSE = Instant.parse("2026-10-15T08:00:00Z");
    private static final LocalDate ORIGIN_DATE = LocalDate.of(2026, 10, 15);
    private static final String OCCURRENCE_KEY = "REC-RULE-UUID-20261015";

    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private SessionRecurrenceRule recurrenceRule;

    @BeforeEach
    void setUp() {
        campaign = CampaignTestFixtures.campaign(
                "CMP-SESS-M", "Session Model Campaign", null,
                new VaccineType("VAX-SESS-M", "Session Vaccine"),
                new TargetGroup("TG-SESS-M", "Session Group"),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.DRAFT);
        center = new VaccinationCenter(
                "CTR-SESS-M", "Session Center", "1 Session Road", "+94 11 000 0000",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo");
        recurrenceRule = new SessionRecurrenceRule(
                campaign, center, Set.of(DayOfWeek.THURSDAY),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "Asia/Colombo");
    }

    @Test
    void validOneOffConstructionInitializesExpectedState() {
        VaccinationSession session = new VaccinationSession(
                campaign, center, START, END, BOOKING_CLOSE, " Asia/Colombo ");

        assertAll(
                () -> assertNull(session.getId()),
                () -> assertEquals(campaign, session.getCampaign()),
                () -> assertEquals(center, session.getCenter()),
                () -> assertNull(session.getRecurrenceRule()),
                () -> assertNull(session.getOccurrenceKey()),
                () -> assertNull(session.getOriginOccurrenceDate()),
                () -> assertFalse(session.isGenerated()),
                () -> assertEquals(START, session.getStartAt()),
                () -> assertEquals(END, session.getEndAt()),
                () -> assertEquals(BOOKING_CLOSE, session.getBookingCloseAt()),
                () -> assertEquals("Asia/Colombo", session.getScheduleTimezone()),
                () -> assertEquals(SessionLifecycleStatus.DRAFT, session.getLifecycleStatus()),
                () -> assertNull(session.getCreatedAt()),
                () -> assertNull(session.getUpdatedAt())
        );
    }

    @Test
    void validGeneratedOccurrenceConstructionInitializesExpectedProvenance() {
        VaccinationSession session = new VaccinationSession(
                campaign, center, recurrenceRule, OCCURRENCE_KEY, ORIGIN_DATE,
                START, END, BOOKING_CLOSE, "UTC");

        assertAll(
                () -> assertNull(session.getId()),
                () -> assertEquals(campaign, session.getCampaign()),
                () -> assertEquals(center, session.getCenter()),
                () -> assertEquals(recurrenceRule, session.getRecurrenceRule()),
                () -> assertEquals(OCCURRENCE_KEY, session.getOccurrenceKey()),
                () -> assertEquals(ORIGIN_DATE, session.getOriginOccurrenceDate()),
                () -> assertTrue(session.isGenerated()),
                () -> assertEquals(START, session.getStartAt()),
                () -> assertEquals(END, session.getEndAt()),
                () -> assertEquals(BOOKING_CLOSE, session.getBookingCloseAt()),
                () -> assertEquals("UTC", session.getScheduleTimezone()),
                () -> assertEquals(SessionLifecycleStatus.DRAFT, session.getLifecycleStatus())
        );
    }

    @Test
    void mandatoryCampaignAndCenterAreEnforced() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(null, center, START, END, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, null, START, END, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(null, center, recurrenceRule, OCCURRENCE_KEY, ORIGIN_DATE,
                                START, END, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, null, recurrenceRule, OCCURRENCE_KEY, ORIGIN_DATE,
                                START, END, BOOKING_CLOSE, "UTC"))
        );
    }

    @Test
    void mandatoryAndNormalizedScheduleTimezoneIsEnforced() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "  ")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Invalid/Zone_Name")),
                () -> {
                    VaccinationSession session = new VaccinationSession(
                            campaign, center, START, END, BOOKING_CLOSE, " Europe/London ");
                    assertEquals("Europe/London", session.getScheduleTimezone());
                }
        );
    }

    @Test
    void sessionIntervalInvariantsAreEnforced() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, null, END, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, START, null, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, START, START, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, END, START, BOOKING_CLOSE, "UTC"))
        );
    }

    @Test
    void bookingCloseTimestampInvariantsAreEnforced() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, START, END, null, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, START, END, START.plusSeconds(1), "UTC")),
                () -> {
                    VaccinationSession equalClose = new VaccinationSession(
                            campaign, center, START, END, START, "UTC");
                    assertEquals(START, equalClose.getBookingCloseAt());
                }
        );
    }

    @Test
    void recurrenceProvenanceEnforcesAllOrNone() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, recurrenceRule, null, ORIGIN_DATE,
                                START, END, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, recurrenceRule, OCCURRENCE_KEY, null,
                                START, END, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, null, OCCURRENCE_KEY, ORIGIN_DATE,
                                START, END, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, null, OCCURRENCE_KEY, null,
                                START, END, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, null, null, ORIGIN_DATE,
                                START, END, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, recurrenceRule, null, null,
                                START, END, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, recurrenceRule, "   ", ORIGIN_DATE,
                                START, END, BOOKING_CLOSE, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new VaccinationSession(campaign, center, recurrenceRule, "K".repeat(65), ORIGIN_DATE,
                                START, END, BOOKING_CLOSE, "UTC"))
        );
    }

    @Test
    void rescheduleUpdatesScheduleAndCenterWhenInDraftOrScheduled() {
        VaccinationSession draftSession = new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo");
        VaccinationCenter newCenter = new VaccinationCenter(
                "CTR-SESS-M2", "New Center", "2 Session Road", "+94 11 000 0001",
                CenterStatus.ACTIVE, 100, 50, "Europe/London");
        Instant newStart = START.plusSeconds(3600);
        Instant newEnd = END.plusSeconds(3600);
        Instant newBookingClose = BOOKING_CLOSE.plusSeconds(1800);

        draftSession.reschedule(newCenter, newStart, newEnd, newBookingClose);

        assertEquals(newCenter, draftSession.getCenter());
        assertEquals("Europe/London", draftSession.getScheduleTimezone());
        assertEquals(newStart, draftSession.getStartAt());
        assertEquals(newEnd, draftSession.getEndAt());
        assertEquals(newBookingClose, draftSession.getBookingCloseAt());

        // Test with SCHEDULED
        draftSession.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        draftSession.reschedule(center, START, END, BOOKING_CLOSE);
        assertEquals(center, draftSession.getCenter());
        assertEquals("Asia/Colombo", draftSession.getScheduleTimezone());
    }

    @Test
    void rescheduleRejectsInvalidLifecycleStatusAndInvalidParameters() {
        VaccinationSession session = new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo");

        // Null center
        assertThrows(IllegalArgumentException.class, () -> session.reschedule(null, START, END, BOOKING_CLOSE));

        // Invalid schedule interval
        assertThrows(IllegalArgumentException.class, () -> session.reschedule(center, END, START, BOOKING_CLOSE));

        // Booking close after start
        assertThrows(IllegalArgumentException.class, () -> session.reschedule(center, START, END, START.plusSeconds(1)));

        // IN_PROGRESS rejection
        VaccinationSession session1 = new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo");
        session1.schedule();
        session1.start();
        assertThrows(IllegalArgumentException.class, () -> session1.reschedule(center, START, END, BOOKING_CLOSE));

        // COMPLETED rejection
        VaccinationSession session2 = new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo");
        session2.schedule();
        session2.start();
        session2.complete();
        assertThrows(IllegalArgumentException.class, () -> session2.reschedule(center, START, END, BOOKING_CLOSE));

        // CANCELLED rejection
        VaccinationSession session3 = new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo");
        session3.cancel();
        assertThrows(IllegalArgumentException.class, () -> session3.reschedule(center, START, END, BOOKING_CLOSE));
    }

    @Test
    void cancelTransitionsDraftAndScheduledToCancelled() {
        VaccinationSession draftSession = new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo");
        draftSession.cancel();
        assertEquals(SessionLifecycleStatus.CANCELLED, draftSession.getLifecycleStatus());

        VaccinationSession scheduledSession = new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo");
        scheduledSession.schedule();
        scheduledSession.cancel();
        assertEquals(SessionLifecycleStatus.CANCELLED, scheduledSession.getLifecycleStatus());
    }

    @Test
    void cancelRejectsInvalidLifecycleStatusAndAlreadyCancelled() {
        VaccinationSession session = new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo");

        // Cancelled once
        session.cancel();
        assertThrows(IllegalStateException.class, session::cancel);

        // IN_PROGRESS rejection
        VaccinationSession inProgressSession = new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo");
        inProgressSession.schedule();
        inProgressSession.start();
        assertThrows(IllegalStateException.class, inProgressSession::cancel);

        // COMPLETED rejection
        VaccinationSession completedSession = new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo");
        completedSession.schedule();
        completedSession.start();
        completedSession.complete();
        assertThrows(IllegalStateException.class, completedSession::cancel);
    }

    @Test
    void transitionLifecycleStatusValidatesNonNull() {
        VaccinationSession session = new VaccinationSession(campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo");
        assertThrows(IllegalArgumentException.class, () -> session.transitionLifecycleStatus(null));
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        assertEquals(SessionLifecycleStatus.SCHEDULED, session.getLifecycleStatus());
    }

    @Test
    void entityExposesNoBroadPublicSetters() {
        List<String> setterNames = Arrays.stream(VaccinationSession.class.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();

        assertTrue(setterNames.isEmpty(), "VaccinationSession must not expose broad public setters: " + setterNames);
    }
}
