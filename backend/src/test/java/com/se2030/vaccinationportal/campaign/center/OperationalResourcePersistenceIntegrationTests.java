package com.se2030.vaccinationportal.campaign.center;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OperationalResourcePersistenceIntegrationTests {

    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private VaccinationCenterRepository centerRepository;

    private VaccinationCenter centerA;
    private VaccinationCenter centerB;

    @BeforeEach
    void setUp() {
        centerA = centerRepository.save(new VaccinationCenter("CTR-RES-A", "Resource Center A", "100 Med St", "+1000", CenterStatus.ACTIVE, 40, 80, "UTC"));
        centerB = centerRepository.save(new VaccinationCenter("CTR-RES-B", "Resource Center B", "200 Med St", "+2000", CenterStatus.ACTIVE, 30, 60, "UTC"));
    }

    @Test
    void persistsResourceAndPessimisticLockQuery() {
        OperationalResource saved = resourceRepository.saveAndFlush(
                new OperationalResource(centerA, "STATION-01", ResourceType.VACCINATION_STATION, "Vaccination Station 1")
        );

        OperationalResource reloaded = resourceRepository.findById(saved.getId()).orElseThrow();
        assertEquals("STATION-01", reloaded.getCode());
        assertEquals(ResourceType.VACCINATION_STATION, reloaded.getResourceType());
        assertEquals(OperationalResourceStatus.ACTIVE, reloaded.getStatus());
        assertNotNull(reloaded.getCreatedAt());
        assertNotNull(reloaded.getUpdatedAt());

        // Pessimistic write lock query
        OperationalResource locked = resourceRepository.findByIdForUpdate(saved.getId()).orElseThrow();
        assertEquals(saved.getId(), locked.getId());
    }

    @Test
    void enforcesUniqueCodePerCenter_rejectsDuplicate() {
        resourceRepository.saveAndFlush(
                new OperationalResource(centerA, "ROOM-01", ResourceType.ROOM, "Consultation Room 1")
        );

        // Same code at center A must fail
        assertThrows(DataIntegrityViolationException.class, () -> {
            resourceRepository.saveAndFlush(
                    new OperationalResource(centerA, "ROOM-01", ResourceType.ROOM, "Duplicate Room 1")
            );
        });
    }

    @Test
    void permitsSameCodeAtDifferentCenters() {
        resourceRepository.saveAndFlush(
                new OperationalResource(centerA, "ROOM-01", ResourceType.ROOM, "Center A Room 1")
        );

        // Same code at different center B is permitted
        OperationalResource atCenterB = resourceRepository.saveAndFlush(
                new OperationalResource(centerB, "ROOM-01", ResourceType.ROOM, "Center B Room 1")
        );
        assertNotNull(atCenterB.getId());
    }

    @Test
    void persistsMaintenanceStatusCorrectly() {
        OperationalResource maintenanceStation = new OperationalResource(
                centerA,
                "MAINT-01",
                ResourceType.VACCINATION_STATION,
                "Station in Maintenance",
                OperationalResourceStatus.MAINTENANCE
        );
        OperationalResource saved = resourceRepository.saveAndFlush(maintenanceStation);

        OperationalResource reloaded = resourceRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OperationalResourceStatus.MAINTENANCE, reloaded.getStatus());
    }

    @Test
    void findByCenterAndTypeWithoutStatusFilter_returnsAllStatuses() {
        resourceRepository.saveAndFlush(new OperationalResource(centerA, "ST-01", ResourceType.VACCINATION_STATION, "Station 1", OperationalResourceStatus.ACTIVE));
        resourceRepository.saveAndFlush(new OperationalResource(centerA, "ST-02", ResourceType.VACCINATION_STATION, "Station 2", OperationalResourceStatus.MAINTENANCE));
        resourceRepository.saveAndFlush(new OperationalResource(centerA, "ST-03", ResourceType.VACCINATION_STATION, "Station 3", OperationalResourceStatus.INACTIVE));
        resourceRepository.saveAndFlush(new OperationalResource(centerA, "RM-01", ResourceType.ROOM, "Room 1", OperationalResourceStatus.ACTIVE));

        List<OperationalResource> allStations = resourceRepository.findByCenterIdAndResourceTypeOrderByCodeAsc(
                centerA.getId(),
                ResourceType.VACCINATION_STATION
        );
        assertEquals(3, allStations.size());
        assertEquals("ST-01", allStations.get(0).getCode());
        assertEquals("ST-02", allStations.get(1).getCode());
        assertEquals("ST-03", allStations.get(2).getCode());
    }
}
