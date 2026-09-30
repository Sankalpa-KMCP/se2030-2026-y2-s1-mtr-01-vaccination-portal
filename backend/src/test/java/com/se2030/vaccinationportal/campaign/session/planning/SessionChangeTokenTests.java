package com.se2030.vaccinationportal.campaign.session.planning;

import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.session.planning.service.VaccinationSessionService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionChangeTokenTests {

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
    void generatesDeterministicHashForIdenticalInputs() {
        String token1 = VaccinationSessionService.generatePreviewToken(
                10L, oldSnapshot, newSnapshot, SessionChangeType.CENTER_TRANSFERRED, 5, 2);
        String token2 = VaccinationSessionService.generatePreviewToken(
                10L, oldSnapshot, newSnapshot, SessionChangeType.CENTER_TRANSFERRED, 5, 2);

        assertNotNull(token1);
        assertEquals(64, token1.length()); // SHA-256 hex string length
        assertEquals(token1, token2);
    }

    @Test
    void generatesDifferentTokenWhenAnyFieldVaries() {
        String base = VaccinationSessionService.generatePreviewToken(
                10L, oldSnapshot, newSnapshot, SessionChangeType.CENTER_TRANSFERRED, 5, 2);

        // Different session ID
        String diffSessionId = VaccinationSessionService.generatePreviewToken(
                11L, oldSnapshot, newSnapshot, SessionChangeType.CENTER_TRANSFERRED, 5, 2);
        assertNotEquals(base, diffSessionId);

        // Different change type
        String diffType = VaccinationSessionService.generatePreviewToken(
                10L, oldSnapshot, newSnapshot, SessionChangeType.RESCHEDULED, 5, 2);
        assertNotEquals(base, diffType);

        // Different appointment count
        String diffAppt = VaccinationSessionService.generatePreviewToken(
                10L, oldSnapshot, newSnapshot, SessionChangeType.CENTER_TRANSFERRED, 6, 2);
        assertNotEquals(base, diffAppt);

        // Different waitlist count
        String diffWaitlist = VaccinationSessionService.generatePreviewToken(
                10L, oldSnapshot, newSnapshot, SessionChangeType.CENTER_TRANSFERRED, 5, 3);
        assertNotEquals(base, diffWaitlist);

        // Different proposed snapshot (e.g. proposed schedule changed)
        SessionScheduleSnapshot modifiedNewSnapshot = new SessionScheduleSnapshot(
                2L, "Europe/London", START.plusSeconds(7200), END.plusSeconds(7200),
                BOOKING_CLOSE.plusSeconds(7200), SessionLifecycleStatus.DRAFT);
        String diffProposed = VaccinationSessionService.generatePreviewToken(
                10L, oldSnapshot, modifiedNewSnapshot, SessionChangeType.CENTER_TRANSFERRED, 5, 2);
        assertNotEquals(base, diffProposed);

        // Different old snapshot (e.g. underlying session was modified)
        SessionScheduleSnapshot modifiedOldSnapshot = new SessionScheduleSnapshot(
                1L, "Asia/Colombo", START.plusSeconds(1800), END.plusSeconds(1800),
                BOOKING_CLOSE.plusSeconds(1800), SessionLifecycleStatus.DRAFT);
        String diffOld = VaccinationSessionService.generatePreviewToken(
                10L, modifiedOldSnapshot, newSnapshot, SessionChangeType.CENTER_TRANSFERRED, 5, 2);
        assertNotEquals(base, diffOld);
    }

    @Test
    void validatesNonNullParameters() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> VaccinationSessionService.generatePreviewToken(null, oldSnapshot, newSnapshot, SessionChangeType.RESCHEDULED, 0, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> VaccinationSessionService.generatePreviewToken(10L, null, newSnapshot, SessionChangeType.RESCHEDULED, 0, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> VaccinationSessionService.generatePreviewToken(10L, oldSnapshot, null, SessionChangeType.RESCHEDULED, 0, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> VaccinationSessionService.generatePreviewToken(10L, oldSnapshot, newSnapshot, null, 0, 0))
        );
    }
}
