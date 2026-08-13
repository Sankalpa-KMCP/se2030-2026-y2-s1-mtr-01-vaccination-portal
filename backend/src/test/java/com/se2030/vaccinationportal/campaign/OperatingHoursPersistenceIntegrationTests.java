package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
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
 * Integration tests verifying persistence, queries, boundary conditions,
 * and parent locking for {@link OperatingHours} and {@link OperatingHoursRepository}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OperatingHoursPersistenceIntegrationTests {

    @Autowired
    private OperatingHoursRepository operatingHoursRepository;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    private VaccinationCenter centerA;
    private VaccinationCenter centerB;

    @BeforeEach
    void setUp() {
        centerA = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-PERSIST-A",
                "Persistence Test Center A",
                "100 Health Ave, Colombo",
                "+94 11 111 2222",
                CenterStatus.ACTIVE,
                200,
                50,
                "UTC"
        ));

        centerB = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-PERSIST-B",
                "Persistence Test Center B",
                "200 Clinic Road, Kandy",
                "+94 81 333 4444",
                CenterStatus.ACTIVE,
                150,
                40,
                "UTC"
        ));
    }

    @Test
    @DisplayName("Successfully persist OperatingHours and retrieve by ID")
    void persistAndRetrieveOperatingHours() {
        OperatingHours opHours = new OperatingHours(
                centerA,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 30),
                LocalTime.of(16, 30),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        );

        OperatingHours saved = operatingHoursRepository.saveAndFlush(opHours);

        assertNotNull(saved.getId());
        assertEquals(centerA.getId(), saved.getCenter().getId());
        assertEquals(DayOfWeek.MONDAY, saved.getDayOfWeek());
        assertEquals(LocalTime.of(8, 30), saved.getOpening());
        assertEquals(LocalTime.of(16, 30), saved.getClosing());
        assertEquals(LocalDate.of(2026, 9, 1), saved.getEffectiveFrom());
        assertEquals(LocalDate.of(2026, 12, 31), saved.getEffectiveTo());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<OperatingHours> found = operatingHoursRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("Mandatory center foreign key relationship persists and navigates correctly")
    void centerForeignKeyPersistsAndNavigates() {
        OperatingHours opHours = new OperatingHours(
                centerA,
                DayOfWeek.TUESDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                LocalDate.of(2026, 9, 1)
        );

        OperatingHours saved = operatingHoursRepository.saveAndFlush(opHours);
        OperatingHours retrieved = operatingHoursRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(retrieved.getCenter());
        assertEquals(centerA.getId(), retrieved.getCenter().getId());
        assertEquals("CTR-PERSIST-A", retrieved.getCenter().getCode());
    }

    @Test
    @DisplayName("Multiple disjoint split shift records for same center and weekday can coexist at persistence level")
    void multipleSplitShiftsCanCoexistAtPersistenceLevel() {
        // Morning shift: 08:00 - 12:00
        OperatingHours morning = operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA,
                DayOfWeek.WEDNESDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                LocalDate.of(2026, 9, 1)
        ));

        // Afternoon shift: 13:00 - 17:00
        OperatingHours afternoon = operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA,
                DayOfWeek.WEDNESDAY,
                LocalTime.of(13, 0),
                LocalTime.of(17, 0),
                LocalDate.of(2026, 9, 1)
        ));

        assertNotNull(morning.getId());
        assertNotNull(afternoon.getId());
        assertNotEquals(morning.getId(), afternoon.getId());

        List<OperatingHours> wednesdayShifts = operatingHoursRepository.findByCenterIdAndDayOfWeek(
                centerA.getId(), DayOfWeek.WEDNESDAY
        );
        assertEquals(2, wednesdayShifts.size());
    }

    @Test
    @DisplayName("findByCenterId returns only records for the requested center")
    void findByCenterIdFiltersCorrectly() {
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), LocalDate.of(2026, 9, 1)
        ));
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), LocalDate.of(2026, 9, 1)
        ));
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerB, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), LocalDate.of(2026, 9, 1)
        ));

        List<OperatingHours> centerARecords = operatingHoursRepository.findByCenterId(centerA.getId());
        assertEquals(2, centerARecords.size());

        List<OperatingHours> centerBRecords = operatingHoursRepository.findByCenterId(centerB.getId());
        assertEquals(1, centerBRecords.size());
        assertEquals(centerB.getId(), centerBRecords.get(0).getCenter().getId());
    }

    @Test
    @DisplayName("findByCenterIdAndDayOfWeek returns only matching weekday records for the center")
    void findByCenterIdAndDayOfWeekFiltersCorrectly() {
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), LocalDate.of(2026, 9, 1)
        ));
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA, DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), LocalDate.of(2026, 9, 1)
        ));
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerB, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), LocalDate.of(2026, 9, 1)
        ));

        List<OperatingHours> mondayA = operatingHoursRepository.findByCenterIdAndDayOfWeek(
                centerA.getId(), DayOfWeek.MONDAY
        );
        assertEquals(1, mondayA.size());
        assertEquals(DayOfWeek.MONDAY, mondayA.get(0).getDayOfWeek());
        assertEquals(centerA.getId(), mondayA.get(0).getCenter().getId());

        List<OperatingHours> tuesdayA = operatingHoursRepository.findByCenterIdAndDayOfWeek(
                centerA.getId(), DayOfWeek.TUESDAY
        );
        assertTrue(tuesdayA.isEmpty());
    }

    @Test
    @DisplayName("findEffectiveOperatingHours includes record on effectiveFrom start boundary")
    void findEffectiveOperatingHoursOnEffectiveFromBoundary() {
        LocalDate fromDate = LocalDate.of(2026, 9, 1);
        LocalDate toDate = LocalDate.of(2026, 9, 30);

        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), fromDate, toDate
        ));

        List<OperatingHours> effective = operatingHoursRepository.findEffectiveOperatingHours(
                centerA.getId(), DayOfWeek.MONDAY, fromDate
        );
        assertEquals(1, effective.size());
    }

    @Test
    @DisplayName("findEffectiveOperatingHours includes record on effectiveTo end boundary")
    void findEffectiveOperatingHoursOnEffectiveToEndBoundary() {
        LocalDate fromDate = LocalDate.of(2026, 9, 1);
        LocalDate toDate = LocalDate.of(2026, 9, 30);

        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), fromDate, toDate
        ));

        List<OperatingHours> effective = operatingHoursRepository.findEffectiveOperatingHours(
                centerA.getId(), DayOfWeek.MONDAY, toDate
        );
        assertEquals(1, effective.size());
    }

    @Test
    @DisplayName("findEffectiveOperatingHours includes open-ended records with null effectiveTo")
    void findEffectiveOperatingHoursWithOpenEndedEffectiveTo() {
        LocalDate fromDate = LocalDate.of(2026, 9, 1);
        LocalDate evalDate = LocalDate.of(2027, 6, 15);

        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), fromDate, null
        ));

        List<OperatingHours> effective = operatingHoursRepository.findEffectiveOperatingHours(
                centerA.getId(), DayOfWeek.MONDAY, evalDate
        );
        assertEquals(1, effective.size());
    }

    @Test
    @DisplayName("findEffectiveOperatingHours excludes records before effectiveFrom start date")
    void findEffectiveOperatingHoursExcludesBeforeStart() {
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate evalDate = LocalDate.of(2026, 9, 30); // Before effectiveFrom

        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), fromDate, null
        ));

        List<OperatingHours> effective = operatingHoursRepository.findEffectiveOperatingHours(
                centerA.getId(), DayOfWeek.MONDAY, evalDate
        );
        assertTrue(effective.isEmpty());
    }

    @Test
    @DisplayName("findEffectiveOperatingHours excludes records after effectiveTo end date")
    void findEffectiveOperatingHoursExcludesAfterEnd() {
        LocalDate fromDate = LocalDate.of(2026, 9, 1);
        LocalDate toDate = LocalDate.of(2026, 9, 30);
        LocalDate evalDate = LocalDate.of(2026, 10, 1); // After effectiveTo

        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), fromDate, toDate
        ));

        List<OperatingHours> effective = operatingHoursRepository.findEffectiveOperatingHours(
                centerA.getId(), DayOfWeek.MONDAY, evalDate
        );
        assertTrue(effective.isEmpty());
    }

    @Test
    @DisplayName("findEffectiveOperatingHours excludes records for a different weekday")
    void findEffectiveOperatingHoursExcludesDifferentWeekday() {
        LocalDate date = LocalDate.of(2026, 9, 7); // Monday

        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), LocalDate.of(2026, 9, 1), null
        ));

        List<OperatingHours> effective = operatingHoursRepository.findEffectiveOperatingHours(
                centerA.getId(), DayOfWeek.MONDAY, date
        );
        assertTrue(effective.isEmpty());
    }

    @Test
    @DisplayName("findEffectiveOperatingHours returns multiple effective split shifts ordered by opening time ascending")
    void findEffectiveOperatingHoursReturnsSplitShiftsOrderedByOpeningTime() {
        LocalDate evalDate = LocalDate.of(2026, 9, 15);

        // Afternoon shift (inserted first to test ordering): 13:30 - 17:30
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA,
                DayOfWeek.THURSDAY,
                LocalTime.of(13, 30),
                LocalTime.of(17, 30),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        ));

        // Morning shift (inserted second): 08:30 - 12:30
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA,
                DayOfWeek.THURSDAY,
                LocalTime.of(8, 30),
                LocalTime.of(12, 30),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        ));

        // Evening shift (inserted third): 18:00 - 20:00
        operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA,
                DayOfWeek.THURSDAY,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        ));

        List<OperatingHours> effective = operatingHoursRepository.findEffectiveOperatingHours(
                centerA.getId(), DayOfWeek.THURSDAY, evalDate
        );

        assertEquals(3, effective.size());
        assertEquals(LocalTime.of(8, 30), effective.get(0).getOpening());
        assertEquals(LocalTime.of(13, 30), effective.get(1).getOpening());
        assertEquals(LocalTime.of(18, 0), effective.get(2).getOpening());
    }

    @Test
    @DisplayName("findByIdForUpdate acquires pessimistic write lock and retrieves center in active transaction")
    void findByIdForUpdateRetrievesCenterWithPessimisticWriteLock() {
        Optional<VaccinationCenter> locked = centerRepository.findByIdForUpdate(centerA.getId());

        assertTrue(locked.isPresent());
        assertEquals(centerA.getId(), locked.get().getId());
        assertEquals("CTR-PERSIST-A", locked.get().getCode());
        assertEquals(CenterStatus.ACTIVE, locked.get().getCenterStatus());
    }

    @Test
    @DisplayName("findCenterIdById returns parent center ID for an operating hours record")
    void findCenterIdByIdReturnsParentCenterId() {
        OperatingHours opHours = operatingHoursRepository.saveAndFlush(new OperatingHours(
                centerA,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                null
        ));

        Optional<Long> centerIdOpt = operatingHoursRepository.findCenterIdById(opHours.getId());
        assertTrue(centerIdOpt.isPresent());
        assertEquals(centerA.getId(), centerIdOpt.get());

        Optional<Long> missingCenterIdOpt = operatingHoursRepository.findCenterIdById(999999L);
        assertTrue(missingCenterIdOpt.isEmpty());
    }
}
