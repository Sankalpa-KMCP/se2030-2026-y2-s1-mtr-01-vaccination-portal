package com.se2030.vaccinationportal.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = roleRepository.save(new Role("ROLE_CENTER_MANAGER", "Vaccination Center Manager"));
        testUser = new UserAccount("manager@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        testUser.setPhoneNumber("+94 77 123 4567");
        testUser.addRole(role);
        testUser = userAccountRepository.save(testUser);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid credentials returns 200 and JWT token")
    void login_validCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest("manager@portal.gov", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is("manager@portal.gov")))
                .andExpect(jsonPath("$.user.phoneNumber", is("+94 77 123 4567")))
                .andExpect(jsonPath("$.user.roles", hasItem("ROLE_CENTER_MANAGER")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid password returns 401 Unauthorized")
    void login_invalidPassword_returns401() throws Exception {
        LoginRequest request = new LoginRequest("manager@portal.gov", "WrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with blank fields returns 400 Bad Request")
    void login_blankFields_returns400() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with valid Bearer token returns 200 and UserProfileResponse")
    void getMe_withValidBearerToken_returns200() throws Exception {
        String token = jwtTokenService.generateToken(testUser.getId(), testUser.getEmail(), List.of("ROLE_CENTER_MANAGER"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$.email", is("manager@portal.gov")))
                .andExpect(jsonPath("$.phoneNumber", is("+94 77 123 4567")))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_CENTER_MANAGER")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without Authorization header returns 401 JSON")
    void getMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with invalid or tampered token returns 401 JSON")
    void getMe_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token.string"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("Secured business endpoint denies anonymous access with 403 Forbidden")
    void securedBusinessEndpoint_deniesAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns"))
                .andExpect(status().isForbidden());
    }
}
