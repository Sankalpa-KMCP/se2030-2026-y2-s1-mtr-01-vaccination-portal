package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying domain invariants, validation rules, schedule mutation,
 * and boundary evaluation for {@link OperatingHours} (WBVP-SE2030 / MF-02).
 */
class OperatingHoursModelTests {

    private VaccinationCenter sampleCenter;

    @BeforeEach
    void setUp() {
        sampleCenter = new VaccinationCenter(
                "CTR-OP-TEST",
                "Operating Hours Test Center",
                "123 Clinic Road, Colombo",
                "+94 11 234 5678",
                CenterStatus.ACTIVE,
                200,
                50
        );
    }

    @Test
    @DisplayName("Valid OperatingHours construction with bounded dates passes validation")
    void validOperatingHoursConstructionPasses() {
        OperatingHours opHours = new OperatingHours(
                sampleCenter,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 30),
                LocalTime.of(16, 30),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        );

        assertEquals(sampleCenter, opHours.getCenter());
        assertEquals(DayOfWeek.MONDAY, opHours.getDayOfWeek());
        assertEquals(LocalTime.of(8, 30), opHours.getOpening());
        assertEquals(LocalTime.of(16, 30), opHours.getClosing());
        assertEquals(LocalDate.of(2026, 9, 1), opHours.getEffectiveFrom());
        assertEquals(LocalDate.of(2026, 12, 31), opHours.getEffectiveTo());
    }

    @Test
    @DisplayName("Valid OperatingHours construction with open-ended effectiveTo passes validation")
    void validOpenEndedOperatingHoursPasses() {
        OperatingHours opHours = new OperatingHours(
                sampleCenter,
                DayOfWeek.FRIDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                LocalDate.of(2026, 9, 1)
        );

        assertEquals(sampleCenter, opHours.getCenter());
        assertEquals(DayOfWeek.FRIDAY, opHours.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), opHours.getOpening());
        assertEquals(LocalTime.of(17, 0), opHours.getClosing());
        assertEquals(LocalDate.of(2026, 9, 1), opHours.getEffectiveFrom());
        assertNull(opHours.getEffectiveTo());
    }

    @Test
    @DisplayName("Null center is rejected")
    void nullCenterThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new OperatingHours(
                        null,
                        DayOfWeek.MONDAY,
                        LocalTime.of(8, 0),
                        LocalTime.of(16, 0),
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
        assertTrue(ex.getMessage().contains("Center is required"));
    }

    @Test
    @DisplayName("Null dayOfWeek is rejected")
    void nullDayOfWeekThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new OperatingHours(
                        sampleCenter,
                        null,
                        LocalTime.of(8, 0),
                        LocalTime.of(16, 0),
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
        assertTrue(ex.getMessage().contains("Day of week is required"));
    }

    @Test
    @DisplayName("Null opening time is rejected")
    void nullOpeningTimeThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new OperatingHours(
                        sampleCenter,
                        DayOfWeek.TUESDAY,
                        null,
                        LocalTime.of(16, 0),
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
        assertTrue(ex.getMessage().contains("Opening time is required"));
    }

    @Test
    @DisplayName("Null closing time is rejected")
    void nullClosingTimeThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new OperatingHours(
                        sampleCenter,
                        DayOfWeek.TUESDAY,
                        LocalTime.of(8, 0),
                        null,
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
        assertTrue(ex.getMessage().contains("Closing time is required"));
    }

    @Test
    @DisplayName("Opening time equal to closing time is rejected")
    void openingEqualsClosingThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new OperatingHours(
                        sampleCenter,
                        DayOfWeek.WEDNESDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(9, 0),
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
        assertTrue(ex.getMessage().contains("cannot equal closing time"));
    }

    @Test
    @DisplayName("Opening time after closing time is rejected")
    void openingAfterClosingThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new OperatingHours(
                        sampleCenter,
                        DayOfWeek.WEDNESDAY,
                        LocalTime.of(17, 0),
                        LocalTime.of(9, 0),
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
        assertTrue(ex.getMessage().contains("must be strictly before closing time"));
    }

    @Test
    @DisplayName("Null effectiveFrom date is rejected")
    void nullEffectiveFromThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new OperatingHours(
                        sampleCenter,
                        DayOfWeek.THURSDAY,
                        LocalTime.of(8, 0),
                        LocalTime.of(16, 0),
                        null,
                        LocalDate.of(2026, 12, 31)
                )
        );
        assertTrue(ex.getMessage().contains("Effective from date is required"));
    }

    @Test
    @DisplayName("effectiveTo before effectiveFrom is rejected")
    void effectiveToBeforeEffectiveFromThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new OperatingHours(
                        sampleCenter,
                        DayOfWeek.THURSDAY,
                        LocalTime.of(8, 0),
                        LocalTime.of(16, 0),
                        LocalDate.of(2026, 9, 15),
                        LocalDate.of(2026, 9, 14)
                )
        );
        assertTrue(ex.getMessage().contains("cannot be before effective from date"));
    }

    @Test
    @DisplayName("effectiveTo equal to effectiveFrom is accepted for single-day validity")
    void effectiveToEqualToEffectiveFromPasses() {
        LocalDate sameDay = LocalDate.of(2026, 10, 10);
        OperatingHours opHours = new OperatingHours(
                sampleCenter,
                DayOfWeek.SATURDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                sameDay,
                sameDay
        );

        assertEquals(sameDay, opHours.getEffectiveFrom());
        assertEquals(sameDay, opHours.getEffectiveTo());
        assertTrue(opHours.isEffectiveOn(sameDay));
        assertFalse(opHours.isEffectiveOn(sameDay.plusDays(1)));
    }

    @Test
    @DisplayName("Valid schedule update modifies fields and revalidates successfully")
    void validScheduleUpdateSucceeds() {
        OperatingHours opHours = new OperatingHours(
                sampleCenter,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        opHours.updateSchedule(
                DayOfWeek.TUESDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 31)
        );

        assertEquals(sampleCenter, opHours.getCenter()); // Center remains immutable
        assertEquals(DayOfWeek.TUESDAY, opHours.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), opHours.getOpening());
        assertEquals(LocalTime.of(17, 0), opHours.getClosing());
        assertEquals(LocalDate.of(2026, 10, 1), opHours.getEffectiveFrom());
        assertEquals(LocalDate.of(2026, 12, 31), opHours.getEffectiveTo());
    }

    @Test
    @DisplayName("Invalid schedule update re-runs constraints and throws exception")
    void invalidScheduleUpdateThrowsException() {
        OperatingHours opHours = new OperatingHours(
                sampleCenter,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                opHours.updateSchedule(
                        DayOfWeek.MONDAY,
                        LocalTime.of(14, 0),
                        LocalTime.of(10, 0),
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );
        assertTrue(ex.getMessage().contains("must be strictly before closing time"));
    }

    @Test
    @DisplayName("Setting effectiveTo allows schedule end-dating")
    void setEffectiveToAllowsEndDating() {
        OperatingHours opHours = new OperatingHours(
                sampleCenter,
                DayOfWeek.WEDNESDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );
        assertNull(opHours.getEffectiveTo());

        opHours.setEffectiveTo(LocalDate.of(2026, 9, 30));
        assertEquals(LocalDate.of(2026, 9, 30), opHours.getEffectiveTo());
        assertTrue(opHours.isEffectiveOn(LocalDate.of(2026, 9, 30)));
        assertFalse(opHours.isEffectiveOn(LocalDate.of(2026, 10, 1)));
    }

    @Test
    @DisplayName("isEffectiveOn accurately evaluates date boundaries")
    void isEffectiveOnEvaluatesBoundaries() {
        OperatingHours opHours = new OperatingHours(
                sampleCenter,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        assertFalse(opHours.isEffectiveOn(null));
        assertFalse(opHours.isEffectiveOn(LocalDate.of(2026, 8, 31)));
        assertTrue(opHours.isEffectiveOn(LocalDate.of(2026, 9, 1)));
        assertTrue(opHours.isEffectiveOn(LocalDate.of(2026, 9, 15)));
        assertTrue(opHours.isEffectiveOn(LocalDate.of(2026, 9, 30)));
        assertFalse(opHours.isEffectiveOn(LocalDate.of(2026, 10, 1)));
    }

    @Test
    @DisplayName("isOpenAt accurately evaluates operating time boundaries")
    void isOpenAtEvaluatesTimeBoundaries() {
        OperatingHours opHours = new OperatingHours(
                sampleCenter,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        assertFalse(opHours.isOpenAt(null));
        assertFalse(opHours.isOpenAt(LocalTime.of(7, 59, 59)));
        assertTrue(opHours.isOpenAt(LocalTime.of(8, 0)));
        assertTrue(opHours.isOpenAt(LocalTime.of(12, 0)));
        assertTrue(opHours.isOpenAt(LocalTime.of(15, 59, 59)));
        assertFalse(opHours.isOpenAt(LocalTime.of(16, 0)));
        assertFalse(opHours.isOpenAt(LocalTime.of(16, 1)));
    }

    @Test
    @DisplayName("equals, hashCode, and toString behave consistently")
    void equalsHashCodeToStringConsistency() {
        OperatingHours h1 = new OperatingHours(
                sampleCenter,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );
        OperatingHours h2 = new OperatingHours(
                sampleCenter,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());
        assertTrue(h1.toString().contains("OperatingHours"));
        assertTrue(h1.toString().contains("MONDAY"));
    }
}
