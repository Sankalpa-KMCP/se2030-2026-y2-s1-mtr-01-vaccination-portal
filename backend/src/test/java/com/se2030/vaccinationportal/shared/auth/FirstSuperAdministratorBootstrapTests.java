package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.bootstrap.FirstSuperAdministratorBootstrap;
import com.se2030.vaccinationportal.shared.auth.bootstrap.FirstSuperAdministratorBootstrapService;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FirstSuperAdministratorBootstrapTests {

    private static final String PASSWORD = "BootstrapSuperPassword123!";

    @Autowired private FirstSuperAdministratorBootstrapService bootstrapService;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(new Role("ROLE_SUPER_ADMIN", "Super Administrator"));
        roleRepository.save(new Role("ROLE_ADMIN", "System Administrator"));
        roleRepository.save(new Role("ROLE_CITIZEN", "Citizen"));
    }

    @Test
    @DisplayName("bootstrap is disabled when no configuration is provided")
    void noConfiguration_doesNothing() {
        runner("", "", "", "").run(null);
        assertEquals(0, userAccountRepository.count());
    }

    @Test
    @DisplayName("partial bootstrap configuration fails without creating an account")
    void partialConfiguration_failsAtomically() {
        FirstSuperAdministratorBootstrap runner = runner(
                "superadmin@example.com", PASSWORD, "", "+94 77 100 2000");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> runner.run(null));
        assertTrue(error.getMessage().contains("must all be provided together"));
        assertEquals(0, userAccountRepository.count());
    }

    @Test
    @DisplayName("full bootstrap configuration creates one active super administrator with hashed credentials")
    void fullConfiguration_createsFirstSuperAdministratorSafely() {
        runner(" FIRST.SUPERADMIN@Example.com ", PASSWORD, " First Super Administrator ", " 0771002000 ").run(null);

        UserAccount superAdmin = userAccountRepository.findByEmailIgnoreCase("first.superadmin@example.com").orElseThrow();
        assertEquals(AccountStatus.ACTIVE, superAdmin.getAccountStatus());
        assertEquals("First Super Administrator", superAdmin.getFullName());
        assertEquals("0771002000", superAdmin.getPhoneNumber());
        assertEquals(1, superAdmin.getRoles().size());
        assertEquals("ROLE_SUPER_ADMIN", superAdmin.getRoles().iterator().next().getCode());
        assertNotEquals(PASSWORD, superAdmin.getPasswordHash());
        assertTrue(passwordEncoder.matches(PASSWORD, superAdmin.getPasswordHash()));
        assertNull(superAdmin.getRequestedRole());
        assertNull(superAdmin.getReviewedBy());
        assertNull(superAdmin.getReviewedAt());
        assertNull(superAdmin.getRejectionReason());
    }

    @Test
    @DisplayName("first super administrator creation rejects a phone that is not exactly ten digits")
    void fullConfiguration_invalidPhone_failsWithoutCreatingSuperAdministrator() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                runner("superadmin@example.com", PASSWORD, "Super Administrator", "+94771002000").run(null));

        assertTrue(error.getMessage().contains("phone number must contain exactly 10 digits"));
        assertEquals(0, userAccountRepository.count());
    }

    @Test
    @DisplayName("rerunning bootstrap when a super administrator exists creates no duplicate and never resets password")
    void existingSuperAdministrator_isNeverModified() {
        runner("superadmin@example.com", PASSWORD, "Super Administrator", "0771002000").run(null);
        String originalHash = userAccountRepository.findByEmailIgnoreCase("superadmin@example.com")
                .orElseThrow().getPasswordHash();

        runner("invalid legacy config", "legacy config", "Legacy Config", "+94 77 999 9999").run(null);

        UserAccount unchanged = userAccountRepository.findByEmailIgnoreCase("superadmin@example.com").orElseThrow();
        assertEquals(1, userAccountRepository.count());
        assertEquals(originalHash, unchanged.getPasswordHash());
        assertEquals("Super Administrator", unchanged.getFullName());
        assertTrue(passwordEncoder.matches(PASSWORD, unchanged.getPasswordHash()));
    }

    @Test
    @DisplayName("configured email already owned by a non-super-admin fails instead of silently elevating it")
    void existingNonSuperAdministratorEmail_failsWithoutElevation() {
        UserAccount citizen = new UserAccount(
                "existing@example.com", passwordEncoder.encode(PASSWORD), AccountStatus.ACTIVE);
        citizen.addRole(roleRepository.findByCode("ROLE_CITIZEN").orElseThrow());
        userAccountRepository.saveAndFlush(citizen);

        assertThrows(IllegalStateException.class, () -> runner(
                "existing@example.com", PASSWORD, "Existing Citizen", "0771002000").run(null));

        UserAccount unchanged = userAccountRepository.findByEmailIgnoreCase("existing@example.com").orElseThrow();
        assertEquals(1, unchanged.getRoles().size());
        assertEquals("ROLE_CITIZEN", unchanged.getRoles().iterator().next().getCode());
    }

    @Test
    @DisplayName("an existing super administrator with another email suppresses creation of any extra bootstrap super admin")
    void existingSuperAdministratorWithDifferentEmail_createsNothing() {
        UserAccount superAdmin = new UserAccount(
                "existing-superadmin@example.com", passwordEncoder.encode(PASSWORD), AccountStatus.ACTIVE);
        superAdmin.addRole(roleRepository.findByCode("ROLE_SUPER_ADMIN").orElseThrow());
        userAccountRepository.saveAndFlush(superAdmin);

        runner("new-superadmin@example.com", PASSWORD, "New Super Administrator", "+94 77 100 2000").run(null);

        assertEquals(1, userAccountRepository.count());
        assertFalse(userAccountRepository.existsByEmailIgnoreCase("new-superadmin@example.com"));
    }

    private FirstSuperAdministratorBootstrap runner(String email, String password, String fullName, String phone) {
        return new FirstSuperAdministratorBootstrap(bootstrapService, email, password, fullName, phone);
    }
}
