package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.service.OperatingHoursService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying {@link OperatingHoursService} schedule creation,
 * updates, multidimensional conflict enforcement, end-dating/deactivation,
 * deterministic weekday sorting, and date-aware effective hours resolution.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OperatingHoursServiceIntegrationTests {

    @Autowired
    private OperatingHoursService operatingHoursService;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private OperatingHoursRepository operatingHoursRepository;

    private VaccinationCenter activeCenter;
    private VaccinationCenter inactiveCenter;

    @BeforeEach
    void setUp() {
        activeCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-SRV-01",
                "Service Test Active Center",
                "100 Healthcare Blvd, Colombo",
                "+94 11 222 3333",
                CenterStatus.ACTIVE,
                200,
                50
        ));

        inactiveCenter = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-SRV-02",
                "Service Test Inactive Center",
                "200 Closed Lane, Colombo",
                "+94 11 444 5555",
                CenterStatus.INACTIVE,
                100,
                30
        ));
    }

    @Test
    @DisplayName("1. Successfully create valid OperatingHours for an active center")
    void createValidOperatingHoursSuccessfully() {
        OperatingHours created = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 30),
                LocalTime.of(16, 30),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        );

        assertNotNull(created.getId());
        assertEquals(activeCenter.getId(), created.getCenter().getId());
        assertEquals(DayOfWeek.MONDAY, created.getDayOfWeek());
        assertEquals(LocalTime.of(8, 30), created.getOpening());
        assertEquals(LocalTime.of(16, 30), created.getClosing());
        assertEquals(LocalDate.of(2026, 9, 1), created.getEffectiveFrom());
        assertEquals(LocalDate.of(2026, 12, 31), created.getEffectiveTo());
    }

    @Test
    @DisplayName("2. Successfully create OperatingHours for an INACTIVE center")
    void createOperatingHoursForInactiveCenterAllowed() {
        OperatingHours created = operatingHoursService.createOperatingHours(
                inactiveCenter.getId(),
                DayOfWeek.TUESDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        assertNotNull(created.getId());
        assertEquals(inactiveCenter.getId(), created.getCenter().getId());
        assertEquals(CenterStatus.INACTIVE, created.getCenter().getCenterStatus());
    }

    @Test
    @DisplayName("3. Create OperatingHours for nonexistent center throws IllegalArgumentException")
    void createOperatingHoursMissingCenterThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            operatingHoursService.createOperatingHours(
                    999999L,
                    DayOfWeek.MONDAY,
                    LocalTime.of(8, 0),
                    LocalTime.of(16, 0),
                    LocalDate.of(2026, 9, 1),
                    null
            );
        });

        assertTrue(ex.getMessage().contains("Vaccination center not found"));
    }

    @Test
    @DisplayName("4. Multiple disjoint shifts on the same weekday are accepted without conflict")
    void multipleDisjointShiftsSameWeekdayAllowed() {
        OperatingHours morning = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.WEDNESDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        OperatingHours afternoon = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.WEDNESDAY,
                LocalTime.of(13, 0),
                LocalTime.of(17, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        assertNotNull(morning.getId());
        assertNotNull(afternoon.getId());
        assertNotEquals(morning.getId(), afternoon.getId());
    }

    @Test
    @DisplayName("5. Adjacent shifts touching at boundary [08:00-12:00) and [12:00-16:00) are allowed")
    void adjacentShiftsAllowed() {
        OperatingHours shift1 = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.THURSDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        OperatingHours shift2 = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.THURSDAY,
                LocalTime.of(12, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        assertNotNull(shift1.getId());
        assertNotNull(shift2.getId());
    }

    @Test
    @DisplayName("6. Overlapping time intervals with overlapping effective dates are rejected")
    void overlappingTimeAndDateRejected() {
        operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.FRIDAY,
                LocalTime.of(8, 0),
                LocalTime.of(14, 0),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            operatingHoursService.createOperatingHours(
                    activeCenter.getId(),
                    DayOfWeek.FRIDAY,
                    LocalTime.of(12, 0),
                    LocalTime.of(18, 0),
                    LocalDate.of(2026, 9, 15),
                    LocalDate.of(2026, 10, 15)
            );
        });

        assertTrue(ex.getMessage().contains("Operating hours conflict with existing schedule"));
    }

    @Test
    @DisplayName("7. Exact duplicate schedule on same day, date span, and time interval is rejected")
    void exactDuplicateScheduleRejected() {
        operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.SATURDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            operatingHoursService.createOperatingHours(
                    activeCenter.getId(),
                    DayOfWeek.SATURDAY,
                    LocalTime.of(9, 0),
                    LocalTime.of(13, 0),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 12, 31)
            );
        });

        assertTrue(ex.getMessage().contains("Operating hours conflict with existing schedule"));
    }

    @Test
    @DisplayName("8. Identical time intervals in disjoint effective periods are allowed")
    void identicalTimesInDisjointEffectivePeriodsAllowed() {
        OperatingHours period1 = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(15, 0),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30)
        );

        OperatingHours period2 = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(15, 0),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 12, 31)
        );

        assertNotNull(period1.getId());
        assertNotNull(period2.getId());
    }

    @Test
    @DisplayName("9. Identical overlapping times on different weekdays are allowed")
    void sameTimesOnDifferentWeekdaysAllowed() {
        OperatingHours monday = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        OperatingHours tuesday = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.TUESDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        assertNotNull(monday.getId());
        assertNotNull(tuesday.getId());
    }

    @Test
    @DisplayName("10. Successfully update normal schedule fields on an existing record")
    void updateNormalFieldsSuccessfully() {
        OperatingHours opHours = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        OperatingHours updated = operatingHoursService.updateOperatingHours(
                opHours.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 11, 30)
        );

        assertEquals(LocalTime.of(9, 0), updated.getOpening());
        assertEquals(LocalTime.of(17, 0), updated.getClosing());
        assertEquals(LocalDate.of(2026, 9, 15), updated.getEffectiveFrom());
        assertEquals(LocalDate.of(2026, 11, 30), updated.getEffectiveTo());
    }

    @Test
    @DisplayName("11. Successfully update dayOfWeek when new weekday does not conflict")
    void updateDayOfWeekSuccessfully() {
        OperatingHours opHours = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        OperatingHours updated = operatingHoursService.updateOperatingHours(
                opHours.getId(),
                DayOfWeek.WEDNESDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        assertEquals(DayOfWeek.WEDNESDAY, updated.getDayOfWeek());
    }

    @Test
    @DisplayName("12. Update causing conflict is rejected and original persisted state is preserved")
    void updateCausingConflictRejectedAndStatePreserved() {
        // Shift 1 on Monday: 08:00 - 12:00
        OperatingHours shift1 = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        // Shift 2 on Monday: 13:00 - 17:00
        OperatingHours shift2 = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(13, 0),
                LocalTime.of(17, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        // Try updating shift2 to overlap with shift1 (11:00 - 15:00)
        assertThrows(IllegalArgumentException.class, () -> {
            operatingHoursService.updateOperatingHours(
                    shift2.getId(),
                    DayOfWeek.MONDAY,
                    LocalTime.of(11, 0),
                    LocalTime.of(15, 0),
                    LocalDate.of(2026, 9, 1),
                    null
            );
        });

        // Verify shift2 persisted state remained unchanged
        OperatingHours reloaded = operatingHoursService.getOperatingHoursById(shift2.getId()).orElseThrow();
        assertEquals(LocalTime.of(13, 0), reloaded.getOpening());
        assertEquals(LocalTime.of(17, 0), reloaded.getClosing());
    }

    @Test
    @DisplayName("13. Update excludes the record itself from sibling conflict checking")
    void updateExcludesItselfFromConflictChecking() {
        OperatingHours opHours = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        );

        // Updating only the closing time without changing anything else
        OperatingHours updated = operatingHoursService.updateOperatingHours(
                opHours.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        );

        assertEquals(LocalTime.of(17, 0), updated.getClosing());
    }

    @Test
    @DisplayName("14. Successfully end-date an open-ended schedule")
    void endDateOpenEndedScheduleSuccessfully() {
        OperatingHours opHours = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        OperatingHours endDated = operatingHoursService.endDateOperatingHours(
                opHours.getId(),
                LocalDate.of(2026, 10, 31)
        );

        assertEquals(LocalDate.of(2026, 10, 31), endDated.getEffectiveTo());
    }

    @Test
    @DisplayName("15. Successfully shorten a finite period to an earlier valid end date")
    void endDateFinitePeriodToEarlierDateSuccessfully() {
        OperatingHours opHours = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        );

        OperatingHours shortened = operatingHoursService.endDateOperatingHours(
                opHours.getId(),
                LocalDate.of(2026, 10, 15)
        );

        assertEquals(LocalDate.of(2026, 10, 15), shortened.getEffectiveTo());
    }

    @Test
    @DisplayName("16. End-date before effectiveFrom is rejected with IllegalArgumentException")
    void endDateBeforeEffectiveFromRejected() {
        OperatingHours opHours = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        assertThrows(IllegalArgumentException.class, () -> {
            operatingHoursService.endDateOperatingHours(
                    opHours.getId(),
                    LocalDate.of(2026, 8, 31)
            );
        });
    }

    @Test
    @DisplayName("17. End-date extension beyond an existing finite effectiveTo is rejected")
    void endDateExtensionBeyondCurrentEffectiveToRejected() {
        OperatingHours opHours = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 31)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            operatingHoursService.endDateOperatingHours(
                    opHours.getId(),
                    LocalDate.of(2026, 11, 1)
            );
        });

        assertTrue(ex.getMessage().contains("cannot extend beyond current effective to date"));
    }

    @Test
    @DisplayName("18. getOperatingHoursByCenter returns records sorted deterministically Monday->Sunday then opening/effective date")
    void listCenterHoursDeterministicCalendarOrdering() {
        // Friday shift
        operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.FRIDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        // Monday afternoon shift: 13:00
        operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(13, 0),
                LocalTime.of(17, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        // Monday morning shift: 08:00
        operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        // Tuesday shift
        operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.TUESDAY,
                LocalTime.of(8, 30),
                LocalTime.of(16, 30),
                LocalDate.of(2026, 9, 1),
                null
        );

        List<OperatingHours> calendar = operatingHoursService.getOperatingHoursByCenter(activeCenter.getId());

        assertEquals(4, calendar.size());
        assertEquals(DayOfWeek.MONDAY, calendar.get(0).getDayOfWeek());
        assertEquals(LocalTime.of(8, 0), calendar.get(0).getOpening());

        assertEquals(DayOfWeek.MONDAY, calendar.get(1).getDayOfWeek());
        assertEquals(LocalTime.of(13, 0), calendar.get(1).getOpening());

        assertEquals(DayOfWeek.TUESDAY, calendar.get(2).getDayOfWeek());
        assertEquals(DayOfWeek.FRIDAY, calendar.get(3).getDayOfWeek());
    }

    @Test
    @DisplayName("19. getEffectiveOperatingHours derives weekday from date and returns only effective records")
    void effectiveLookupDerivesWeekdayAndFiltersDates() {
        LocalDate dateMonday = LocalDate.of(2026, 9, 7); // Monday

        operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.TUESDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        List<OperatingHours> effective = operatingHoursService.getEffectiveOperatingHours(
                activeCenter.getId(),
                dateMonday
        );

        assertEquals(1, effective.size());
        assertEquals(DayOfWeek.MONDAY, effective.get(0).getDayOfWeek());
    }

    @Test
    @DisplayName("20. getEffectiveOperatingHours preserves split-shift opening time ascending order")
    void effectiveLookupPreservesSplitShiftOrder() {
        LocalDate dateWed = LocalDate.of(2026, 9, 9); // Wednesday

        // Afternoon shift created first
        operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.WEDNESDAY,
                LocalTime.of(13, 30),
                LocalTime.of(17, 30),
                LocalDate.of(2026, 9, 1),
                null
        );

        // Morning shift created second
        operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.WEDNESDAY,
                LocalTime.of(8, 30),
                LocalTime.of(12, 30),
                LocalDate.of(2026, 9, 1),
                null
        );

        List<OperatingHours> effective = operatingHoursService.getEffectiveOperatingHours(
                activeCenter.getId(),
                dateWed
        );

        assertEquals(2, effective.size());
        assertEquals(LocalTime.of(8, 30), effective.get(0).getOpening());
        assertEquals(LocalTime.of(13, 30), effective.get(1).getOpening());
    }

    @Test
    @DisplayName("getOperatingHoursById returns entity when found and empty Optional when absent")
    void getOperatingHoursByIdBehavior() {
        OperatingHours opHours = operatingHoursService.createOperatingHours(
                activeCenter.getId(),
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        );

        Optional<OperatingHours> found = operatingHoursService.getOperatingHoursById(opHours.getId());
        assertTrue(found.isPresent());
        assertEquals(opHours.getId(), found.get().getId());

        Optional<OperatingHours> missing = operatingHoursService.getOperatingHoursById(999999L);
        assertTrue(missing.isEmpty());
    }

    @Test
    @DisplayName("Null parameter validation on service methods throws IllegalArgumentException")
    void nullParameterValidationsThrowException() {
        assertThrows(IllegalArgumentException.class, () -> operatingHoursService.createOperatingHours(
                null, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), LocalDate.of(2026, 9, 1), null
        ));

        assertThrows(IllegalArgumentException.class, () -> operatingHoursService.updateOperatingHours(
                null, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), LocalDate.of(2026, 9, 1), null
        ));

        assertThrows(IllegalArgumentException.class, () -> operatingHoursService.endDateOperatingHours(
                null, LocalDate.of(2026, 9, 30)
        ));

        assertThrows(IllegalArgumentException.class, () -> operatingHoursService.endDateOperatingHours(
                1L, null
        ));

        assertThrows(IllegalArgumentException.class, () -> operatingHoursService.getOperatingHoursById(null));

        assertThrows(IllegalArgumentException.class, () -> operatingHoursService.getOperatingHoursByCenter(null));

        assertThrows(IllegalArgumentException.class, () -> operatingHoursService.getEffectiveOperatingHours(null, LocalDate.now()));

        assertThrows(IllegalArgumentException.class, () -> operatingHoursService.getEffectiveOperatingHours(1L, null));
    }
}
