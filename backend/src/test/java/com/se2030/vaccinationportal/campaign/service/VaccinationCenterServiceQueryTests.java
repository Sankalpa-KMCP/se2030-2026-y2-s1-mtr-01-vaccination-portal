package com.se2030.vaccinationportal.campaign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationCenterServiceQueryTests {

    @Autowired
    private VaccinationCenterService centerService;

    @Autowired
    private OperatingHoursService operatingHoursService;

    @Autowired
    private CenterClosureService closureService;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private OperatingHoursRepository operatingHoursRepository;

    @Autowired
    private CenterClosureRepository closureRepository;

    private VaccinationCenter centerA;
    private VaccinationCenter centerB;

    @BeforeEach
    void setUp() {
        centerA = centerService.createCenter(
                "CTR-AAA",
                "Center Alpha",
                "100 Alpha St",
                "+94 11 1111111",
                100,
                50,
                "Asia/Colombo"
        );

        centerB = centerService.createCenter(
                "CTR-BBB",
                "Center Beta",
                "200 Beta St",
                "+94 11 2222222",
                150,
                75,
                "Asia/Colombo"
        );
    }

    @Test
    @DisplayName("getRequiredCenterById returns center when exists")
    void getRequiredCenterByIdSuccess() {
        VaccinationCenter found = centerService.getRequiredCenterById(centerA.getId());
        assertNotNull(found);
        assertEquals("CTR-AAA", found.getCode());
    }

    @Test
    @DisplayName("getRequiredCenterById throws ResourceNotFoundException when missing or null")
    void getRequiredCenterByIdNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> centerService.getRequiredCenterById(999999L));
        assertThrows(ResourceNotFoundException.class, () -> centerService.getRequiredCenterById(null));
    }

    @Test
    @DisplayName("getCenters with null status returns all centers ordered by code ascending")
    void getCentersAllOrdered() {
        List<VaccinationCenter> all = centerService.getCenters(null);
        assertTrue(all.size() >= 2);
        // Verify ascending order by code
        for (int i = 0; i < all.size() - 1; i++) {
            assertTrue(all.get(i).getCode().compareTo(all.get(i + 1).getCode()) <= 0);
        }
    }

    @Test
    @DisplayName("getCenters with status filter returns only matching centers ordered by code ascending")
    void getCentersFilteredOrdered() {
        centerService.deactivateCenter(centerB.getId());

        List<VaccinationCenter> active = centerService.getCenters(CenterStatus.ACTIVE);
        assertTrue(active.stream().allMatch(c -> c.getCenterStatus() == CenterStatus.ACTIVE));
        assertTrue(active.stream().anyMatch(c -> c.getCode().equals("CTR-AAA")));

        List<VaccinationCenter> inactive = centerService.getCenters(CenterStatus.INACTIVE);
        assertTrue(inactive.stream().allMatch(c -> c.getCenterStatus() == CenterStatus.INACTIVE));
        assertTrue(inactive.stream().anyMatch(c -> c.getCode().equals("CTR-BBB")));
    }

    @Test
    @DisplayName("Center mutation methods throw ResourceNotFoundException for nonexistent IDs")
    void centerMutationsNotFound() {
        assertThrows(ResourceNotFoundException.class, () ->
                centerService.updateCenter(999999L, "New Name", "New Addr", "New Contact", 100, 50));
        assertThrows(ResourceNotFoundException.class, () ->
                centerService.activateCenter(999999L));
        assertThrows(ResourceNotFoundException.class, () ->
                centerService.deactivateCenter(999999L));
    }

    @Test
    @DisplayName("OperatingHours methods throw ResourceNotFoundException on missing parent or mismatch")
    void operatingHoursParentHandling() {
        // Create missing parent
        assertThrows(ResourceNotFoundException.class, () ->
                operatingHoursService.createOperatingHours(
                        999999L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), LocalDate.now(), null));

        // List missing parent
        assertThrows(ResourceNotFoundException.class, () ->
                operatingHoursService.getOperatingHoursByCenter(999999L));

        // List existing center with no hours -> empty list
        List<OperatingHours> emptyList = operatingHoursService.getOperatingHoursByCenter(centerA.getId());
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        // Create valid hours for centerA
        OperatingHours hours = operatingHoursService.createOperatingHours(
                centerA.getId(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), LocalDate.now(), null);

        // Update with mismatched parent center ID -> ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () ->
                operatingHoursService.updateOperatingHours(
                        centerB.getId(), hours.getId(), DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), LocalDate.now(), null));

        // End-date with mismatched parent center ID -> ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () ->
                operatingHoursService.endDateOperatingHours(centerB.getId(), hours.getId(), LocalDate.now()));

        // Update with matching parent center ID -> success
        OperatingHours updated = operatingHoursService.updateOperatingHours(
                centerA.getId(), hours.getId(), DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), LocalDate.now(), null);
        assertNotNull(updated);
        assertEquals(LocalTime.of(9, 0), updated.getOpening());
    }

    @Test
    @DisplayName("CenterClosure methods throw ResourceNotFoundException on missing parent or mismatch")
    void centerClosureParentHandling() {
        Instant start = Instant.now().plusSeconds(86400);
        Instant end = Instant.now().plusSeconds(172800);

        // Create missing parent
        assertThrows(ResourceNotFoundException.class, () ->
                closureService.createClosure(999999L, start, end, "Maintenance"));

        // List missing parent
        assertThrows(ResourceNotFoundException.class, () ->
                closureService.getClosuresForCenter(999999L));

        // List existing center with no closures -> empty list
        List<CenterClosure> emptyList = closureService.getClosuresForCenter(centerA.getId());
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        // Create valid closure for centerA
        CenterClosure closure = closureService.createClosure(centerA.getId(), start, end, "Maintenance");

        // Update with mismatched parent center ID -> ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () ->
                closureService.updateFutureClosure(centerB.getId(), closure.getId(), start.plusSeconds(3600), end.plusSeconds(3600), "Updated"));

        // Cancel with mismatched parent center ID -> ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () ->
                closureService.cancelFutureClosure(centerB.getId(), closure.getId()));

        // Cancel with matching parent center ID -> success
        CenterClosure cancelled = closureService.cancelFutureClosure(centerA.getId(), closure.getId());
        assertNotNull(cancelled);
        assertEquals(CenterClosureStatus.CANCELLED, cancelled.getStatus());
    }
}
