package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionChangeEventModelTests {

    private static final Instant OLD_START = Instant.parse("2026-10-15T09:00:00Z");
    private static final Instant OLD_END = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant OLD_BOOKING_CLOSE = Instant.parse("2026-10-15T08:00:00Z");

    private static final Instant NEW_START = Instant.parse("2026-10-15T13:00:00Z");
    private static final Instant NEW_END = Instant.parse("2026-10-15T16:00:00Z");
    private static final Instant NEW_BOOKING_CLOSE = Instant.parse("2026-10-15T12:00:00Z");

    private VaccinationSession session;
    private VaccinationCenter center;
    private SessionScheduleSnapshot oldSnapshot;
    private SessionScheduleSnapshot newSnapshot;

    @BeforeEach
    void setUp() {
        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-AUDIT-M", "Audit Campaign", null,
                new VaccineType("VAX-AUDIT", "Audit Vaccine"),
                new TargetGroup("TG-AUDIT", "Audit Group"),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.DRAFT);
        center = new VaccinationCenter(
                "CTR-AUDIT-M", "Audit Center", "1 Audit Way", "+94 11 000 0000",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo");

        // Use reflection or constructor for center with ID if needed; VaccinationSession takes center
        session = new VaccinationSession(campaign, center, OLD_START, OLD_END, OLD_BOOKING_CLOSE, "Asia/Colombo");

        oldSnapshot = new SessionScheduleSnapshot(
                10L, "Asia/Colombo", OLD_START, OLD_END, OLD_BOOKING_CLOSE, SessionLifecycleStatus.SCHEDULED);
        newSnapshot = new SessionScheduleSnapshot(
                10L, "Asia/Colombo", NEW_START, NEW_END, NEW_BOOKING_CLOSE, SessionLifecycleStatus.SCHEDULED);
    }

    @Test
    void validEventInitializesExpectedState() {
        SessionChangeEvent event = new SessionChangeEvent(
                session,
                SessionChangeType.RESCHEDULED,
                oldSnapshot,
                newSnapshot,
                "  Power outage scheduled in morning  ",
                5,
                2,
                "  admin-user-01  "
        );

        assertAll(
                () -> assertNull(event.getId()),
                () -> assertEquals(session, event.getSession()),
                () -> assertEquals(SessionChangeType.RESCHEDULED, event.getChangeType()),
                () -> assertEquals(oldSnapshot, event.getOldSnapshot()),
                () -> assertEquals(newSnapshot, event.getNewSnapshot()),
                () -> assertEquals("Power outage scheduled in morning", event.getReason()),
                () -> assertEquals(5, event.getAffectedAppointmentCount()),
                () -> assertEquals(2, event.getAffectedWaitlistCount()),
                () -> assertEquals("admin-user-01", event.getActorId()),
                () -> assertNull(event.getCreatedAt())
        );
    }

    @Test
    void nullSessionRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SessionChangeEvent(
                        null,
                        SessionChangeType.RESCHEDULED,
                        oldSnapshot,
                        newSnapshot,
                        "Valid reason",
                        0,
                        0,
                        "actor-1"
                )
        );
        assertEquals("Vaccination session is required", ex.getMessage());
    }

    @Test
    void nullChangeTypeRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SessionChangeEvent(
                        session,
                        null,
                        oldSnapshot,
                        newSnapshot,
                        "Valid reason",
                        0,
                        0,
                        "actor-1"
                )
        );
        assertEquals("Change type is required", ex.getMessage());
    }

    @Test
    void nullOldSnapshotRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SessionChangeEvent(
                        session,
                        SessionChangeType.RESCHEDULED,
                        null,
                        newSnapshot,
                        "Valid reason",
                        0,
                        0,
                        "actor-1"
                )
        );
        assertEquals("Old snapshot is required", ex.getMessage());
    }

    @Test
    void nullNewSnapshotRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SessionChangeEvent(
                        session,
                        SessionChangeType.RESCHEDULED,
                        oldSnapshot,
                        null,
                        "Valid reason",
                        0,
                        0,
                        "actor-1"
                )
        );
        assertEquals("New snapshot is required", ex.getMessage());
    }

    @Test
    void blankReasonRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionChangeEvent(session, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot,
                                null, 0, 0, "actor-1")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionChangeEvent(session, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot,
                                "", 0, 0, "actor-1")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionChangeEvent(session, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot,
                                "   ", 0, 0, "actor-1"))
        );
    }

    @Test
    void excessiveReasonLengthRejected() {
        String longReason = "R".repeat(501);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SessionChangeEvent(session, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot,
                        longReason, 0, 0, "actor-1")
        );
        assertEquals("Reason cannot exceed 500 characters", ex.getMessage());
    }

    @Test
    void negativeCountsRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionChangeEvent(session, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot,
                                "Valid reason", -1, 0, "actor-1")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionChangeEvent(session, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot,
                                "Valid reason", 0, -1, "actor-1"))
        );
    }

    @Test
    void blankActorRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionChangeEvent(session, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot,
                                "Valid reason", 0, 0, null)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionChangeEvent(session, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot,
                                "Valid reason", 0, 0, "")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionChangeEvent(session, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot,
                                "Valid reason", 0, 0, "   "))
        );
    }

    @Test
    void excessiveActorLengthRejected() {
        String longActor = "A".repeat(65);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SessionChangeEvent(session, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot,
                        "Valid reason", 0, 0, longActor)
        );
        assertEquals("Actor ID cannot exceed 64 characters", ex.getMessage());
    }

    @Test
    void eventExposesNoBroadPublicSetters() {
        List<String> setterNames = Arrays.stream(SessionChangeEvent.class.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();

        assertTrue(setterNames.isEmpty(), "SessionChangeEvent must not expose broad public setters: " + setterNames);
    }

    @Test
    void validSnapshotInitializesAllFields() {
        SessionScheduleSnapshot snapshot = new SessionScheduleSnapshot(
                42L, " Asia/Colombo ", OLD_START, OLD_END, OLD_BOOKING_CLOSE, SessionLifecycleStatus.DRAFT);

        assertAll(
                () -> assertEquals(42L, snapshot.getCenterId()),
                () -> assertEquals("Asia/Colombo", snapshot.getScheduleTimezone()),
                () -> assertEquals(OLD_START, snapshot.getStartAt()),
                () -> assertEquals(OLD_END, snapshot.getEndAt()),
                () -> assertEquals(OLD_BOOKING_CLOSE, snapshot.getBookingCloseAt()),
                () -> assertEquals(SessionLifecycleStatus.DRAFT, snapshot.getLifecycleStatus())
        );
    }

    @Test
    void snapshotScheduleValidationRejectsInvalidIntervals() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionScheduleSnapshot(null, "UTC", OLD_START, OLD_END, OLD_BOOKING_CLOSE, SessionLifecycleStatus.DRAFT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionScheduleSnapshot(1L, null, OLD_START, OLD_END, OLD_BOOKING_CLOSE, SessionLifecycleStatus.DRAFT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionScheduleSnapshot(1L, "  ", OLD_START, OLD_END, OLD_BOOKING_CLOSE, SessionLifecycleStatus.DRAFT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionScheduleSnapshot(1L, "Invalid/Timezone", OLD_START, OLD_END, OLD_BOOKING_CLOSE, SessionLifecycleStatus.DRAFT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionScheduleSnapshot(1L, "UTC", null, OLD_END, OLD_BOOKING_CLOSE, SessionLifecycleStatus.DRAFT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionScheduleSnapshot(1L, "UTC", OLD_START, null, OLD_BOOKING_CLOSE, SessionLifecycleStatus.DRAFT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionScheduleSnapshot(1L, "UTC", OLD_END, OLD_START, OLD_BOOKING_CLOSE, SessionLifecycleStatus.DRAFT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionScheduleSnapshot(1L, "UTC", OLD_START, OLD_START, OLD_BOOKING_CLOSE, SessionLifecycleStatus.DRAFT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionScheduleSnapshot(1L, "UTC", OLD_START, OLD_END, null, SessionLifecycleStatus.DRAFT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionScheduleSnapshot(1L, "UTC", OLD_START, OLD_END, OLD_START.plusSeconds(1), SessionLifecycleStatus.DRAFT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new SessionScheduleSnapshot(1L, "UTC", OLD_START, OLD_END, OLD_BOOKING_CLOSE, null))
        );
    }

    @Test
    void snapshotEqualsHashCodeAndToString() {
        SessionScheduleSnapshot s1 = new SessionScheduleSnapshot(
                1L, "UTC", OLD_START, OLD_END, OLD_BOOKING_CLOSE, SessionLifecycleStatus.SCHEDULED);
        SessionScheduleSnapshot s2 = new SessionScheduleSnapshot(
                1L, "UTC", OLD_START, OLD_END, OLD_BOOKING_CLOSE, SessionLifecycleStatus.SCHEDULED);
        SessionScheduleSnapshot s3 = new SessionScheduleSnapshot(
                2L, "UTC", OLD_START, OLD_END, OLD_BOOKING_CLOSE, SessionLifecycleStatus.SCHEDULED);

        assertAll(
                () -> assertEquals(s1, s2),
                () -> assertEquals(s1.hashCode(), s2.hashCode()),
                () -> assertNotEquals(s1, s3),
                () -> assertNotNull(s1.toString())
        );
    }

    @Test
    void snapshotExposesNoBroadPublicSetters() {
        List<String> setterNames = Arrays.stream(SessionScheduleSnapshot.class.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();

        assertTrue(setterNames.isEmpty(), "SessionScheduleSnapshot must not expose broad public setters: " + setterNames);
    }
}
