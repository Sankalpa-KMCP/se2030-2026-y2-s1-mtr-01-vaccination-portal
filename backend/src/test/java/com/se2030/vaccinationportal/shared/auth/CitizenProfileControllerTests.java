package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.CitizenProfileRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CitizenProfileControllerTests {

    private static final String PASSWORD = "CitizenProfileController123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private CitizenProfileRepository citizenProfileRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;

    private UserAccount citizen;

    @BeforeEach
    void setUp() {
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
        citizen = saveActive("citizen.profile@example.com", "Citizen Before", "+94 77 000 1111",
                "ROLE_CITIZEN");
    }

    @Test
    @DisplayName("citizen GET returns own legacy account data and null profile fields")
    void getOwnProfile_citizenLegacyAccount_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me").header(HttpHeaders.AUTHORIZATION, bearer(citizen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(citizen.getId().intValue())))
                .andExpect(jsonPath("$.fullName", is("Citizen Before")))
                .andExpect(jsonPath("$.email", is("citizen.profile@example.com")))
                .andExpect(jsonPath("$.phoneNumber", is("+94 77 000 1111")))
                .andExpect(jsonPath("$.accountStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.roles[0]", is("ROLE_CITIZEN")))
                .andExpect(jsonPath("$.dateOfBirth").value((Object) null))
                .andExpect(jsonPath("$.gender").value((Object) null))
                .andExpect(jsonPath("$.address").value((Object) null))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("citizen with a legacy phone can still authenticate, call auth/me, and view the profile")
    void legacyPhone_citizenAuthenticationAndReadsRemainAvailable() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"citizen.profile@example.com",
                                 "password":"CitizenProfileController123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(citizen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber", is("+94 77 000 1111")));

        mockMvc.perform(get("/api/v1/profile/me").header(HttpHeaders.AUTHORIZATION, bearer(citizen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber", is("+94 77 000 1111")));
    }

    @Test
    @DisplayName("citizen PUT derives ownership from JWT, ignores protected properties, and GET returns updates")
    void updateOwnProfile_citizen_usesPrincipalAndOnlyUpdatesAllowedFields() throws Exception {
        UserAccount otherCitizen = saveActive(
                "other.citizen@example.com", "Other Citizen", "+94 77 900 9000", "ROLE_CITIZEN");
        String originalEmail = citizen.getEmail();
        String originalPasswordHash = citizen.getPasswordHash();
        String payload = """
                {
                  "id": %d,
                  "userId": %d,
                  "email": "attacker@example.com",
                  "roles": ["ROLE_ADMIN"],
                  "accountStatus": "REJECTED",
                  "password": "replacement",
                  "passwordHash": "replacement-hash",
                  "fullName": "  Citizen Updated  ",
                  "phoneNumber": "  0771234567  ",
                  "dateOfBirth": "1995-04-23",
                  "gender": "FEMALE",
                  "address": "  10 Temple Road  "
                }
                """.formatted(otherCitizen.getId(), otherCitizen.getId());

        mockMvc.perform(put("/api/v1/profile/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(citizen))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(citizen.getId().intValue())))
                .andExpect(jsonPath("$.fullName", is("Citizen Updated")))
                .andExpect(jsonPath("$.phoneNumber", is("0771234567")))
                .andExpect(jsonPath("$.dateOfBirth", is("1995-04-23")))
                .andExpect(jsonPath("$.gender", is("FEMALE")))
                .andExpect(jsonPath("$.address", is("10 Temple Road")))
                .andExpect(jsonPath("$.email", is(originalEmail)))
                .andExpect(jsonPath("$.accountStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.roles[0]", is("ROLE_CITIZEN")));

        UserAccount persisted = userAccountRepository.findById(citizen.getId()).orElseThrow();
        UserAccount untouchedOther = userAccountRepository.findById(otherCitizen.getId()).orElseThrow();
        assertEquals(originalEmail, persisted.getEmail());
        assertEquals(originalPasswordHash, persisted.getPasswordHash());
        assertEquals(AccountStatus.ACTIVE, persisted.getAccountStatus());
        assertEquals(List.of("ROLE_CITIZEN"), persisted.getRoles().stream().map(Role::getCode).toList());
        assertEquals("Other Citizen", untouchedOther.getFullName());
        assertTrue(citizenProfileRepository.findByUser_Id(otherCitizen.getId()).isEmpty());

        mockMvc.perform(get("/api/v1/profile/me").header(HttpHeaders.AUTHORIZATION, bearer(citizen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Citizen Updated")))
                .andExpect(jsonPath("$.dateOfBirth", is("1995-04-23")))
                .andExpect(jsonPath("$.gender", is("FEMALE")))
                .andExpect(jsonPath("$.address", is("10 Temple Road")));
    }

    @Test
    @DisplayName("future date of birth and invalid controlled gender are rejected")
    void updateOwnProfile_invalidDemographics_returns400() throws Exception {
        assertValidationFailure("""
                {"fullName":"Citizen","phoneNumber":"0771234567",
                 "dateOfBirth":"2999-01-01","gender":"MALE","address":"Address"}
                """);

        mockMvc.perform(put("/api/v1/profile/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(citizen))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Citizen","phoneNumber":"0771234567",
                                 "dateOfBirth":"1990-01-01","gender":"UNSUPPORTED","address":"Address"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));
    }

    @ParameterizedTest(name = "invalid profile payload {index} returns 400")
    @ValueSource(strings = {
            "{\"fullName\":\"   \",\"phoneNumber\":\"0771234567\"}",
            "{\"fullName\":\"Citizen\",\"phoneNumber\":\"   \"}",
            "{\"fullName\":\"Citizen\",\"phoneNumber\":\"0771234567\",\"address\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"
    })
    void updateOwnProfile_invalidRequiredOrLengthField_returns400(String payload) throws Exception {
        assertValidationFailure(payload);
    }

    @ParameterizedTest(name = "invalid profile phone {0} returns 400")
    @ValueSource(strings = {
            "771234567",
            "07712345678",
            "07712ABC67",
            "077-123-4567",
            "+94771234567",
            "077 1234567"
    })
    void updateOwnProfile_invalidPhone_returns400(String phoneNumber) throws Exception {
        mockMvc.perform(put("/api/v1/profile/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(citizen))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Citizen","phoneNumber":"%s"}
                                """.formatted(phoneNumber)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber",
                        hasItem("Phone number must contain exactly 10 digits.")));
    }

    @Test
    @DisplayName("anonymous GET and PUT receive 401")
    void profileEndpoints_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} cannot access citizen profile endpoints")
    @ValueSource(strings = {
            "ROLE_ADMIN",
            "ROLE_HEALTHCARE_OFFICER",
            "ROLE_CENTER_MANAGER",
            "ROLE_CAMPAIGN_OFFICER",
            "ROLE_INVENTORY_OFFICER",
            "ROLE_MANAGEMENT_USER"
    })
    void profileEndpoints_staffRoles_returns403(String roleCode) throws Exception {
        UserAccount staff = saveActive(
                roleCode.toLowerCase() + "@example.com", "Staff", "+94 76 555 0000", roleCode);

        mockMvc.perform(get("/api/v1/profile/me").header(HttpHeaders.AUTHORIZATION, bearer(staff)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/profile/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isForbidden());
    }

    private void assertValidationFailure(String payload) throws Exception {
        mockMvc.perform(put("/api/v1/profile/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(citizen))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    private UserAccount saveActive(String email, String fullName, String phoneNumber, String roleCode) {
        UserAccount account = new UserAccount(email, passwordEncoder.encode(PASSWORD), AccountStatus.ACTIVE);
        account.setFullName(fullName);
        account.setPhoneNumber(phoneNumber);
        account.addRole(roleRepository.findByCode(roleCode).orElseThrow());
        return userAccountRepository.saveAndFlush(account);
    }

    private String bearer(UserAccount account) {
        return "Bearer " + jwtTokenService.generateToken(
                account.getId(), account.getEmail(), account.getRoles().stream().map(Role::getCode).toList());
    }

    private String validPayload() {
        return """
                {"fullName":"Citizen","phoneNumber":"0771234567",
                 "dateOfBirth":"1990-01-01","gender":"OTHER","address":"Address"}
                """;
    }
}
