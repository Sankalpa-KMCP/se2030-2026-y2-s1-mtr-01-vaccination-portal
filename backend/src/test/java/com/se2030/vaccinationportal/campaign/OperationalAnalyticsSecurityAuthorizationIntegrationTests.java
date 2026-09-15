package com.se2030.vaccinationportal.campaign;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Method-security integration tests for GET /api/v1/analytics/operations endpoint.
 * Exercises Spring Security @PreAuthorize filter chain with real JWT tokens.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class OperationalAnalyticsSecurityAuthorizationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    @Nested
    @DisplayName("Allowed Roles (ADMIN, CAMPAIGN_OFFICER, CENTER_MANAGER)")
    class AllowedRoles {

        @Test
        @DisplayName("ADMIN role receives 200 OK for operational analytics")
        void adminRoleAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/operations")
                            .header(HttpHeaders.AUTHORIZATION, token(1L, "admin@test.gov", List.of("ROLE_ADMIN")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slotUtilizationPercentage").exists())
                    .andExpect(jsonPath("$.campaignCountByStatus").exists());
        }

        @Test
        @DisplayName("CAMPAIGN_OFFICER role receives 200 OK for operational analytics")
        void campaignOfficerRoleAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/operations")
                            .header(HttpHeaders.AUTHORIZATION, token(2L, "co@test.gov", List.of("ROLE_CAMPAIGN_OFFICER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slotUtilizationPercentage").exists());
        }

        @Test
        @DisplayName("CENTER_MANAGER role receives 200 OK for operational analytics")
        void centerManagerRoleAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/operations")
                            .header(HttpHeaders.AUTHORIZATION, token(3L, "cm@test.gov", List.of("ROLE_CENTER_MANAGER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slotUtilizationPercentage").exists());
        }
    }

    @Nested
    @DisplayName("Denied Roles (MANAGEMENT_USER, HEALTHCARE_OFFICER, CITIZEN, INVENTORY_OFFICER)")
    class DeniedRoles {

        @Test
        @DisplayName("MANAGEMENT_USER role is denied with 403 Forbidden")
        void managementUserDenied() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/operations")
                            .header(HttpHeaders.AUTHORIZATION, token(4L, "mgmt@test.gov", List.of("ROLE_MANAGEMENT_USER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("HEALTHCARE_OFFICER role is denied with 403 Forbidden")
        void healthcareOfficerDenied() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/operations")
                            .header(HttpHeaders.AUTHORIZATION, token(5L, "hco@test.gov", List.of("ROLE_HEALTHCARE_OFFICER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CITIZEN role is denied with 403 Forbidden")
        void citizenDenied() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/operations")
                            .header(HttpHeaders.AUTHORIZATION, token(6L, "cit@test.gov", List.of("ROLE_CITIZEN")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INVENTORY_OFFICER role is denied with 403 Forbidden")
        void inventoryOfficerDenied() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/operations")
                            .header(HttpHeaders.AUTHORIZATION, token(7L, "inv@test.gov", List.of("ROLE_INVENTORY_OFFICER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Unauthenticated / Anonymous Requests")
    class UnauthenticatedRequests {

        @Test
        @DisplayName("Anonymous request without token is rejected (403 Forbidden under method security)")
        void anonymousRequestRejected() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/operations")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }
}
