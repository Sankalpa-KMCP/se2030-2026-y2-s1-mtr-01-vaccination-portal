package com.se2030.vaccinationportal.campaign;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SessionCapacityPlanSecurityIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private SessionCapacityPlanRepository planRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private OperationalStaffRosterRepository staffRosterRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private SessionResourceAssignmentRepository resourceAssignmentRepository;

    private VaccinationSession session;
    private final Instant sessionStart = Instant.parse("2026-11-01T08:00:00Z");
    private final Instant sessionEnd = Instant.parse("2026-11-01T12:00:00Z"); // 240 min

    @BeforeEach
    void setUp() {
        VaccinationCenter center = centerRepository.save(new VaccinationCenter(
                "CTR-SEC-01", "Security Test Center", "404 Guard Road", "+9999",
                CenterStatus.ACTIVE, 60, 100, "UTC"
        ));

        VaccineType vaccine = vaccineTypeRepository.save(new VaccineType("VAX-SEC-01", "Security Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-SEC-01", "Security Target"));

        VaccinationCampaign campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-SEC-01", "Security Campaign", "Testing Security",
                vaccine, targetGroup,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-12-31T00:00:00Z"),
                CampaignStatus.DRAFT
        ));

        doseProgrammeRepository.save(new CampaignDoseProgramme(
                campaign, 1, DoseCategory.PRIMARY, "1", 21, false, 10,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31)
        ));

        session = sessionRepository.save(new VaccinationSession(
                campaign, center, sessionStart, sessionEnd, Instant.parse("2026-11-01T07:00:00Z"), "UTC"
        ));

        // 2 active vaccinators, 2 active stations -> recommended throughput = 2 * (240 / 10) = 48
        OperationalStaffRoster r1 = staffRosterRepository.save(new OperationalStaffRoster(center, 201L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE));
        OperationalStaffRoster r2 = staffRosterRepository.save(new OperationalStaffRoster(center, 202L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE));
        staffAssignmentRepository.save(new StaffAssignment(session, r1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        staffAssignmentRepository.save(new StaffAssignment(session, r2, StaffRole.VACCINATOR, sessionStart, sessionEnd));

        OperationalResource s1 = resourceRepository.save(new OperationalResource(center, "ST-1", ResourceType.VACCINATION_STATION, "St 1", OperationalResourceStatus.ACTIVE));
        OperationalResource s2 = resourceRepository.save(new OperationalResource(center, "ST-2", ResourceType.VACCINATION_STATION, "St 2", OperationalResourceStatus.ACTIVE));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, s1, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, s2, sessionStart, sessionEnd));
    }

    private String createBearerToken(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    @Test
    @DisplayName("Anonymous normal capacity plan within recommended throughput succeeds with 201 and null approvedBy")
    void normalCapacityPlan_anonymous_succeeds() throws Exception {
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 48,
                    "approvedBy": "client-spoof-ignored"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/sessions/" + session.getId() + "/capacity-plans/1"))
                .andExpect(jsonPath("$.revision", is(1)))
                .andExpect(jsonPath("$.recommendedThroughput", is(48)))
                .andExpect(jsonPath("$.approvedCapacity", is(48)))
                .andExpect(jsonPath("$.approvedBy").doesNotExist());

        SessionCapacityPlan plan = planRepository.findTopBySessionIdOrderByRevisionDesc(session.getId()).orElseThrow();
        assertNull(plan.getApprovedBy());
    }

    @Test
    @DisplayName("Anonymous capacity override (> recommended) is rejected with 401 Unauthorized")
    void overrideCapacityPlan_anonymous_rejected401() throws Exception {
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 60,
                    "overrideReason": "Community surge demand",
                    "approvedBy": "spoofed-user"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Full authentication is required")));
    }

    @Test
    @DisplayName("ROLE_CITIZEN capacity override (> recommended) is rejected with 403 Forbidden")
    void overrideCapacityPlan_citizenRole_rejected403() throws Exception {
        String token = createBearerToken(101L, "citizen@portal.local", List.of("ROLE_CITIZEN"));
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 60,
                    "overrideReason": "Community surge demand"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("ROLE_CENTER_MANAGER is required")));
    }

    @Test
    @DisplayName("ROLE_ADMIN capacity override (> recommended) is rejected with 403 Forbidden under deny-by-default")
    void overrideCapacityPlan_adminRole_rejected403() throws Exception {
        String token = createBearerToken(1L, "admin@portal.local", List.of("ROLE_ADMIN"));
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 60,
                    "overrideReason": "Administrative surge"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("ROLE_CENTER_MANAGER is required")));
    }

    @Test
    @DisplayName("ROLE_HEALTHCARE_OFFICER capacity override (> recommended) is rejected with 403 Forbidden")
    void overrideCapacityPlan_healthcareOfficer_rejected403() throws Exception {
        String token = createBearerToken(102L, "hco@portal.local", List.of("ROLE_HEALTHCARE_OFFICER"));
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 60,
                    "overrideReason": "Healthcare surge"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("ROLE_CAMPAIGN_OFFICER capacity override (> recommended) is rejected with 403 Forbidden")
    void overrideCapacityPlan_campaignOfficer_rejected403() throws Exception {
        String token = createBearerToken(103L, "co@portal.local", List.of("ROLE_CAMPAIGN_OFFICER"));
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 60,
                    "overrideReason": "Campaign surge"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("ROLE_CENTER_MANAGER capacity override with valid reason succeeds with 201 and trusted user ID audit")
    void overrideCapacityPlan_centerManager_succeedsWithTrustedAudit() throws Exception {
        String token = createBearerToken(42L, "manager@portal.local", List.of("ROLE_CENTER_MANAGER"));
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 60,
                    "overrideReason": "Special surge approval",
                    "approvedBy": "client-spoofed-value-9999"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.revision", is(1)))
                .andExpect(jsonPath("$.approvedCapacity", is(60)))
                .andExpect(jsonPath("$.overrideReason", is("Special surge approval")))
                .andExpect(jsonPath("$.approvedBy", is("42")));

        SessionCapacityPlan plan = planRepository.findTopBySessionIdOrderByRevisionDesc(session.getId()).orElseThrow();
        assertEquals("42", plan.getApprovedBy()); // Ignored client input, persisted trusted actor user ID
    }

    @Test
    @DisplayName("ROLE_CENTER_MANAGER normal capacity plan persists trusted user ID audit")
    void normalCapacityPlan_centerManager_persistsTrustedAudit() throws Exception {
        String token = createBearerToken(42L, "manager@portal.local", List.of("ROLE_CENTER_MANAGER"));
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 48,
                    "approvedBy": "client-spoofed-admin"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approvedCapacity", is(48)))
                .andExpect(jsonPath("$.approvedBy", is("42")));

        SessionCapacityPlan plan = planRepository.findTopBySessionIdOrderByRevisionDesc(session.getId()).orElseThrow();
        assertEquals("42", plan.getApprovedBy());
    }

    @Test
    @DisplayName("ROLE_CENTER_MANAGER override without reason is rejected with 400 Bad Request")
    void overrideCapacityPlan_missingReason_rejected400() throws Exception {
        String token = createBearerToken(42L, "manager@portal.local", List.of("ROLE_CENTER_MANAGER"));
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 60
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("override reason is required")));
    }

    @Test
    @DisplayName("ROLE_CENTER_MANAGER override exceeding ceiling is rejected with 400 Bad Request")
    void overrideCapacityPlan_exceedingCeiling_rejected400() throws Exception {
        String token = createBearerToken(42L, "manager@portal.local", List.of("ROLE_CENTER_MANAGER"));
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 120,
                    "overrideReason": "Exceeding hard ceiling"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("cannot exceed session service ceiling")));
    }

    @Test
    @DisplayName("POST /preview remains unauthenticated and calculates preview accurately")
    void previewCapacityPlan_unauthenticated_succeeds() throws Exception {
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 60,
                    "overrideReason": "Previewing surge"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedThroughput", is(48)))
                .andExpect(jsonPath("$.projectedApprovedCapacity", is(60)))
                .andExpect(jsonPath("$.overrideRequired", is(true)))
                .andExpect(jsonPath("$.overrideValid", is(true)));
    }

    @Test
    @DisplayName("GET history endpoints remain accessible under staged rollout")
    void getHistory_endpointsAccessible() throws Exception {
        // Create revision 1
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 48
                }
                """;
        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/capacity-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revision", is(1)))
                .andExpect(jsonPath("$[0].approvedCapacity", is(48)));

        mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/capacity-plans/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision", is(1)));

        mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/capacity-plans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision", is(1)));
    }
}
