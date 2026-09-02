package com.se2030.vaccinationportal.vaccination;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.VaccinationPortalApplication;
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
 * Spring Security integration tests for {@code /api/v1/vaccination/drafts} endpoints (MF-05).
 * Tests method security and HTTP boundary enforcement for allowed and denied roles.
 */
@SpringBootTest(classes = VaccinationPortalApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class VaccinationDraftSecurityAuthorizationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    @Nested
    @DisplayName("Allowed Roles (ADMIN, HEALTHCARE_OFFICER)")
    class AllowedRoles {

        @Test
        @DisplayName("ADMIN role is allowed to access GET /api/v1/vaccination/drafts")
        void adminAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/vaccination/drafts")
                            .header(HttpHeaders.AUTHORIZATION, token(1L, "admin@test.gov", List.of("ROLE_ADMIN")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HEALTHCARE_OFFICER role is allowed to access GET /api/v1/vaccination/drafts")
        void healthcareOfficerAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/vaccination/drafts")
                            .header(HttpHeaders.AUTHORIZATION, token(2L, "hco@test.gov", List.of("ROLE_HEALTHCARE_OFFICER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Denied Roles (CITIZEN, CENTER_MANAGER, CAMPAIGN_OFFICER, INVENTORY_OFFICER, MANAGEMENT_USER)")
    class DeniedRoles {

        @Test
        @DisplayName("CITIZEN role is denied with 403 Forbidden on GET drafts")
        void citizenDenied() throws Exception {
            mockMvc.perform(get("/api/v1/vaccination/drafts")
                            .header(HttpHeaders.AUTHORIZATION, token(3L, "citizen@test.gov", List.of("ROLE_CITIZEN")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CENTER_MANAGER role is denied with 403 Forbidden on GET drafts")
        void centerManagerDenied() throws Exception {
            mockMvc.perform(get("/api/v1/vaccination/drafts")
                            .header(HttpHeaders.AUTHORIZATION, token(4L, "cm@test.gov", List.of("ROLE_CENTER_MANAGER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CAMPAIGN_OFFICER role is denied with 403 Forbidden on POST draft")
        void campaignOfficerDenied() throws Exception {
            mockMvc.perform(post("/api/v1/vaccination/drafts")
                            .header(HttpHeaders.AUTHORIZATION, token(5L, "co@test.gov", List.of("ROLE_CAMPAIGN_OFFICER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"appointmentId\":1}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Unauthenticated / Anonymous Requests")
    class UnauthenticatedRequests {

        @Test
        @DisplayName("Anonymous request to GET /api/v1/vaccination/drafts returns 401/403")
        void anonymousDraftsListRejected() throws Exception {
            mockMvc.perform(get("/api/v1/vaccination/drafts")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Anonymous request to POST /api/v1/vaccination/drafts returns 401/403")
        void anonymousDraftCreateRejected() throws Exception {
            mockMvc.perform(post("/api/v1/vaccination/drafts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"appointmentId\":1}"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
