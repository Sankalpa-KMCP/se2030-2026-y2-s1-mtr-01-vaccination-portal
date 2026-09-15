package com.se2030.vaccinationportal.campaign;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprehensive H01 Security Integration Tests for Center, Operational Resource, and Staff Roster endpoints.
 * Validates role-based authorization, anonymous denial, and public center discovery preservation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CenterSecurityAuthorizationIntegrationTests {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private OperationalStaffRosterRepository rosterRepository;

    private VaccinationCenter center;
    private OperationalResource resource;
    private OperationalStaffRoster rosterEntry;

    @BeforeEach
    void setUp() {
        center = centerRepository.save(new VaccinationCenter(
                "CTR-SEC-01", "Security Center 1", "789 Health Ave", "contact@center.gov",
                com.se2030.vaccinationportal.campaign.model.CenterStatus.ACTIVE, 50, 150, "UTC"
        ));

        resource = resourceRepository.save(new OperationalResource(
                center, "RES-01", ResourceType.VACCINATION_STATION, "Station Alpha", OperationalResourceStatus.ACTIVE
        ));

        rosterEntry = rosterRepository.save(new OperationalStaffRoster(
                center, 1001L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE
        ));
    }

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    // =========================================================================
    // 1. ADMINISTRATIVE CENTER READS REQUIRE AUTHORIZED STAFF ROLES (MF02-R-001)
    // =========================================================================

    @Nested
    @DisplayName("Administrative Center Reads")
    class AdministrativeCenterReads {

        @Test
        @DisplayName("Anonymous GET center endpoints are denied 403")
        void anonymousCenterReadsDenied() throws Exception {
            mockMvc.perform(get("/api/v1/centers"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/centers/" + center.getId()))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/operating-hours"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/closures"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN GET center endpoints are denied 403")
        void citizenCenterReadsDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            mockMvc.perform(get("/api/v1/centers")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/centers/" + center.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/operating-hours")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/closures")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_INVENTORY_OFFICER GET center endpoints are denied 403")
        void inventoryOfficerCenterReadsDenied() throws Exception {
            String auth = token(601L, "inventory@portal.gov", List.of("ROLE_INVENTORY_OFFICER"));

            mockMvc.perform(get("/api/v1/centers")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/centers/" + center.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/operating-hours")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/closures")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Authorized staff roles succeed on all center GET endpoints with 200 OK")
        void authorizedStaffRolesSucceed() throws Exception {
            List<String> authorizedRoles = List.of(
                    "ROLE_ADMIN",
                    "ROLE_CAMPAIGN_OFFICER",
                    "ROLE_CENTER_MANAGER",
                    "ROLE_HEALTHCARE_OFFICER",
                    "ROLE_MANAGEMENT_USER"
            );

            long userId = 101L;
            for (String role : authorizedRoles) {
                String auth = token(userId++, role.toLowerCase() + "@portal.gov", List.of(role));

                mockMvc.perform(get("/api/v1/centers")
                                .header(HttpHeaders.AUTHORIZATION, auth))
                        .andExpect(status().isOk());

                mockMvc.perform(get("/api/v1/centers/" + center.getId())
                                .header(HttpHeaders.AUTHORIZATION, auth))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id", is(center.getId().intValue())));

                mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/operating-hours")
                                .header(HttpHeaders.AUTHORIZATION, auth))
                        .andExpect(status().isOk());

                mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/closures")
                                .header(HttpHeaders.AUTHORIZATION, auth))
                        .andExpect(status().isOk());
            }
        }
    }

    // =========================================================================
    // 2. CENTER MUTATIONS REQUIRE ADMIN OR CENTER_MANAGER
    // =========================================================================

    @Nested
    @DisplayName("Center Mutation Authorization")
    class CenterMutationAuthorization {

        @Test
        @DisplayName("Anonymous POST /api/v1/centers is denied 403")
        void anonymousCreateCenterDenied() throws Exception {
            String payload = """
                    {
                        "code": "CTR-ANON-NEW",
                        "name": "Anonymous Center",
                        "address": "123 Hidden Lane",
                        "contact": "anon@center.gov",
                        "safeConcurrentOccupancy": 30,
                        "sessionServiceCeiling": 100,
                        "timezone": "UTC"
                    }
                    """;

            mockMvc.perform(post("/api/v1/centers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN POST /api/v1/centers is denied 403")
        void citizenCreateCenterDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            String payload = """
                    {
                        "code": "CTR-CIT-NEW",
                        "name": "Citizen Center",
                        "address": "123 Citizen Lane",
                        "contact": "citizen@center.gov",
                        "safeConcurrentOccupancy": 30,
                        "sessionServiceCeiling": 100,
                        "timezone": "UTC"
                    }
                    """;

            mockMvc.perform(post("/api/v1/centers")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_ADMIN POST /api/v1/centers succeeds 201")
        void adminCreateCenterSucceeds() throws Exception {
            String auth = token(101L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            String payload = """
                    {
                        "code": "CTR-ADM-01",
                        "name": "Admin Center",
                        "address": "100 Admin Blvd",
                        "contact": "admin@center.gov",
                        "safeConcurrentOccupancy": 40,
                        "sessionServiceCeiling": 120,
                        "timezone": "UTC"
                    }
                    """;

            mockMvc.perform(post("/api/v1/centers")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code", is("CTR-ADM-01")));
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER PUT /api/v1/centers/{id} succeeds 200")
        void managerUpdateCenterSucceeds() throws Exception {
            String auth = token(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            String payload = """
                    {
                        "name": "Updated Center Name",
                        "address": "789 Health Ave Updated",
                        "contact": "updated@center.gov",
                        "safeConcurrentOccupancy": 60,
                        "sessionServiceCeiling": 180
                    }
                    """;

            mockMvc.perform(put("/api/v1/centers/" + center.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Updated Center Name")));
        }
    }

    // =========================================================================
    // 3. OPERATIONAL RESOURCE AUTHORIZATION
    // =========================================================================

    @Nested
    @DisplayName("Operational Resource Authorization")
    class ResourceAuthorization {

        @Test
        @DisplayName("Anonymous GET resources is denied 403")
        void anonymousListResourcesDenied() throws Exception {
            mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/resources"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN GET resources is denied 403")
        void citizenListResourcesDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/resources")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER GET resources succeeds 200")
        void healthcareOfficerListResourcesSucceeds() throws Exception {
            String auth = token(301L, "hco@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/resources")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER POST resources succeeds 201")
        void centerManagerCreateResourceSucceeds() throws Exception {
            String auth = token(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            String payload = """
                    {
                        "code": "RES-MGR-01",
                        "resourceType": "VACCINATION_STATION",
                        "name": "Station Bravo",
                        "status": "ACTIVE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/centers/" + center.getId() + "/resources")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code", is("RES-MGR-01")));
        }
    }

    // =========================================================================
    // 4. STAFF ROSTER AUTHORIZATION
    // =========================================================================

    @Nested
    @DisplayName("Staff Roster Authorization")
    class StaffRosterAuthorization {

        @Test
        @DisplayName("Anonymous GET staff-roster is denied 403")
        void anonymousListRosterDenied() throws Exception {
            mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/staff-roster"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN GET staff-roster is denied 403")
        void citizenListRosterDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            mockMvc.perform(get("/api/v1/centers/" + center.getId() + "/staff-roster")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER POST staff-roster succeeds 201")
        void managerRegisterStaffSucceeds() throws Exception {
            String auth = token(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            String payload = """
                    {
                        "userId": 2002,
                        "role": "CHECK_IN_STAFF",
                        "status": "ACTIVE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/centers/" + center.getId() + "/staff-roster")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId", is(2002)));
        }
    }
}
