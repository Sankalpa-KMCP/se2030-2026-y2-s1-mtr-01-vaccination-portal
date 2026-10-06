package com.se2030.vaccinationportal.campaign.session.planning;

import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.session.planning.service.SessionChangePreview;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionChangePreviewTests {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");
    private static final Instant END = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant BOOKING_CLOSE = Instant.parse("2026-10-15T08:00:00Z");

    private SessionScheduleSnapshot oldSnapshot;
    private SessionScheduleSnapshot newSnapshot;

    @BeforeEach
    void setUp() {
        oldSnapshot = new SessionScheduleSnapshot(
                1L, "Asia/Colombo", START, END, BOOKING_CLOSE, SessionLifecycleStatus.DRAFT);
        newSnapshot = new SessionScheduleSnapshot(
                2L, "Europe/London", START.plusSeconds(3600), END.plusSeconds(3600),
                BOOKING_CLOSE.plusSeconds(3600), SessionLifecycleStatus.DRAFT);
    }

    @Test
    void validConstructionInitializesRecordCorrectly() {
        SessionChangePreview preview = new SessionChangePreview(
                10L, SessionChangeType.CENTER_TRANSFERRED, oldSnapshot, newSnapshot, 5, 2, "sample-token");

        assertAll(
                () -> assertEquals(10L, preview.sessionId()),
                () -> assertEquals(SessionChangeType.CENTER_TRANSFERRED, preview.changeType()),
                () -> assertEquals(oldSnapshot, preview.oldSnapshot()),
                () -> assertEquals(newSnapshot, preview.newSnapshot()),
                () -> assertEquals(5, preview.affectedAppointmentCount()),
                () -> assertEquals(2, preview.affectedWaitlistCount()),
                () -> assertEquals("sample-token", preview.previewToken())
        );
    }

    @Test
    void mandatoryFieldsAndInvariantsAreEnforced() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionChangePreview(null, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot, 0, 0, "token")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionChangePreview(10L, null, oldSnapshot, newSnapshot, 0, 0, "token")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionChangePreview(10L, SessionChangeType.RESCHEDULED, null, newSnapshot, 0, 0, "token")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionChangePreview(10L, SessionChangeType.RESCHEDULED, oldSnapshot, null, 0, 0, "token")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionChangePreview(10L, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot, -1, 0, "token")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionChangePreview(10L, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot, 0, -1, "token")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionChangePreview(10L, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot, 0, 0, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionChangePreview(10L, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot, 0, 0, "   "))
        );
    }

    @Test
    void equalsAndHashCodeAdhereToRecordContract() {
        SessionChangePreview preview1 = new SessionChangePreview(
                10L, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot, 3, 1, "tok-1");
        SessionChangePreview preview2 = new SessionChangePreview(
                10L, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot, 3, 1, "tok-1");
        SessionChangePreview preview3 = new SessionChangePreview(
                10L, SessionChangeType.RESCHEDULED, oldSnapshot, newSnapshot, 4, 1, "tok-1");

        assertEquals(preview1, preview2);
        assertEquals(preview1.hashCode(), preview2.hashCode());
        assertNotEquals(preview1, preview3);
    }
}
