package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.dto.CitizenRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class CitizenProfileRegistrationRollbackIntegrationTests {

    @Autowired private AuthService authService;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("citizen profile persistence failure rolls back the newly-created user account")
    void registerCitizen_profilePersistenceFails_rollsBackAccount() {
        boolean roleCreated = roleRepository.findByCode("ROLE_CITIZEN").isEmpty();
        if (roleCreated) {
            roleRepository.saveAndFlush(new Role("ROLE_CITIZEN", "Citizen"));
        }
        String email = "profile-rollback-" + Long.toUnsignedString(System.nanoTime()) + "@example.com";
        jdbcTemplate.execute("""
                ALTER TABLE citizen_profiles
                ADD CONSTRAINT ck_citizen_profile_registration_failure CHECK (address IS NOT NULL)
                """);
        try {
            assertThrows(DataIntegrityViolationException.class, () -> authService.registerCitizen(
                    new CitizenRegistrationRequest(
                            "Rollback Citizen", email, "+94 77 444 5555", "RollbackPassword123!")));

            assertFalse(userAccountRepository.existsByEmailIgnoreCase(email),
                    "Failed profile creation must not leave a partially-created account");
        } finally {
            jdbcTemplate.execute("""
                    ALTER TABLE citizen_profiles
                    DROP CONSTRAINT ck_citizen_profile_registration_failure
                    """);
            if (roleCreated) {
                roleRepository.findByCode("ROLE_CITIZEN").ifPresent(roleRepository::delete);
            }
        }
    }
}
