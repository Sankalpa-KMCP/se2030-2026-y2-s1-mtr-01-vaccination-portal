package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
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
class OperationalStaffRosterPersistenceIntegrationTests {

    @Autowired private OperationalStaffRosterRepository rosterRepository;
    @Autowired private VaccinationCenterRepository centerRepository;

    private VaccinationCenter centerA;
    private VaccinationCenter centerB;

    @BeforeEach
    void setUp() {
        centerA = centerRepository.save(new VaccinationCenter("CTR-STAFF-A", "Staff Center A", "100 Nurse Way", "+1000", CenterStatus.ACTIVE, 40, 80, "UTC"));
        centerB = centerRepository.save(new VaccinationCenter("CTR-STAFF-B", "Staff Center B", "200 Nurse Way", "+2000", CenterStatus.ACTIVE, 30, 60, "UTC"));
    }

    @Test
    void persistsRosterAndPessimisticLockQuery() {
        OperationalStaffRoster saved = rosterRepository.saveAndFlush(
                new OperationalStaffRoster(centerA, 101L, StaffRole.VACCINATOR)
        );

        OperationalStaffRoster reloaded = rosterRepository.findById(saved.getId()).orElseThrow();
        assertEquals(101L, reloaded.getUserId());
        assertEquals(StaffRole.VACCINATOR, reloaded.getRole());
        assertEquals(OperationalStaffStatus.ACTIVE, reloaded.getStatus());
        assertNotNull(reloaded.getCreatedAt());
        assertNotNull(reloaded.getUpdatedAt());

        // Pessimistic write lock queries
        OperationalStaffRoster lockedById = rosterRepository.findByIdForUpdate(saved.getId()).orElseThrow();
        assertEquals(saved.getId(), lockedById.getId());

        OperationalStaffRoster lockedByUserAndCenter = rosterRepository.findByCenterIdAndUserIdForUpdate(centerA.getId(), 101L).orElseThrow();
        assertEquals(saved.getId(), lockedByUserAndCenter.getId());
    }

    @Test
    void enforcesUniqueUserPerCenter_rejectsDuplicate() {
        rosterRepository.saveAndFlush(
                new OperationalStaffRoster(centerA, 201L, StaffRole.VACCINATOR)
        );

        // Duplicate user at center A must fail
        assertThrows(DataIntegrityViolationException.class, () -> {
            rosterRepository.saveAndFlush(
                    new OperationalStaffRoster(centerA, 201L, StaffRole.CHECK_IN_STAFF)
            );
        });
    }

    @Test
    void permitsSameUserAtDifferentCenters() {
        rosterRepository.saveAndFlush(
                new OperationalStaffRoster(centerA, 201L, StaffRole.VACCINATOR)
        );

        // Same user registered at different center B is permitted
        OperationalStaffRoster atCenterB = rosterRepository.saveAndFlush(
                new OperationalStaffRoster(centerB, 201L, StaffRole.VACCINATOR)
        );
        assertNotNull(atCenterB.getId());
    }

    @Test
    void queriesByCenterAndUser() {
        rosterRepository.saveAndFlush(new OperationalStaffRoster(centerA, 301L, StaffRole.VACCINATOR));
        rosterRepository.saveAndFlush(new OperationalStaffRoster(centerA, 302L, StaffRole.CHECK_IN_STAFF));
        rosterRepository.saveAndFlush(new OperationalStaffRoster(centerB, 301L, StaffRole.SUPERVISOR));

        List<OperationalStaffRoster> centerARoster = rosterRepository.findByCenterIdOrderByUserIdAsc(centerA.getId());
        assertEquals(2, centerARoster.size());

        List<OperationalStaffRoster> user301Entries = rosterRepository.findByUserIdOrderByCenterIdAsc(301L);
        assertEquals(2, user301Entries.size());
    }
}
