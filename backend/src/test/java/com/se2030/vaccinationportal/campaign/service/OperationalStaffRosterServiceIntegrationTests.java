package com.se2030.vaccinationportal.campaign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OperationalStaffRosterServiceIntegrationTests {

    @Autowired private OperationalStaffRosterService rosterService;
    @Autowired private VaccinationCenterRepository centerRepository;

    private VaccinationCenter center;
    private VaccinationCenter centerOther;

    @BeforeEach
    void setUp() {
        center = centerRepository.save(new VaccinationCenter("CTR-SRV-01", "Service Center", "500 Clinic Way", "+555", CenterStatus.ACTIVE, 40, 80, "UTC"));
        centerOther = centerRepository.save(new VaccinationCenter("CTR-SRV-02", "Other Center", "600 Clinic Way", "+666", CenterStatus.ACTIVE, 30, 60, "UTC"));
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

    @Test
    void rosterService_updatesRoleAndStatus_preservesUserId() {
        OperationalStaffRoster roster = rosterService.registerStaffMember(
                center.getId(),
                888L,
                StaffRole.VACCINATOR,
                OperationalStaffStatus.ACTIVE
        );

        OperationalStaffRoster updated = rosterService.updateStaffRoster(
                center.getId(),
                roster.getId(),
                StaffRole.SUPERVISOR,
                OperationalStaffStatus.INACTIVE
        );

        assertEquals(888L, updated.getUserId());
        assertEquals(StaffRole.SUPERVISOR, updated.getRole());
        assertEquals(OperationalStaffStatus.INACTIVE, updated.getStatus());

        // Verify nested ownership check
        assertThrows(ResourceNotFoundException.class, () -> rosterService.updateStaffRoster(
                centerOther.getId(),
                roster.getId(),
                StaffRole.CHECK_IN_STAFF,
                OperationalStaffStatus.ACTIVE
        ));
    }

    @Test
    void rosterService_getRosterEntryWithCenterOwnership() {
        OperationalStaffRoster roster = rosterService.registerStaffMember(
                center.getId(),
                999L,
                StaffRole.CHECK_IN_STAFF
        );

        OperationalStaffRoster found = rosterService.getRosterEntry(center.getId(), roster.getId());
        assertNotNull(found);
        assertEquals(999L, found.getUserId());

        // Mismatch center throws 404
        assertThrows(ResourceNotFoundException.class, () -> rosterService.getRosterEntry(centerOther.getId(), roster.getId()));
    }

    @Test
    void rosterService_queriesWithFilters() {
        rosterService.registerStaffMember(center.getId(), 101L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE);
        rosterService.registerStaffMember(center.getId(), 102L, StaffRole.VACCINATOR, OperationalStaffStatus.INACTIVE);
        rosterService.registerStaffMember(center.getId(), 103L, StaffRole.CHECK_IN_STAFF, OperationalStaffStatus.ACTIVE);
        rosterService.registerStaffMember(center.getId(), 104L, StaffRole.SUPERVISOR, OperationalStaffStatus.ACTIVE);

        List<OperationalStaffRoster> all = rosterService.getStaffRosterForCenter(center.getId());
        assertEquals(4, all.size());

        List<OperationalStaffRoster> vaccinators = rosterService.getStaffRosterForCenter(center.getId(), StaffRole.VACCINATOR, null);
        assertEquals(2, vaccinators.size());

        List<OperationalStaffRoster> active = rosterService.getStaffRosterForCenter(center.getId(), null, OperationalStaffStatus.ACTIVE);
        assertEquals(3, active.size());

        List<OperationalStaffRoster> activeVaccinators = rosterService.getStaffRosterForCenter(center.getId(), StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE);
        assertEquals(1, activeVaccinators.size());
        assertEquals(101L, activeVaccinators.get(0).getUserId());
    }
}
