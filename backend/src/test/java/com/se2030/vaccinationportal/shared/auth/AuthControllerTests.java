package com.se2030.vaccinationportal.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.shared.auth.controller.RefreshTokenCookieSupport;
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

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
    @DisplayName("POST /api/v1/auth/login with wrong password returns 401 INVALID_CREDENTIALS")
    void login_wrongPassword_returns401WithMachineCode() throws Exception {
        LoginRequest request = new LoginRequest("manager@portal.gov", "WrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with unknown email returns the same enumeration-safe 401")
    void login_unknownEmail_returnsSameEnumerationSafeResponse() throws Exception {
        LoginRequest request = new LoginRequest("ghost.unknown@portal.gov", "Whatever123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with an active timed lock returns 423 ACCOUNT_LOCKED")
    void login_timedLockedAccount_returns423WithMachineCode() throws Exception {
        testUser.setLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));
        userAccountRepository.saveAndFlush(testUser);

        LoginRequest request = new LoginRequest("manager@portal.gov", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code", is("ACCOUNT_LOCKED")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with an ineligible account returns 403 ACCOUNT_NOT_ACTIVE")
    void login_ineligibleAccount_returns403WithMachineCode() throws Exception {
        testUser.setAccountStatus(AccountStatus.SUSPENDED);
        userAccountRepository.saveAndFlush(testUser);

        LoginRequest request = new LoginRequest("manager@portal.gov", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCOUNT_NOT_ACTIVE")));
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

    private String loginAndCaptureRefreshCookie() throws Exception {
        String setCookie = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("manager@portal.gov", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");
        assertNotNull(setCookie, "Login must issue the refresh-token cookie");
        return setCookie;
    }

    private Cookie refreshCookieFrom(String setCookie) {
        String pair = setCookie.split(";", 2)[0];
        String value = pair.substring(pair.indexOf("refresh_token=") + "refresh_token=".length());
        return new Cookie("refresh_token", value);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login issues an HttpOnly SameSite=Lax Secure refresh cookie scoped to /api/v1/auth by default")
    void login_issuesHardenedRefreshCookie() throws Exception {
        String setCookie = loginAndCaptureRefreshCookie();

        assertTrue(setCookie.startsWith("refresh_token="), "Cookie must carry the opaque refresh token");
        assertTrue(setCookie.contains("HttpOnly"), "Refresh cookie must be HttpOnly");
        assertTrue(setCookie.contains("SameSite=Lax"), "Refresh cookie must be SameSite=Lax");
        assertTrue(setCookie.contains("Path=/api/v1/auth"), "Refresh cookie must be scoped to auth endpoints");
        assertTrue(setCookie.contains("Max-Age=604800"), "Refresh cookie Max-Age must match the 7-day lifetime");
        assertTrue(setCookie.contains("Secure"), "Canonical base configuration serves the cookie with Secure by default");
    }

    @Test
    @DisplayName("RefreshTokenCookieSupport respects secure flag for production vs local dev opt-out")
    void cookieSupport_respectsSecureFlag() {
        org.springframework.http.ResponseCookie secureCookie = RefreshTokenCookieSupport.build("refresh_token", "raw-token", 604800, true);
        assertTrue(secureCookie.isSecure(), "Production/default cookie must be Secure");
        assertTrue(secureCookie.toString().contains("Secure"));

        org.springframework.http.ResponseCookie plainCookie = RefreshTokenCookieSupport.build("refresh_token", "raw-token", 604800, false);
        assertFalse(plainCookie.isSecure(), "Local dev opt-out cookie must not be Secure");
        assertFalse(plainCookie.toString().contains("Secure"));

        org.springframework.http.ResponseCookie clearedPlain = RefreshTokenCookieSupport.clear("refresh_token", false);
        assertFalse(clearedPlain.isSecure());
        assertEquals(0, clearedPlain.getMaxAge().getSeconds());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh rotates the cookie and returns a new access token")
    void refresh_withValidCookie_rotatesAndReturnsAccessToken() throws Exception {
        String originalCookie = loginAndCaptureRefreshCookie();
        Cookie originalRefresh = refreshCookieFrom(originalCookie);

        String refreshedSetCookie = mockMvc.perform(post("/api/v1/auth/refresh").cookie(originalRefresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is("manager@portal.gov")))
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");
        assertNotNull(refreshedSetCookie, "Refresh must set a replacement cookie");

        Cookie rotatedRefresh = refreshCookieFrom(refreshedSetCookie);
        assertNotEquals(originalRefresh.getValue(), rotatedRefresh.getValue(),
                "Rotation must replace the presented refresh token");

        // The old token is single-use: presenting it again is reuse and revokes the family.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(originalRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));

        // The rotated successor is dead too: family revocation covers it.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotatedRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout revokes the family and clears the cookie idempotently")
    void logout_revokesFamilyAndClearsCookie() throws Exception {
        String originalCookie = loginAndCaptureRefreshCookie();
        Cookie refresh = refreshCookieFrom(originalCookie);

        String clearedSetCookie = mockMvc.perform(post("/api/v1/auth/logout").cookie(refresh))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");
        assertNotNull(clearedSetCookie);
        assertTrue(clearedSetCookie.contains("Max-Age=0"), "Logout must expire the refresh cookie");

        // Idempotent repetition and cookie-less logout stay 204 without disclosure.
        mockMvc.perform(post("/api/v1/auth/logout").cookie(refresh)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());

        // The presented family is revoked: refresh with the old cookie is rejected.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh without a cookie returns a generic 401")
    void refresh_withoutCookie_returnsGeneric401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
    }
}
