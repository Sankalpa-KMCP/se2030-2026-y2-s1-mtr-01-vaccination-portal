package com.se2030.vaccinationportal.campaign;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Security Integration Tests for Master Data lookup endpoints (VaccineType and TargetGroup).
 * Validates role-based authorization matrix and unauthenticated access behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MasterDataSecurityAuthorizationIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    @BeforeEach
    void setUp() {
        vaccineTypeRepository.save(new VaccineType("VAX-MD-SEC-01", "Security Master Vaccine", "dose", true));
        targetGroupRepository.save(new TargetGroup("TG-MD-SEC-01", "Security Master Target", true));
    }

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    // =========================================================================
    // 1. GET /api/v1/vaccine-types
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/vaccine-types Authorization")
    class GetVaccineTypesSecurity {

        @Test
        @DisplayName("ROLE_ADMIN is authorized (200 OK)")
        void adminCanListVaccineTypes() throws Exception {
            String auth = token(1L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            mockMvc.perform(get("/api/v1/vaccine-types")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER is authorized (200 OK)")
        void campaignOfficerCanListVaccineTypes() throws Exception {
            String auth = token(2L, "campaign@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            mockMvc.perform(get("/api/v1/vaccine-types")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER is denied (403 Forbidden)")
        void centerManagerDenied() throws Exception {
            String auth = token(3L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            mockMvc.perform(get("/api/v1/vaccine-types")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER is denied (403 Forbidden)")
        void healthcareOfficerDenied() throws Exception {
            String auth = token(4L, "officer@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            mockMvc.perform(get("/api/v1/vaccine-types")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_MANAGEMENT_USER is denied (403 Forbidden)")
        void managementUserDenied() throws Exception {
            String auth = token(5L, "mgmt@portal.gov", List.of("ROLE_MANAGEMENT_USER"));
            mockMvc.perform(get("/api/v1/vaccine-types")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN is denied (403 Forbidden)")
        void citizenDenied() throws Exception {
            String auth = token(6L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            mockMvc.perform(get("/api/v1/vaccine-types")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_INVENTORY_OFFICER is denied (403 Forbidden)")
        void inventoryOfficerDenied() throws Exception {
            String auth = token(7L, "inventory@portal.gov", List.of("ROLE_INVENTORY_OFFICER"));
            mockMvc.perform(get("/api/v1/vaccine-types")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated request is rejected (401 or 403)")
        void unauthenticatedDenied() throws Exception {
            mockMvc.perform(get("/api/v1/vaccine-types"))
                    .andExpect(status().is4xxClientError());
        }
    }

    // =========================================================================
    // 2. GET /api/v1/target-groups
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/target-groups Authorization")
    class GetTargetGroupsSecurity {

        @Test
        @DisplayName("ROLE_ADMIN is authorized (200 OK)")
        void adminCanListTargetGroups() throws Exception {
            String auth = token(1L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            mockMvc.perform(get("/api/v1/target-groups")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER is authorized (200 OK)")
        void campaignOfficerCanListTargetGroups() throws Exception {
            String auth = token(2L, "campaign@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            mockMvc.perform(get("/api/v1/target-groups")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER is denied (403 Forbidden)")
        void centerManagerDenied() throws Exception {
            String auth = token(3L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            mockMvc.perform(get("/api/v1/target-groups")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER is denied (403 Forbidden)")
        void healthcareOfficerDenied() throws Exception {
            String auth = token(4L, "officer@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            mockMvc.perform(get("/api/v1/target-groups")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_MANAGEMENT_USER is denied (403 Forbidden)")
        void managementUserDenied() throws Exception {
            String auth = token(5L, "mgmt@portal.gov", List.of("ROLE_MANAGEMENT_USER"));
            mockMvc.perform(get("/api/v1/target-groups")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN is denied (403 Forbidden)")
        void citizenDenied() throws Exception {
            String auth = token(6L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            mockMvc.perform(get("/api/v1/target-groups")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_INVENTORY_OFFICER is denied (403 Forbidden)")
        void inventoryOfficerDenied() throws Exception {
            String auth = token(7L, "inventory@portal.gov", List.of("ROLE_INVENTORY_OFFICER"));
            mockMvc.perform(get("/api/v1/target-groups")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated request is rejected (401 or 403)")
        void unauthenticatedDenied() throws Exception {
            mockMvc.perform(get("/api/v1/target-groups"))
                    .andExpect(status().is4xxClientError());
        }
    }

    // =========================================================================
    // 3. POST /api/v1/vaccine-types
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/vaccine-types Authorization")
    class CreateVaccineTypeSecurity {

        @Test
        @DisplayName("ROLE_ADMIN is authorized (201 Created)")
        void adminCanCreate() throws Exception {
            String auth = token(1L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            String json = """
                    {"code": "VAX-SEC-02", "name": "Sec Vaccine 2", "doseUnit": "dose"}
                    """;
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/vaccine-types")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER is authorized (201 Created)")
        void campaignOfficerCanCreate() throws Exception {
            String auth = token(2L, "campaign@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            String json = """
                    {"code": "VAX-SEC-03", "name": "Sec Vaccine 3", "doseUnit": "dose"}
                    """;
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/vaccine-types")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER is denied (403 Forbidden)")
        void centerManagerDenied() throws Exception {
            String auth = token(3L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            String json = """
                    {"code": "VAX-SEC-04", "name": "Sec Vaccine 4", "doseUnit": "dose"}
                    """;
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/vaccine-types")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN is denied (403 Forbidden)")
        void citizenDenied() throws Exception {
            String auth = token(6L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            String json = """
                    {"code": "VAX-SEC-05", "name": "Sec Vaccine 5", "doseUnit": "dose"}
                    """;
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/vaccine-types")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 4. POST /api/v1/target-groups
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/target-groups Authorization")
    class CreateTargetGroupSecurity {

        @Test
        @DisplayName("ROLE_ADMIN is authorized (201 Created)")
        void adminCanCreate() throws Exception {
            String auth = token(1L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            String json = """
                    {"code": "TG-SEC-02", "description": "Sec Target 2"}
                    """;
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/target-groups")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER is authorized (201 Created)")
        void campaignOfficerCanCreate() throws Exception {
            String auth = token(2L, "campaign@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            String json = """
                    {"code": "TG-SEC-03", "description": "Sec Target 3"}
                    """;
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/target-groups")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER is denied (403 Forbidden)")
        void healthcareOfficerDenied() throws Exception {
            String auth = token(4L, "officer@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            String json = """
                    {"code": "TG-SEC-04", "description": "Sec Target 4"}
                    """;
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/target-groups")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isForbidden());
        }
    }
}
