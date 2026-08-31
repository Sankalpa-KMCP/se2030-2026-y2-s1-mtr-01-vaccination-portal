package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.bootstrap.FirstAdministratorBootstrap;
import com.se2030.vaccinationportal.shared.auth.bootstrap.FirstAdministratorBootstrapService;
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
class FirstAdministratorBootstrapTests {

    private static final String PASSWORD = "BootstrapPassword123!";

    @Autowired private FirstAdministratorBootstrapService bootstrapService;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
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
        FirstAdministratorBootstrap runner = runner(
                "bootstrap@example.com", PASSWORD, "", "+94 77 100 2000");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> runner.run(null));
        assertTrue(error.getMessage().contains("must all be provided together"));
        assertEquals(0, userAccountRepository.count());
    }

    @Test
    @DisplayName("full bootstrap configuration creates one active administrator with hashed credentials and no review metadata")
    void fullConfiguration_createsFirstAdministratorSafely() {
        runner(" FIRST.ADMIN@Example.com ", PASSWORD, " First Administrator ", " +94 77 100 2000 ").run(null);

        UserAccount administrator = userAccountRepository.findByEmailIgnoreCase("first.admin@example.com").orElseThrow();
        assertEquals(AccountStatus.ACTIVE, administrator.getAccountStatus());
        assertEquals("First Administrator", administrator.getFullName());
        assertEquals("+94 77 100 2000", administrator.getPhoneNumber());
        assertEquals(1, administrator.getRoles().size());
        assertEquals("ROLE_ADMIN", administrator.getRoles().iterator().next().getCode());
        assertNotEquals(PASSWORD, administrator.getPasswordHash());
        assertTrue(passwordEncoder.matches(PASSWORD, administrator.getPasswordHash()));
        assertNull(administrator.getRequestedRole());
        assertNull(administrator.getReviewedBy());
        assertNull(administrator.getReviewedAt());
        assertNull(administrator.getRejectionReason());
    }

    @Test
    @DisplayName("rerunning bootstrap when an administrator exists creates no duplicate and never resets password")
    void existingAdministrator_isNeverModified() {
        runner("admin@example.com", PASSWORD, "Administrator", "+94 77 100 2000").run(null);
        String originalHash = userAccountRepository.findByEmailIgnoreCase("admin@example.com")
                .orElseThrow().getPasswordHash();

        runner("admin@example.com", "DifferentPassword123!", "Changed Name", "+94 77 999 9999").run(null);

        UserAccount unchanged = userAccountRepository.findByEmailIgnoreCase("admin@example.com").orElseThrow();
        assertEquals(1, userAccountRepository.count());
        assertEquals(originalHash, unchanged.getPasswordHash());
        assertEquals("Administrator", unchanged.getFullName());
        assertTrue(passwordEncoder.matches(PASSWORD, unchanged.getPasswordHash()));
    }

    @Test
    @DisplayName("configured email already owned by a non-admin fails instead of silently elevating it")
    void existingNonAdministratorEmail_failsWithoutElevation() {
        UserAccount citizen = new UserAccount(
                "existing@example.com", passwordEncoder.encode(PASSWORD), AccountStatus.ACTIVE);
        citizen.addRole(roleRepository.findByCode("ROLE_CITIZEN").orElseThrow());
        userAccountRepository.saveAndFlush(citizen);

        assertThrows(IllegalStateException.class, () -> runner(
                "existing@example.com", PASSWORD, "Existing Citizen", "+94 77 100 2000").run(null));

        UserAccount unchanged = userAccountRepository.findByEmailIgnoreCase("existing@example.com").orElseThrow();
        assertEquals(1, unchanged.getRoles().size());
        assertEquals("ROLE_CITIZEN", unchanged.getRoles().iterator().next().getCode());
    }

    @Test
    @DisplayName("an existing administrator with another email suppresses creation of any extra bootstrap admin")
    void existingAdministratorWithDifferentEmail_createsNothing() {
        UserAccount administrator = new UserAccount(
                "existing-admin@example.com", passwordEncoder.encode(PASSWORD), AccountStatus.ACTIVE);
        administrator.addRole(roleRepository.findByCode("ROLE_ADMIN").orElseThrow());
        userAccountRepository.saveAndFlush(administrator);

        runner("new-admin@example.com", PASSWORD, "New Administrator", "+94 77 100 2000").run(null);

        assertEquals(1, userAccountRepository.count());
        assertFalse(userAccountRepository.existsByEmailIgnoreCase("new-admin@example.com"));
    }

    private FirstAdministratorBootstrap runner(String email, String password, String fullName, String phone) {
        return new FirstAdministratorBootstrap(bootstrapService, email, password, fullName, phone);
    }
}
