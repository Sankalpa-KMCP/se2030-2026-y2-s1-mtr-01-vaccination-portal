package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.dto.CitizenProfileResponse;
import com.se2030.vaccinationportal.shared.auth.dto.UpdateCitizenProfileRequest;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.CitizenGender;
import com.se2030.vaccinationportal.shared.auth.model.CitizenProfile;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.CitizenProfileRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.service.CitizenProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CitizenProfileServiceIntegrationTests {

    @Autowired private CitizenProfileService citizenProfileService;
    @Autowired private CitizenProfileRepository citizenProfileRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Role citizenRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        citizenProfileRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.flush();
        citizenRole = roleRepository.save(new Role("ROLE_CITIZEN", "Citizen"));
        adminRole = roleRepository.save(new Role("ROLE_ADMIN", "System Administrator"));
    }

    @Test
    @DisplayName("legacy citizen without profile receives shared account data and null demographics")
    void getOwnProfile_legacyCitizen_returnsAccountAndNullProfileFields() {
        UserAccount citizen = saveAccount(
                "legacy.citizen@example.com", "Legacy Citizen", "+94 77 111 2222", citizenRole);

        CitizenProfileResponse response = citizenProfileService.getOwnProfile(citizen.getId());

        assertEquals(citizen.getId(), response.id());
        assertEquals("Legacy Citizen", response.fullName());
        assertEquals("legacy.citizen@example.com", response.email());
        assertEquals("+94 77 111 2222", response.phoneNumber());
        assertEquals(AccountStatus.ACTIVE, response.accountStatus());
        assertEquals(List.of("ROLE_CITIZEN"), response.roles());
        assertEquals(citizen.getCreatedAt(), response.createdAt());
        assertNull(response.dateOfBirth());
        assertNull(response.gender());
        assertNull(response.address());
        assertTrue(citizenProfileRepository.findByUser_Id(citizen.getId()).isEmpty());
    }

    @Test
    @DisplayName("profile update lazily creates one profile and changes only allowed fields")
    void updateOwnProfile_legacyCitizen_createsProfileAndPreservesProtectedAccountFields() {
        UserAccount citizen = saveAccount(
                "protected@example.com", "Before Name", "+94 70 000 0000", citizenRole);
        Long originalId = citizen.getId();
        String originalEmail = citizen.getEmail();
        String originalPasswordHash = citizen.getPasswordHash();

        CitizenProfileResponse response = citizenProfileService.updateOwnProfile(citizen.getId(),
                new UpdateCitizenProfileRequest(
                        "  Updated Citizen  ",
                        "  +94 77 987 6543  ",
                        LocalDate.of(1994, 6, 15),
                        CitizenGender.PREFER_NOT_TO_SAY,
                        "  42 Lake Road, Colombo  "
                ));

        CitizenProfile profile = citizenProfileRepository.findByUser_Id(originalId).orElseThrow();
        UserAccount persisted = userAccountRepository.findById(originalId).orElseThrow();
        assertEquals(originalId, profile.getUserId());
        assertEquals(originalId, profile.getUser().getId());
        assertEquals("Updated Citizen", persisted.getFullName());
        assertEquals("+94 77 987 6543", persisted.getPhoneNumber());
        assertEquals(originalEmail, persisted.getEmail());
        assertEquals(originalPasswordHash, persisted.getPasswordHash());
        assertEquals(AccountStatus.ACTIVE, persisted.getAccountStatus());
        assertEquals(List.of("ROLE_CITIZEN"), persisted.getRoles().stream().map(Role::getCode).toList());
        assertEquals(LocalDate.of(1994, 6, 15), profile.getDateOfBirth());
        assertEquals(CitizenGender.PREFER_NOT_TO_SAY, profile.getGender());
        assertEquals("42 Lake Road, Colombo", profile.getAddress());
        assertEquals("Updated Citizen", response.fullName());
        assertEquals("+94 77 987 6543", response.phoneNumber());
        assertEquals(LocalDate.of(1994, 6, 15), response.dateOfBirth());
        assertEquals(CitizenGender.PREFER_NOT_TO_SAY, response.gender());
        assertEquals("42 Lake Road, Colombo", response.address());

        CitizenProfileResponse reloaded = citizenProfileService.getOwnProfile(originalId);
        assertEquals(response.fullName(), reloaded.fullName());
        assertEquals(response.phoneNumber(), reloaded.phoneNumber());
        assertEquals(response.dateOfBirth(), reloaded.dateOfBirth());
        assertEquals(response.gender(), reloaded.gender());
        assertEquals(response.address(), reloaded.address());
    }

    @Test
    @DisplayName("updating an existing citizen profile reuses the one-to-one row")
    void updateOwnProfile_existingProfile_updatesSingleRow() {
        UserAccount citizen = saveAccount(
                "existing.profile@example.com", "Existing", "+94 71 111 1111", citizenRole);
        citizenProfileRepository.saveAndFlush(new CitizenProfile(citizen));

        citizenProfileService.updateOwnProfile(citizen.getId(), new UpdateCitizenProfileRequest(
                "Existing Updated", "+94 71 222 2222", null, CitizenGender.OTHER, "Updated address"));

        assertEquals(1, citizenProfileRepository.count());
        assertEquals(CitizenGender.OTHER,
                citizenProfileRepository.findByUser_Id(citizen.getId()).orElseThrow().getGender());
    }

    @Test
    @DisplayName("non-citizen accounts cannot use the citizen profile service")
    void getAndUpdate_nonCitizen_rejected() {
        UserAccount administrator = saveAccount(
                "admin@example.com", "Administrator", "+94 76 333 3333", adminRole);

        assertThrows(AccessDeniedException.class,
                () -> citizenProfileService.getOwnProfile(administrator.getId()));
        assertThrows(AccessDeniedException.class,
                () -> citizenProfileService.updateOwnProfile(administrator.getId(),
                        new UpdateCitizenProfileRequest("Admin", "+94 76 333 3333", null, null, null)));
        assertFalse(citizenProfileRepository.findByUser_Id(administrator.getId()).isPresent());
    }

    private UserAccount saveAccount(String email, String fullName, String phoneNumber, Role role) {
        UserAccount account = new UserAccount(
                email, passwordEncoder.encode("CitizenProfilePassword123!"), AccountStatus.ACTIVE);
        account.setFullName(fullName);
        account.setPhoneNumber(phoneNumber);
        account.addRole(role);
        return userAccountRepository.saveAndFlush(account);
    }
}
