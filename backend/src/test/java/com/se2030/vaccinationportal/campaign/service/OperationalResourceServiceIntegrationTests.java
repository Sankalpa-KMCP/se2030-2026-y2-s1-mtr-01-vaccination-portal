package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OperationalResourceServiceIntegrationTests {

    @Autowired private OperationalResourceService resourceService;
    @Autowired private OperationalStaffRosterService rosterService;
    @Autowired private VaccinationCenterRepository centerRepository;

    private VaccinationCenter center;

    @BeforeEach
    void setUp() {
        center = centerRepository.save(new VaccinationCenter("CTR-SRV-01", "Service Center", "500 Clinic Way", "+555", CenterStatus.ACTIVE, 40, 80, "UTC"));
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
    }

    @Test
    void rosterService_registersAndUpdatesStatus() {
        OperationalStaffRoster roster = rosterService.registerStaffMember(
                center.getId(),
                777L,
                StaffRole.VACCINATOR
        );
        assertNotNull(roster.getId());
        assertEquals(777L, roster.getUserId());
        assertEquals(StaffRole.VACCINATOR, roster.getRole());
        assertEquals(OperationalStaffStatus.ACTIVE, roster.getStatus());

        // Duplicate registration rejection
        assertThrows(IllegalStateException.class, () -> rosterService.registerStaffMember(
                center.getId(), 777L, StaffRole.CHECK_IN_STAFF
        ));

        // Update status
        OperationalStaffRoster updated = rosterService.updateStaffStatus(roster.getId(), OperationalStaffStatus.INACTIVE);
        assertEquals(OperationalStaffStatus.INACTIVE, updated.getStatus());

        // Query by user
        List<OperationalStaffRoster> userEntries = rosterService.getRosterEntriesForUser(777L);
        assertEquals(1, userEntries.size());
        assertEquals(center.getId(), userEntries.get(0).getCenter().getId());
    }
}
