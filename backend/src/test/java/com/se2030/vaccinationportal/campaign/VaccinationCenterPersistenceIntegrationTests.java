package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.service.VaccinationCenterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying VaccinationCenter persistence, uniqueness constraints,
 * status filtering, and service-level lifecycle management (WBVP-SE2030 section 6.1 / 12).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationCenterPersistenceIntegrationTests {

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private VaccinationCenterService centerService;

    @BeforeEach
    void setUp() {
        // Clear any previous state if needed (transactional rollback will clean up per test)
    }

    @Test
    @DisplayName("Successfully persist VaccinationCenter and retrieve by code")
    void persistAndRetrieveVaccinationCenter() {
        VaccinationCenter center = new VaccinationCenter(
                "CTR-NCMC-01",
                "National Children's Medical Center",
                "123 Healthcare Ave, Colombo 07",
                "+94 11 2345678",
                CenterStatus.ACTIVE,
                150,
                50,
                "Asia/Colombo"
        );

        VaccinationCenter saved = centerRepository.saveAndFlush(center);

        assertNotNull(saved.getId());
        assertEquals("CTR-NCMC-01", saved.getCode());
        assertEquals("National Children's Medical Center", saved.getName());
        assertEquals("123 Healthcare Ave, Colombo 07", saved.getAddress());
        assertEquals("+94 11 2345678", saved.getContact());
        assertEquals(CenterStatus.ACTIVE, saved.getCenterStatus());
        assertEquals(150, saved.getSafeConcurrentOccupancy());
        assertEquals(50, saved.getSessionServiceCeiling());
        assertEquals("Asia/Colombo", saved.getTimezone());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<VaccinationCenter> found = centerRepository.findByCode("CTR-NCMC-01");
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertTrue(centerRepository.existsByCode("CTR-NCMC-01"));
    }

    @Test
    @DisplayName("Duplicate center code violates database unique constraint")
    void duplicateCenterCodeViolatesUniqueConstraint() {
        VaccinationCenter c1 = new VaccinationCenter(
                "CTR-DUP-01",
                "Center One",
                "Address One",
                "Contact One",
                CenterStatus.ACTIVE,
                100,
                null,
                "UTC"
        );
        centerRepository.saveAndFlush(c1);

        VaccinationCenter c2 = new VaccinationCenter(
                "CTR-DUP-01",
                "Center Two",
                "Address Two",
                "Contact Two",
                CenterStatus.ACTIVE,
                120,
                null,
                "UTC"
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            centerRepository.saveAndFlush(c2);
        });
    }

    @Test
    @DisplayName("Query centers by CenterStatus filters active and inactive centers")
    void queryCentersByCenterStatus() {
        centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-ACT-01", "Active Center 1", "Addr 1", "Cont 1", CenterStatus.ACTIVE, 100, null, "UTC"
        ));
        centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-ACT-02", "Active Center 2", "Addr 2", "Cont 2", CenterStatus.ACTIVE, 150, 40, "UTC"
        ));
        centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-INACT-01", "Inactive Center", "Addr 3", "Cont 3", CenterStatus.INACTIVE, 80, null, "UTC"
        ));

        List<VaccinationCenter> activeCenters = centerRepository.findByCenterStatus(CenterStatus.ACTIVE);
        assertEquals(2, activeCenters.size());

        List<VaccinationCenter> inactiveCenters = centerRepository.findByCenterStatus(CenterStatus.INACTIVE);
        assertEquals(1, inactiveCenters.size());
        assertEquals("CTR-INACT-01", inactiveCenters.get(0).getCode());
    }

    @Test
    @DisplayName("Service creates center with normalized uppercase code and default ACTIVE status")
    void serviceCreatesCenterSuccessfully() {
        VaccinationCenter center = centerService.createCenter(
                "  ctr-kandy-gen  ",
                "Kandy General Hospital Center",
                "William Gopallawa Mawatha, Kandy",
                "+94 81 2222222",
                200,
                60,
                "  Asia/Colombo  "
        );

        assertNotNull(center.getId());
        assertEquals("CTR-KANDY-GEN", center.getCode());
        assertEquals(CenterStatus.ACTIVE, center.getCenterStatus());
        assertEquals("Asia/Colombo", center.getTimezone());

        Optional<VaccinationCenter> lookup = centerService.getCenterByCode("ctr-kandy-gen");
        assertTrue(lookup.isPresent());
        assertEquals(center.getId(), lookup.get().getId());
    }

    @Test
    @DisplayName("Service rejects creation when center code already exists")
    void serviceRejectsDuplicateCenterCode() {
        centerService.createCenter(
                "CTR-GALLE-01",
                "Galle General Hospital",
                "Karapitiya, Galle",
                "+94 91 2234567",
                120,
                null,
                "UTC"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                centerService.createCenter(
                        "ctr-galle-01",
                        "Another Center Galle",
                        "Different Address",
                        "Different Contact",
                        100,
                        null,
                        "UTC"
                )
        );
        assertTrue(ex.getMessage().contains("Vaccination center already exists with code"));
    }

    @Test
    @DisplayName("Service updates editable master attributes while preserving center code")
    void serviceUpdatesEditableAttributesPreservingCode() {
        VaccinationCenter created = centerService.createCenter(
                "CTR-JAFFNA-01",
                "Jaffna Teaching Hospital",
                "Hospital Rd, Jaffna",
                "+94 21 2222222",
                100,
                30,
                "Asia/Colombo"
        );

        VaccinationCenter updated = centerService.updateCenter(
                created.getId(),
                "Jaffna Central Hospital Vaccination Center",
                "Main St, Jaffna",
                "+94 21 3333333",
                140,
                45
        );

        assertEquals("CTR-JAFFNA-01", updated.getCode(), "Center code must remain unchanged during update");
        assertEquals("Jaffna Central Hospital Vaccination Center", updated.getName());
        assertEquals("Main St, Jaffna", updated.getAddress());
        assertEquals("+94 21 3333333", updated.getContact());
        assertEquals(140, updated.getSafeConcurrentOccupancy());
        assertEquals(45, updated.getSessionServiceCeiling());
        assertEquals("Asia/Colombo", updated.getTimezone());
    }

    @Test
    @DisplayName("Service activates and deactivates center lifecycle safely")
    void serviceActivatesAndDeactivatesCenter() {
        VaccinationCenter center = centerService.createCenter(
                "CTR-MATARA-01",
                "Matara District General Hospital",
                "Anagarika Dharmapala Mawatha, Matara",
                "+94 41 2222222",
                80,
                null,
                "UTC"
        );
        assertEquals(CenterStatus.ACTIVE, center.getCenterStatus());

        // Deactivate center
        VaccinationCenter deactivated = centerService.deactivateCenter(center.getId());
        assertEquals(CenterStatus.INACTIVE, deactivated.getCenterStatus());

        // Reactivate center
        VaccinationCenter reactivated = centerService.activateCenter(center.getId());
        assertEquals(CenterStatus.ACTIVE, reactivated.getCenterStatus());
    }

    @Test
    @DisplayName("Service exposes no hard-delete workflow and retains inactive centers in database")
    void centersAreRetainedWithoutHardDelete() {
        VaccinationCenter center = centerService.createCenter(
                "CTR-NEGOMBO-01",
                "Negombo General Hospital",
                "Colombo Rd, Negombo",
                "+94 31 2222222",
                90,
                null,
                "UTC"
        );

        centerService.deactivateCenter(center.getId());

        List<VaccinationCenter> all = centerService.getAllCenters();
        assertEquals(1, all.size());
        assertEquals("CTR-NEGOMBO-01", all.get(0).getCode());
        assertEquals(CenterStatus.INACTIVE, all.get(0).getCenterStatus());
    }

    @Test
    @DisplayName("Service rejects an invalid explicitly supplied scheduling timezone")
    void serviceRejectsInvalidSchedulingTimezone() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                centerService.createCenter(
                        "CTR-INVALID-TZ",
                        "Invalid timezone center",
                        "1 Test Road",
                        "+94 11 000 0000",
                        100,
                        null,
                        "Invalid/Timezone")
        );

        assertTrue(error.getMessage().contains("Center scheduling timezone is invalid"));
        assertFalse(centerRepository.existsByCode("CTR-INVALID-TZ"));
    }
}
