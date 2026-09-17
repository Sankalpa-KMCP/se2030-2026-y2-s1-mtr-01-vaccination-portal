package com.se2030.vaccinationportal.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.shared.auth.dto.CitizenRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationControllerTests {

    private static final String RAW_PASSWORD = "RegistrationPassword123!";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpCanonicalRoles() {
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
    @DisplayName("anonymous citizen registration returns 201 and ignores client-controlled authority fields")
    void registerCitizen_anonymousRequest_returns201WithServerControlledAuthority() throws Exception {
        String request = """
                {
                  "fullName": "Citizen One",
                  "email": " CITIZEN.ONE@Example.com ",
                  "phoneNumber": "0771002000",
                  "password": "RegistrationPassword123!",
                  "accountStatus": "REJECTED",
                  "roles": ["ROLE_ADMIN"],
                  "reviewedBy": 1,
                  "rejectionReason": "client supplied"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register/citizen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("citizen.one@example.com")))
                .andExpect(jsonPath("$.accountStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_CITIZEN")))
                .andExpect(jsonPath("$.roles.length()", is(1)))
                .andExpect(jsonPath("$.requestedRoleCode").isEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("anonymous staff registration returns 202 without granting requested authority")
    void registerStaff_anonymousRequest_returns202WithoutAuthority() throws Exception {
        StaffRegistrationRequest request = new StaffRegistrationRequest(
                "Healthcare Worker", "worker@example.com", "0761002000",
                RAW_PASSWORD, "ROLE_HEALTHCARE_OFFICER");

        mockMvc.perform(post("/api/v1/auth/register/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accountStatus", is("PENDING_APPROVAL")))
                .andExpect(jsonPath("$.roles", empty()))
                .andExpect(jsonPath("$.requestedRoleCode", is("ROLE_HEALTHCARE_OFFICER")))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("duplicate normalized registration email returns stable 409 response")
    void registerCitizen_duplicateEmail_returns409() throws Exception {
        userAccountRepository.saveAndFlush(new UserAccount(
                "duplicate@example.com", passwordEncoder.encode(RAW_PASSWORD), AccountStatus.ACTIVE));
        CitizenRegistrationRequest request = new CitizenRegistrationRequest(
                "Duplicate", " DUPLICATE@EXAMPLE.COM ", "0772223333", RAW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register/citizen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_EMAIL")))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("forbidden public staff role returns 400")
    void registerStaff_adminRole_returns400() throws Exception {
        StaffRegistrationRequest request = new StaffRegistrationRequest(
                "Administrator Request", "admin-request@example.com", "0774445555",
                RAW_PASSWORD, "ROLE_ADMIN");

        mockMvc.perform(post("/api/v1/auth/register/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_REQUESTED_ROLE")));
    }

    @Test
    @DisplayName("registration fields are validated with stable 400 response")
    void registerCitizen_blankFields_returns400() throws Exception {
        CitizenRegistrationRequest request = new CitizenRegistrationRequest(" ", "not-an-email", "", "");

        mockMvc.perform(post("/api/v1/auth/register/citizen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors.fullName").isArray())
                .andExpect(jsonPath("$.fieldErrors.email").isArray())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").isArray())
                .andExpect(jsonPath("$.fieldErrors.password").isArray());
    }

    @Test
    @DisplayName("contact format failures return friendly field messages")
    void registerCitizen_invalidContactFormat_returnsFriendlyMessages() throws Exception {
        CitizenRegistrationRequest request = new CitizenRegistrationRequest(
                "Citizen", "student@domain", "077-123-4567", RAW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register/citizen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email", hasItem("Enter a valid email address.")))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber",
                        hasItem("Phone number must contain exactly 10 digits.")));
    }

    @ParameterizedTest(name = "citizen registration accepts email {0}")
    @ValueSource(strings = {
            "student@gmail.com",
            "student@university.edu",
            "admin@email.com",
            "  student@gmail.com  "
    })
    void registerCitizen_validEmail_returns201(String email) throws Exception {
        CitizenRegistrationRequest request = new CitizenRegistrationRequest(
                "Citizen", email, "0771234567", RAW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register/citizen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "staff registration accepts email {0}")
    @ValueSource(strings = {
            "student@gmail.com",
            "student@university.edu",
            "admin@email.com",
            "  student@gmail.com  "
    })
    void registerStaff_validEmail_returns202(String email) throws Exception {
        StaffRegistrationRequest request = new StaffRegistrationRequest(
                "Staff", email, "0771234567", RAW_PASSWORD, "ROLE_HEALTHCARE_OFFICER");

        mockMvc.perform(post("/api/v1/auth/register/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @ParameterizedTest(name = "citizen registration rejects email {0}")
    @ValueSource(strings = {
            "student",
            "student@",
            "@gmail.com",
            "student gmail.com",
            "student@domain",
            "   "
    })
    void registerCitizen_invalidEmail_returns400(String email) throws Exception {
        CitizenRegistrationRequest request = new CitizenRegistrationRequest(
                "Citizen", email, "0771234567", RAW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register/citizen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors.email").isArray());
    }

    @ParameterizedTest(name = "staff registration rejects email {0}")
    @ValueSource(strings = {
            "student",
            "student@",
            "@gmail.com",
            "student gmail.com",
            "student@domain",
            "   "
    })
    void registerStaff_invalidEmail_returns400(String email) throws Exception {
        StaffRegistrationRequest request = new StaffRegistrationRequest(
                "Staff", email, "0771234567", RAW_PASSWORD, "ROLE_HEALTHCARE_OFFICER");

        mockMvc.perform(post("/api/v1/auth/register/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors.email").isArray());
    }

    @ParameterizedTest(name = "citizen registration accepts phone {0}")
    @ValueSource(strings = {"0771234567", "  0771234567  "})
    void registerCitizen_validPhone_returns201(String phoneNumber) throws Exception {
        CitizenRegistrationRequest request = new CitizenRegistrationRequest(
                "Citizen", "citizen@example.com", phoneNumber, RAW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register/citizen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "staff registration accepts phone {0}")
    @ValueSource(strings = {"0771234567", "  0771234567  "})
    void registerStaff_validPhone_returns202(String phoneNumber) throws Exception {
        StaffRegistrationRequest request = new StaffRegistrationRequest(
                "Staff", "staff@example.com", phoneNumber, RAW_PASSWORD, "ROLE_HEALTHCARE_OFFICER");

        mockMvc.perform(post("/api/v1/auth/register/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @ParameterizedTest(name = "citizen registration rejects phone {0}")
    @ValueSource(strings = {
            "771234567",
            "07712345678",
            "07712ABC67",
            "077-123-4567",
            "+94771234567",
            "077 1234567",
            "   "
    })
    void registerCitizen_invalidPhone_returns400(String phoneNumber) throws Exception {
        CitizenRegistrationRequest request = new CitizenRegistrationRequest(
                "Citizen", "citizen@example.com", phoneNumber, RAW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register/citizen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").isArray());
    }

    @ParameterizedTest(name = "staff registration rejects phone {0}")
    @ValueSource(strings = {
            "771234567",
            "07712345678",
            "07712ABC67",
            "077-123-4567",
            "+94771234567",
            "077 1234567",
            "   "
    })
    void registerStaff_invalidPhone_returns400(String phoneNumber) throws Exception {
        StaffRegistrationRequest request = new StaffRegistrationRequest(
                "Staff", "staff@example.com", phoneNumber, RAW_PASSWORD, "ROLE_HEALTHCARE_OFFICER");

        mockMvc.perform(post("/api/v1/auth/register/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").isArray());
    }

    @Test
    @DisplayName("pending account returns stable 403 only with correct password and never returns a token")
    void login_pendingAccount_returnsProtectedStatusOutcome() throws Exception {
        saveAccount("pending@example.com", AccountStatus.PENDING_APPROVAL);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("pending@example.com", "WrongPassword!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("pending@example.com", RAW_PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCOUNT_PENDING_APPROVAL")))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("rejected account returns stable 403 only with correct password and never returns a token")
    void login_rejectedAccount_returnsProtectedStatusOutcome() throws Exception {
        saveAccount("rejected@example.com", AccountStatus.REJECTED);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("rejected@example.com", "WrongPassword!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("rejected@example.com", RAW_PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCOUNT_REJECTED")))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("protected current-user endpoint remains unavailable anonymously")
    void getMe_anonymousRequest_remainsProtected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private void saveAccount(String email, AccountStatus status) {
        userAccountRepository.saveAndFlush(
                new UserAccount(email, passwordEncoder.encode(RAW_PASSWORD), status));
    }
}
