package com.se2030.vaccinationportal.campaign.center.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OperationalResourceServiceIntegrationTests {

    @Autowired private OperationalResourceService resourceService;
    @Autowired private VaccinationCenterRepository centerRepository;

    private VaccinationCenter center;
    private VaccinationCenter centerOther;

    @BeforeEach
    void setUp() {
        center = centerRepository.save(new VaccinationCenter("CTR-SRV-01", "Service Center", "500 Clinic Way", "+555", CenterStatus.ACTIVE, 40, 80, "UTC"));
        centerOther = centerRepository.save(new VaccinationCenter("CTR-SRV-02", "Other Center", "600 Clinic Way", "+666", CenterStatus.ACTIVE, 30, 60, "UTC"));
    }

    @Test
    void resourceService_createsAndUpdatesStatus() {
        OperationalResource resource = resourceService.createResource(
                center.getId(),
                "STATION-A",
                ResourceType.VACCINATION_STATION,
                "Vaccination Station A"
        );
        assertNotNull(resource.getId());
        assertEquals("STATION-A", resource.getCode());
        assertEquals(OperationalResourceStatus.ACTIVE, resource.getStatus());

        // Reject duplicate code at same center
        assertThrows(IllegalStateException.class, () -> resourceService.createResource(
                center.getId(), "STATION-A", ResourceType.VACCINATION_STATION, "Duplicate"
        ));

        // Update status
        OperationalResource updated = resourceService.updateResourceStatus(resource.getId(), OperationalResourceStatus.INACTIVE);
        assertEquals(OperationalResourceStatus.INACTIVE, updated.getStatus());

        // Get resource
        OperationalResource retrieved = resourceService.getResource(resource.getId());
        assertEquals(OperationalResourceStatus.INACTIVE, retrieved.getStatus());
    }

    @Test
    void resourceService_updatesNameAndStatus_preservesCodeAndType() {
        OperationalResource resource = resourceService.createResource(
                center.getId(),
                "ROOM-A",
                ResourceType.ROOM,
                "Original Room Name",
                OperationalResourceStatus.ACTIVE
        );

        OperationalResource updated = resourceService.updateResource(
                center.getId(),
                resource.getId(),
                "Updated Room Name",
                OperationalResourceStatus.MAINTENANCE
        );

        assertEquals("ROOM-A", updated.getCode());
        assertEquals(ResourceType.ROOM, updated.getResourceType());
        assertEquals("Updated Room Name", updated.getName());
        assertEquals(OperationalResourceStatus.MAINTENANCE, updated.getStatus());

        // Verify nested ownership check
        assertThrows(ResourceNotFoundException.class, () -> resourceService.updateResource(
                centerOther.getId(),
                resource.getId(),
                "Cross Center Update",
                OperationalResourceStatus.ACTIVE
        ));
    }

    @Test
    void resourceService_getResourceWithCenterOwnership() {
        OperationalResource resource = resourceService.createResource(
                center.getId(),
                "EQUIP-01",
                ResourceType.EQUIPMENT,
                "Freezer Unit 1"
        );

        OperationalResource found = resourceService.getResource(center.getId(), resource.getId());
        assertNotNull(found);
        assertEquals("EQUIP-01", found.getCode());

        // Mismatch center throws 404
        assertThrows(ResourceNotFoundException.class, () -> resourceService.getResource(centerOther.getId(), resource.getId()));
    }

    @Test
    void resourceService_queriesActiveByResourceType() {
        resourceService.createResource(center.getId(), "ST-01", ResourceType.VACCINATION_STATION, "Station 1");
        resourceService.createResource(center.getId(), "ST-02", ResourceType.VACCINATION_STATION, "Station 2");
        OperationalResource st3 = resourceService.createResource(center.getId(), "ST-03", ResourceType.VACCINATION_STATION, "Station 3");
        resourceService.updateResourceStatus(st3.getId(), OperationalResourceStatus.INACTIVE);
        resourceService.createResource(center.getId(), "RM-01", ResourceType.ROOM, "Room 1");

        List<OperationalResource> activeStations = resourceService.getActiveResourcesForCenter(center.getId(), ResourceType.VACCINATION_STATION);
        assertEquals(2, activeStations.size());
        assertEquals("ST-01", activeStations.get(0).getCode());
        assertEquals("ST-02", activeStations.get(1).getCode());

        List<OperationalResource> allResources = resourceService.getResourcesForCenter(center.getId());
        assertEquals(4, allResources.size());

        List<OperationalResource> allStations = resourceService.getResourcesForCenter(center.getId(), ResourceType.VACCINATION_STATION);
        assertEquals(3, allStations.size());
    }
}
