package com.se2030.vaccinationportal.campaign.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TimeSlotInvariantTests {

    private VaccinationSession session;
    private Instant startTime;
    private Instant endTime;

    @BeforeEach
    void setUp() {
        session = mock(VaccinationSession.class);
        startTime = Instant.parse("2026-09-10T08:00:00Z");
        endTime = Instant.parse("2026-09-10T09:00:00Z");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("Capacity must be strictly positive")
    void capacityMustBeStrictlyPositive(int invalidCapacity) {
        assertThrows(IllegalArgumentException.class, () ->
                new TimeSlot(session, startTime, endTime, invalidCapacity)
        );
    }

    @Test
    @DisplayName("Reserved count cannot be negative")
    void reservedCountCannotBeNegative() {
        TimeSlot slot = new TimeSlot(session, startTime, endTime, 5);

        assertThrows(IllegalArgumentException.class, () -> slot.setReservedCount(-1));
        assertEquals(0, slot.getReservedCount());
    }

    @Test
    @DisplayName("Reserved count cannot exceed capacity")
    void reservedCountCannotExceedCapacity() {
        TimeSlot slot = new TimeSlot(session, startTime, endTime, 5);

        assertThrows(IllegalArgumentException.class, () -> slot.setReservedCount(6));
        assertEquals(0, slot.getReservedCount());
    }

    @Test
    @DisplayName("Start time must be strictly before end time")
    void startTimeMustBeStrictlyBeforeEndTime() {
        assertThrows(IllegalArgumentException.class, () ->
                new TimeSlot(session, startTime, startTime, 5)
        );
        assertThrows(IllegalArgumentException.class, () ->
                new TimeSlot(session, startTime, startTime.minusSeconds(1), 5)
        );
    }

    @Test
    @DisplayName("Time setters cannot create an invalid time range")
    void timeSettersCannotCreateInvalidRange() {
        TimeSlot slot = new TimeSlot(session, startTime, endTime, 5);

        assertThrows(IllegalArgumentException.class, () -> slot.setStartTime(endTime));
        assertThrows(IllegalArgumentException.class, () -> slot.setEndTime(startTime));
        assertEquals(startTime, slot.getStartTime());
        assertEquals(endTime, slot.getEndTime());
    }

    @Test
    @DisplayName("Valid slot is accepted")
    void validSlotIsAccepted() {
        TimeSlot slot = assertDoesNotThrow(() -> new TimeSlot(session, startTime, endTime, 5));

        assertEquals(5, slot.getCapacity());
        assertEquals(0, slot.getReservedCount());
    }

    @Test
    @DisplayName("Capacity cannot be lowered below reserved count")
    void capacityCannotBeLoweredBelowReservedCount() {
        TimeSlot slot = new TimeSlot(session, startTime, endTime, 5);
        slot.setReservedCount(3);

        assertThrows(IllegalArgumentException.class, () -> slot.setCapacity(2));
        assertEquals(5, slot.getCapacity());
        assertEquals(3, slot.getReservedCount());
    }
}
