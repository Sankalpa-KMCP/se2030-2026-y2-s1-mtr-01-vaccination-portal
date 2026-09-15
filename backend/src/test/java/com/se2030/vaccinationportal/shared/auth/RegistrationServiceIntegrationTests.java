package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.dto.CitizenRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.dto.RegistrationResponse;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.exception.AccountPendingApprovalException;
import com.se2030.vaccinationportal.shared.auth.exception.AccountRejectedException;
import com.se2030.vaccinationportal.shared.auth.exception.DuplicateEmailException;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidRequestedRoleException;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.CitizenProfile;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.CitizenProfileRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegistrationServiceIntegrationTests {

    private static final String RAW_PASSWORD = "RegistrationPassword123!";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CitizenProfileRepository citizenProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpCanonicalRoles() {
        citizenProfileRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.saveAll(List.of(
                new Role("ROLE_ADMIN", "System Administrator"),
                new Role("ROLE_CENTER_MANAGER", "Vaccination Center Manager"),
                new Role("ROLE_HEALTHCARE_OFFICER", "Healthcare Officer"),
                new Role("ROLE_CAMPAIGN_OFFICER", "Campaign Officer"),
                new Role("ROLE_INVENTORY_OFFICER", "Inventory Officer"),
                new Role("ROLE_CITIZEN", "Citizen"),
                new Role("ROLE_MANAGEMENT_USER", "Management User")
        ));
    }

    @Test
    @DisplayName("citizen registration normalizes email, persists profile, hashes password, and assigns only citizen authority")
    void registerCitizen_validRequest_persistsServerControlledAccount() {
        RegistrationResponse response = authService.registerCitizen(new CitizenRegistrationRequest(
                "  Nimal Perera  ",
                "  NIMAL.PERERA@Example.COM  ",
                "  +94 77 123 4567  ",
                RAW_PASSWORD
        ));

        UserAccount persisted = userAccountRepository.findByEmailIgnoreCase("nimal.perera@example.com").orElseThrow();
        assertEquals("nimal.perera@example.com", persisted.getEmail());
        assertEquals("Nimal Perera", persisted.getFullName());
        assertEquals("+94 77 123 4567", persisted.getPhoneNumber());
        assertEquals(AccountStatus.ACTIVE, persisted.getAccountStatus());
        assertEquals(List.of("ROLE_CITIZEN"), persisted.getRoles().stream().map(Role::getCode).sorted().toList());
        assertNull(persisted.getRequestedRole());
        assertNotEquals(RAW_PASSWORD, persisted.getPasswordHash());
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, persisted.getPasswordHash()));

        assertEquals(persisted.getId(), response.id());
        assertEquals(AccountStatus.ACTIVE, response.accountStatus());
        assertEquals(List.of("ROLE_CITIZEN"), response.roles());
        assertNull(response.requestedRoleCode());

        CitizenProfile profile = citizenProfileRepository.findByUser_Id(persisted.getId()).orElseThrow();
        assertNull(profile.getDateOfBirth());
        assertNull(profile.getGender());
        assertNull(profile.getAddress());
    }

    @Test
    @DisplayName("normalized duplicate email is rejected for citizen and staff registration")
    void register_duplicateNormalizedEmail_rejected() {
        authService.registerCitizen(new CitizenRegistrationRequest(
                "Existing Citizen", "existing@example.com", "+94 71 111 1111", RAW_PASSWORD));

        assertThrows(DuplicateEmailException.class, () -> authService.registerCitizen(
                new CitizenRegistrationRequest("Duplicate", " EXISTING@EXAMPLE.COM ", "+94 72 222 2222", RAW_PASSWORD)));
        assertThrows(DuplicateEmailException.class, () -> authService.registerStaffRequest(
                new StaffRegistrationRequest("Duplicate Staff", "Existing@Example.com", "+94 73 333 3333",
                        RAW_PASSWORD, "ROLE_CENTER_MANAGER")));
    }

    @ParameterizedTest(name = "public staff role {0} is requestable")
    @ValueSource(strings = {
            "ROLE_HEALTHCARE_OFFICER",
            "ROLE_CENTER_MANAGER",
            "ROLE_CAMPAIGN_OFFICER",
            "ROLE_INVENTORY_OFFICER"
    })
    void registerStaff_allowedRole_persistsPendingRequestWithoutAuthority(String requestedRoleCode) {
        String email = requestedRoleCode.toLowerCase() + "@example.com";

        RegistrationResponse response = authService.registerStaffRequest(new StaffRegistrationRequest(
                "  Requested Staff  ", email, "  +44 20 7946 0958  ", RAW_PASSWORD,
                "  " + requestedRoleCode.toLowerCase() + "  "));

        UserAccount persisted = userAccountRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(AccountStatus.PENDING_APPROVAL, persisted.getAccountStatus());
        assertEquals(requestedRoleCode, persisted.getRequestedRole().getCode());
        assertTrue(persisted.getRoles().isEmpty());
        assertEquals("Requested Staff", persisted.getFullName());
        assertEquals("+44 20 7946 0958", persisted.getPhoneNumber());
        assertNotEquals(RAW_PASSWORD, persisted.getPasswordHash());
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, persisted.getPasswordHash()));

        assertEquals(AccountStatus.PENDING_APPROVAL, response.accountStatus());
        assertEquals(requestedRoleCode, response.requestedRoleCode());
        assertTrue(response.roles().isEmpty());
        assertTrue(citizenProfileRepository.findByUser_Id(persisted.getId()).isEmpty(),
                "Staff registration must not create a citizen profile");
    }

    @ParameterizedTest(name = "staff role {0} is not publicly requestable")
    @ValueSource(strings = {
            "ROLE_ADMIN",
            "ROLE_CITIZEN",
            "ROLE_MANAGEMENT_USER",
            "ROLE_SYSTEM_ADMINISTRATOR",
            "ROLE_RECEPTION_STAFF",
            "UNKNOWN_ROLE"
    })
    void registerStaff_forbiddenOrUnknownRole_rejected(String requestedRoleCode) {
        assertThrows(InvalidRequestedRoleException.class, () -> authService.registerStaffRequest(
                new StaffRegistrationRequest("Staff", "staff@example.com", "+94 77 555 5555",
                        RAW_PASSWORD, requestedRoleCode)));
        assertFalse(userAccountRepository.existsByEmailIgnoreCase("staff@example.com"));
    }

    @Test
    @DisplayName("pending status is disclosed only after password verification")
    void login_pendingAccount_preservesEnumerationProtection() {
        UserAccount pending = saveAccount("pending@example.com", AccountStatus.PENDING_APPROVAL);

        assertThrows(BadCredentialsException.class, () -> authService.login(
                new LoginRequest(pending.getEmail(), "WrongPassword!")));
        assertThrows(AccountPendingApprovalException.class, () -> authService.login(
                new LoginRequest(pending.getEmail(), RAW_PASSWORD)));
    }

    @Test
    @DisplayName("rejected status is disclosed only after password verification")
    void login_rejectedAccount_preservesEnumerationProtection() {
        UserAccount rejected = saveAccount("rejected@example.com", AccountStatus.REJECTED);

        assertThrows(BadCredentialsException.class, () -> authService.login(
                new LoginRequest(rejected.getEmail(), "WrongPassword!")));
        assertThrows(AccountRejectedException.class, () -> authService.login(
                new LoginRequest(rejected.getEmail(), RAW_PASSWORD)));
    }

    private UserAccount saveAccount(String email, AccountStatus status) {
        return userAccountRepository.saveAndFlush(
                new UserAccount(email, passwordEncoder.encode(RAW_PASSWORD), status));
    }
}
