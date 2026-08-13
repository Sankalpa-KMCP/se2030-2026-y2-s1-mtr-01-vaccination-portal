package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CenterClosureModelTests {

    private static final Instant START = Instant.parse("2026-09-15T08:00:00Z");
    private static final Instant END = Instant.parse("2026-09-15T12:00:00Z");

    private VaccinationCenter center;

    @BeforeEach
    void setUp() {
        center = new VaccinationCenter(
                "CTR-CLOSURE-MODEL", "Closure Model Center", "1 Health Road", "+94 11 000 0000",
                CenterStatus.ACTIVE, 100, 50, "UTC"
        );
    }

    @Test
    void validConstructionTrimsReasonAndDefaultsActive() {
        CenterClosure closure = new CenterClosure(center, START, END, "  Scheduled maintenance  ");

        assertAll(
                () -> assertEquals(center, closure.getCenter()),
                () -> assertEquals(START, closure.getStartAt()),
                () -> assertEquals(END, closure.getEndAt()),
                () -> assertEquals("Scheduled maintenance", closure.getReason()),
                () -> assertEquals(CenterClosureStatus.ACTIVE, closure.getStatus())
        );
    }

    @Test
    void sameDayOvernightAndMultiDayIntervalsAreValid() {
        assertAll(
                () -> new CenterClosure(center, START, END, "Same day"),
                () -> new CenterClosure(
                        center,
                        Instant.parse("2026-09-15T22:00:00Z"),
                        Instant.parse("2026-09-16T05:00:00Z"),
                        "Overnight"
                ),
                () -> new CenterClosure(
                        center,
                        Instant.parse("2026-09-15T08:00:00Z"),
                        Instant.parse("2026-09-18T17:00:00Z"),
                        "Multi-day"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("invalidCandidates")
    void invalidCandidatesAreRejected(Instant startAt, Instant endAt, String reason) {
        assertThrows(IllegalArgumentException.class, () -> new CenterClosure(center, startAt, endAt, reason));
    }

    private static Stream<Arguments> invalidCandidates() {
        return Stream.of(
                Arguments.of(null, END, "Reason"),
                Arguments.of(START, null, "Reason"),
                Arguments.of(START, START, "Reason"),
                Arguments.of(END, START, "Reason"),
                Arguments.of(START, END, null),
                Arguments.of(START, END, "   "),
                Arguments.of(START, END, "x".repeat(256))
        );
    }

    @Test
    void nullCenterIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CenterClosure(null, START, END, "Reason"));
    }

    @Test
    void maximumLengthReasonIsAcceptedAfterTrimming() {
        String reason = "x".repeat(255);
        CenterClosure closure = new CenterClosure(center, START, END, " " + reason + " ");
        assertEquals(reason, closure.getReason());
    }

    @Test
    void cohesiveUpdateSucceeds() {
        CenterClosure closure = new CenterClosure(center, START, END, "Original");
        Instant newStart = Instant.parse("2026-09-16T08:00:00Z");
        Instant newEnd = Instant.parse("2026-09-17T12:00:00Z");

        closure.updateDetails(newStart, newEnd, "  Updated reason  ");

        assertAll(
                () -> assertEquals(newStart, closure.getStartAt()),
                () -> assertEquals(newEnd, closure.getEndAt()),
                () -> assertEquals("Updated reason", closure.getReason())
        );
    }

    @Test
    void invalidUpdatePreservesExistingState() {
        CenterClosure closure = new CenterClosure(center, START, END, "Original");

        assertThrows(IllegalArgumentException.class, () -> closure.updateDetails(END, START, "Changed"));

        assertAll(
                () -> assertEquals(START, closure.getStartAt()),
                () -> assertEquals(END, closure.getEndAt()),
                () -> assertEquals("Original", closure.getReason())
        );
    }

    @Test
    void cancellationIsTerminalAndBlocksUpdates() {
        CenterClosure closure = new CenterClosure(center, START, END, "Original");

        closure.cancel();

        assertEquals(CenterClosureStatus.CANCELLED, closure.getStatus());
        assertThrows(IllegalArgumentException.class, closure::cancel);
        assertThrows(IllegalArgumentException.class, () -> closure.updateDetails(START, END, "Changed"));
    }

    @Test
    void mutationSurfaceHasNoPublicIdentityOwnershipOrAuditSetters() {
        var publicMethods = Arrays.stream(CenterClosure.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .toList();

        assertAll(
                () -> assertFalse(publicMethods.contains("setId")),
                () -> assertFalse(publicMethods.contains("setCenter")),
                () -> assertFalse(publicMethods.contains("setCreatedAt")),
                () -> assertFalse(publicMethods.contains("setUpdatedAt")),
                () -> assertFalse(publicMethods.contains("equals")),
                () -> assertFalse(publicMethods.contains("hashCode")),
                () -> assertTrue(publicMethods.contains("updateDetails")),
                () -> assertTrue(publicMethods.contains("cancel"))
        );
    }
}
