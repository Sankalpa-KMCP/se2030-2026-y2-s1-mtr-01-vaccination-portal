package com.se2030.vaccinationportal.campaign.center;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffIdentity;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalStaffIdentityRepository;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.center.service.OperationalStaffIdentityLockService;
import com.se2030.vaccinationportal.campaign.center.service.OperationalStaffRosterService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests verifying OperationalStaffIdentity creation, atomic bootstrap idempotency,
 * unique constraint enforcement, deterministic ordered locking, and roster integration under H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OperationalStaffIdentityLockServiceIntegrationTests {

    @Autowired private OperationalStaffIdentityLockService identityLockService;
    @Autowired private OperationalStaffIdentityRepository identityRepository;
    @Autowired private OperationalStaffRosterService rosterService;
    @Autowired private OperationalStaffRosterRepository rosterRepository;
    @Autowired private VaccinationCenterRepository centerRepository;

    private VaccinationCenter centerA;
    private VaccinationCenter centerB;

    @BeforeEach
    void setUp() {
        centerA = centerRepository.save(new VaccinationCenter(
                "CTR-HA", "Center HA", "Address HA", "0112345678",
                CenterStatus.ACTIVE, 50, 200, "UTC"
        ));
        centerB = centerRepository.save(new VaccinationCenter(
                "CTR-HB", "Center HB", "Address HB", "0112345679",
                CenterStatus.ACTIVE, 50, 200, "UTC"
        ));
    }

    @Test
    @DisplayName("ensureAnchor creates persistent identity anchor on first use")
    void ensureAnchor_firstUse_createsIdentity() {
        Long userId = 7001L;
        assertTrue(identityRepository.findByUserId(userId).isEmpty());

        identityLockService.ensureAnchor(userId);

        OperationalStaffIdentity identity = identityRepository.findByUserId(userId).orElseThrow();
        assertEquals(userId, identity.getUserId());
        assertNotNull(identity.getCreatedAt());
        assertNotNull(identity.getId());
    }

    @Test
    @DisplayName("ensureAnchor repeated calls are idempotent and maintain single anchor")
    void ensureAnchor_repeatedCalls_idempotent() {
        Long userId = 7002L;

        identityLockService.ensureAnchor(userId);
        identityLockService.ensureAnchor(userId);
        identityLockService.ensureAnchor(userId);

        assertEquals(1, identityRepository.findAll().stream().filter(i -> i.getUserId().equals(userId)).count());
    }

    @Test
    @DisplayName("lockUser bootstraps and returns locked identity anchor")
    void lockUser_bootstrapsAndLocksAnchor() {
        Long userId = 7003L;

        OperationalStaffIdentity identity = identityLockService.lockUser(userId);

        assertNotNull(identity);
        assertEquals(userId, identity.getUserId());
        assertNotNull(identity.getId());
    }

    @Test
    @DisplayName("lockUsersOrdered handles empty and unsorted user ID collections deterministically")
    void lockUsersOrdered_deterministicOrder() {
        List<Long> userIds = List.of(7010L, 7005L, 7008L, 7005L);

        List<OperationalStaffIdentity> locked = identityLockService.lockUsersOrdered(userIds);

        assertEquals(3, locked.size());
        assertEquals(7005L, locked.get(0).getUserId());
        assertEquals(7008L, locked.get(1).getUserId());
        assertEquals(7010L, locked.get(2).getUserId());
    }

    @Test
    @DisplayName("registerStaffMember automatically establishes identity anchor for zero-row user")
    void registerStaffMember_createsIdentityAnchor() {
        Long userId = 7020L;
        assertTrue(identityRepository.findByUserId(userId).isEmpty());

        OperationalStaffRoster roster = rosterService.registerStaffMember(
                centerA.getId(), userId, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE
        );

        assertNotNull(roster.getId());
        assertEquals(userId, roster.getUserId());
        assertTrue(identityRepository.findByUserId(userId).isPresent());
    }

    @Test
    @DisplayName("registerStaffMember for same user across multiple centers reuses single identity anchor")
    void registerStaffMember_multiCenter_sharesSingleIdentityAnchor() {
        Long userId = 7030L;

        OperationalStaffRoster rosterA = rosterService.registerStaffMember(
                centerA.getId(), userId, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE
        );
        OperationalStaffRoster rosterB = rosterService.registerStaffMember(
                centerB.getId(), userId, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE
        );

        assertNotNull(rosterA.getId());
        assertNotNull(rosterB.getId());
        assertEquals(1, identityRepository.findAll().stream().filter(i -> i.getUserId().equals(userId)).count());
        assertEquals(2, rosterRepository.findByUserIdOrderByCenterIdAsc(userId).size());
    }

    @Test
    @DisplayName("Direct insertion of duplicate userId violates unique constraint")
    void uniqueConstraint_rejectsDuplicateUserId() {
        Long userId = 7040L;
        identityRepository.saveAndFlush(new OperationalStaffIdentity(userId));

        assertThrows(DataIntegrityViolationException.class, () -> {
            identityRepository.saveAndFlush(new OperationalStaffIdentity(userId));
        });
    }
}
