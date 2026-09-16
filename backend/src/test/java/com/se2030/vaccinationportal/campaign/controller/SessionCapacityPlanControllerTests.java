package com.se2030.vaccinationportal.campaign.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.dto.response.CapacityPlanPreviewResponse;
import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.service.SessionCapacityPlanService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 */
@ExtendWith(MockitoExtension.class)
class SessionCapacityPlanControllerTests {

    private MockMvc mockMvc;

    @Mock
    private SessionCapacityPlanService capacityPlanService;

    private VaccinationCampaign sampleCampaign;
    private VaccinationCenter sampleCenter;
    private VaccinationSession sampleSession;
    private SessionCapacityPlan samplePlanRev1;
    private SessionCapacityPlan samplePlanRev2;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new SessionCapacityPlanController(capacityPlanService))
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalCampaignExceptionHandler())
                .setValidator(validator)
                .build();

        VaccineType sampleVaccine = new VaccineType("VAX-01", "COVID-19", "dose", true);
        TargetGroup sampleTarget = new TargetGroup("TG-01", "General", true);
        sampleCampaign = new VaccinationCampaign(
                "CMP-2026",
                "National COVID 2026",
                "Description",
                sampleVaccine,
                sampleTarget,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-30T23:59:59Z")
        );
        ReflectionTestUtils.setField(sampleCampaign, "id", 100L);

        sampleCenter = new VaccinationCenter("CTR-01", "Downtown Center", "123 Main St", "555-0100", CenterStatus.ACTIVE, 50, 100, "UTC");
        ReflectionTestUtils.setField(sampleCenter, "id", 200L);

        sampleSession = new VaccinationSession(
                sampleCampaign,
                sampleCenter,
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T12:00:00Z"),
                Instant.parse("2026-09-01T07:00:00Z"),
                "UTC"
        );
        ReflectionTestUtils.setField(sampleSession, "id", 300L);

        samplePlanRev1 = new SessionCapacityPlan(
                sampleSession,
                1,
                240,
                10,
                3,
                4,
                3,
                3,
                72,
                72,
                50,
                15,
                0.0,
                100,
                null,
                "admin-1"
        );
        ReflectionTestUtils.setField(samplePlanRev1, "id", 501L);
        ReflectionTestUtils.setField(samplePlanRev1, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(samplePlanRev1, "updatedAt", Instant.parse("2026-08-20T10:00:00Z"));

        samplePlanRev2 = new SessionCapacityPlan(
                sampleSession,
                2,
                240,
                10,
                4,
                4,
                4,
                4,
                96,
                96,
                50,
                15,
                0.0,
                100,
                null,
                "admin-2"
        );
        ReflectionTestUtils.setField(samplePlanRev2, "id", 502L);
        ReflectionTestUtils.setField(samplePlanRev2, "createdAt", Instant.parse("2026-08-20T12:00:00Z"));
        ReflectionTestUtils.setField(samplePlanRev2, "updatedAt", Instant.parse("2026-08-20T12:00:00Z"));
    }

    // --- Preview Endpoints ---

    @Test
    @DisplayName("POST /preview returns 200 with calculated preview parameters")
    void previewCapacityPlan_success() throws Exception {
        CapacityPlanPreviewResponse previewResponse = new CapacityPlanPreviewResponse(
                300L,
                240,
                10,
                3,
                4,
                3,
                3,
                72,
                72,
                50,
                15,
                0.0,
                100,
                false,
                true,
                Instant.parse("2026-08-21T10:00:00Z")
        );

        when(capacityPlanService.previewCapacityPlan(eq(300L), any(PreviewCapacityPlanRequest.class)))
                .thenReturn(previewResponse);

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 3,
                    "approvedCapacity": 72
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(300)))
                .andExpect(jsonPath("$.durationMinutes", is(240)))
                .andExpect(jsonPath("$.expectedServiceMinutes", is(10)))
                .andExpect(jsonPath("$.assignedVaccinatorCount", is(3)))
                .andExpect(jsonPath("$.availableStationCount", is(4)))
                .andExpect(jsonPath("$.fullyEquippedStationCount", is(3)))
                .andExpect(jsonPath("$.effectiveStationCount", is(3)))
                .andExpect(jsonPath("$.recommendedThroughput", is(72)))
                .andExpect(jsonPath("$.projectedApprovedCapacity", is(72)))
                .andExpect(jsonPath("$.overrideRequired", is(false)))
                .andExpect(jsonPath("$.overrideValid", is(true)));

        verify(capacityPlanService).previewCapacityPlan(eq(300L), any(PreviewCapacityPlanRequest.class));
    }

    @Test
    @DisplayName("POST /preview returns 400 on negative fully equipped stations")
    void previewCapacityPlan_invalidPayload_negativeStations() throws Exception {
        String jsonPayload = """
                {
                    "fullyEquippedStationCount": -1
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    @DisplayName("POST /preview returns 400 when fully equipped exceeds available stations")
    void previewCapacityPlan_fullyEquippedExceedsAvailable() throws Exception {
        when(capacityPlanService.previewCapacityPlan(eq(300L), any(PreviewCapacityPlanRequest.class)))
                .thenThrow(new IllegalArgumentException("Fully equipped station count (5) cannot exceed available station count (4)"));

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 5
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("cannot exceed available station count")));
    }

    @Test
    @DisplayName("POST /preview returns 404 when session does not exist")
    void previewCapacityPlan_sessionNotFound() throws Exception {
        when(capacityPlanService.previewCapacityPlan(eq(999L), any(PreviewCapacityPlanRequest.class)))
                .thenThrow(new ResourceNotFoundException("Vaccination session not found with ID: 999"));

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 2
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/999/capacity-plans/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("POST /preview returns 409 when existing slots exceed safe concurrent occupancy")
    void previewCapacityPlan_exceedsConcurrentOccupancy_returns409() throws Exception {
        when(capacityPlanService.previewCapacityPlan(eq(300L), any(PreviewCapacityPlanRequest.class)))
                .thenThrow(new IllegalStateException(
                        "Session time slots exceed safe concurrent occupancy (peak: 60.0, limit: 50 at 2026-09-01T08:00:00Z)"));

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 3
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Session time slots exceed safe concurrent occupancy")));
    }

    // --- Create Revision Endpoints ---

    @Test
    @DisplayName("POST /capacity-plans creates revision 1 and returns 201 with Location header")
    void createCapacityPlan_revision1_success() throws Exception {
        when(capacityPlanService.createCapacityPlan(eq(300L), any(CreateSessionCapacityPlanRequest.class), any()))
                .thenReturn(samplePlanRev1);

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 3,
                    "approvedCapacity": 72,
                    "approvedBy": "admin-1"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/sessions/300/capacity-plans/1"))
                .andExpect(jsonPath("$.id", is(501)))
                .andExpect(jsonPath("$.sessionId", is(300)))
                .andExpect(jsonPath("$.revision", is(1)))
                .andExpect(jsonPath("$.durationMinutes", is(240)))
                .andExpect(jsonPath("$.expectedServiceMinutes", is(10)))
                .andExpect(jsonPath("$.assignedVaccinatorCount", is(3)))
                .andExpect(jsonPath("$.availableStationCount", is(4)))
                .andExpect(jsonPath("$.fullyEquippedStationCount", is(3)))
                .andExpect(jsonPath("$.effectiveStationCount", is(3)))
                .andExpect(jsonPath("$.recommendedThroughput", is(72)))
                .andExpect(jsonPath("$.approvedCapacity", is(72)))
                .andExpect(jsonPath("$.approvedBy", is("admin-1")));

        verify(capacityPlanService).createCapacityPlan(eq(300L), any(CreateSessionCapacityPlanRequest.class), any());
    }

    @Test
    @DisplayName("POST /capacity-plans creates revision 2 when re-approved")
    void createCapacityPlan_revision2_success() throws Exception {
        when(capacityPlanService.createCapacityPlan(eq(300L), any(CreateSessionCapacityPlanRequest.class), any()))
                .thenReturn(samplePlanRev2);

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 4,
                    "approvedCapacity": 96,
                    "approvedBy": "admin-2"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/sessions/300/capacity-plans/2"))
                .andExpect(jsonPath("$.revision", is(2)))
                .andExpect(jsonPath("$.approvedCapacity", is(96)))
                .andExpect(jsonPath("$.effectiveStationCount", is(4)));
    }

    @Test
    @DisplayName("POST /capacity-plans returns 409 when session is not in DRAFT status")
    void createCapacityPlan_nonDraftSession_returns409() throws Exception {
        when(capacityPlanService.createCapacityPlan(eq(300L), any(CreateSessionCapacityPlanRequest.class), any()))
                .thenThrow(new IllegalStateException("Session capacity plans can only be created for sessions in DRAFT status"));

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 3,
                    "approvedCapacity": 72
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("only be created for sessions in DRAFT status")));
    }

    @Test
    @DisplayName("POST /capacity-plans returns 409 when existing slots exceed safe concurrent occupancy")
    void createCapacityPlan_exceedsConcurrentOccupancy_returns409() throws Exception {
        when(capacityPlanService.createCapacityPlan(eq(300L), any(CreateSessionCapacityPlanRequest.class), any()))
                .thenThrow(new IllegalStateException(
                        "Session time slots exceed safe concurrent occupancy (peak: 60.0, limit: 50 at 2026-09-01T08:00:00Z)"));

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 3,
                    "approvedCapacity": 72
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Session time slots exceed safe concurrent occupancy")));
    }

    @Test
    @DisplayName("POST /capacity-plans returns 401 when anonymous user attempts capacity override")
    void createCapacityPlan_unauthenticatedOverride_returns401() throws Exception {
        when(capacityPlanService.createCapacityPlan(eq(300L), any(CreateSessionCapacityPlanRequest.class), any()))
                .thenThrow(new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                        "Full authentication is required to override session capacity"));

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 3,
                    "approvedCapacity": 85,
                    "overrideReason": "Surge demand"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Full authentication is required")));
    }

    @Test
    @DisplayName("POST /capacity-plans returns 403 when non-CenterManager user attempts capacity override")
    void createCapacityPlan_unauthorizedRoleOverride_returns403() throws Exception {
        when(capacityPlanService.createCapacityPlan(eq(300L), any(CreateSessionCapacityPlanRequest.class), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                        "User is not authorized to override session capacity. Role ROLE_CENTER_MANAGER is required."));

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 3,
                    "approvedCapacity": 85,
                    "overrideReason": "Surge demand"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("ROLE_CENTER_MANAGER is required")));
    }

    @Test
    @DisplayName("POST /capacity-plans returns 400 when approved capacity exceeds recommended throughput without override reason")
    void createCapacityPlan_exceedsRecommendedWithoutOverride_returns400() throws Exception {
        when(capacityPlanService.createCapacityPlan(eq(300L), any(CreateSessionCapacityPlanRequest.class), any()))
                .thenThrow(new IllegalArgumentException("An override reason is required when approved capacity exceeds recommended throughput"));

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 3,
                    "approvedCapacity": 85
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("override reason is required")));
    }

    @Test
    @DisplayName("POST /capacity-plans succeeds when approved capacity exceeds recommended with valid override reason")
    void createCapacityPlan_exceedsRecommendedWithOverride_success() throws Exception {
        SessionCapacityPlan overridePlan = new SessionCapacityPlan(
                sampleSession, 1, 240, 10, 3, 4, 3, 3, 72, 85, 50, 15, 0.0, 100, "High demand community surge", "admin-1"
        );
        ReflectionTestUtils.setField(overridePlan, "id", 503L);

        when(capacityPlanService.createCapacityPlan(eq(300L), any(CreateSessionCapacityPlanRequest.class), any()))
                .thenReturn(overridePlan);

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 3,
                    "approvedCapacity": 85,
                    "overrideReason": "High demand community surge",
                    "approvedBy": "admin-1"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approvedCapacity", is(85)))
                .andExpect(jsonPath("$.overrideReason", is("High demand community surge")));
    }

    @Test
    @DisplayName("POST /capacity-plans returns 400 when approved capacity exceeds hard session service ceiling")
    void createCapacityPlan_exceedsHardCeiling_returns400() throws Exception {
        when(capacityPlanService.createCapacityPlan(eq(300L), any(CreateSessionCapacityPlanRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Approved capacity (120) cannot exceed session service ceiling (100)"));

        String jsonPayload = """
                {
                    "fullyEquippedStationCount": 3,
                    "approvedCapacity": 120,
                    "overrideReason": "Special VIP session",
                    "approvedBy": "admin-1"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/300/capacity-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("cannot exceed session service ceiling")));
    }

    // --- Inspection / GET Endpoints ---

    @Test
    @DisplayName("GET /capacity-plans returns list of revisions ordered ascending")
    void listCapacityPlans_success() throws Exception {
        when(capacityPlanService.getCapacityPlansForSession(300L))
                .thenReturn(List.of(samplePlanRev1, samplePlanRev2));

        mockMvc.perform(get("/api/v1/sessions/300/capacity-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].revision", is(1)))
                .andExpect(jsonPath("$[0].approvedCapacity", is(72)))
                .andExpect(jsonPath("$[1].revision", is(2)))
                .andExpect(jsonPath("$[1].approvedCapacity", is(96)));

        verify(capacityPlanService).getCapacityPlansForSession(300L);
    }

    @Test
    @DisplayName("GET /capacity-plans/latest returns latest revision")
    void getLatestCapacityPlan_success() throws Exception {
        when(capacityPlanService.getLatestCapacityPlan(300L))
                .thenReturn(samplePlanRev2);

        mockMvc.perform(get("/api/v1/sessions/300/capacity-plans/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision", is(2)))
                .andExpect(jsonPath("$.approvedCapacity", is(96)))
                .andExpect(jsonPath("$.approvedBy", is("admin-2")));

        verify(capacityPlanService).getLatestCapacityPlan(300L);
    }

    @Test
    @DisplayName("GET /capacity-plans/latest returns 404 when no plan has been created")
    void getLatestCapacityPlan_notFound() throws Exception {
        when(capacityPlanService.getLatestCapacityPlan(300L))
                .thenThrow(new ResourceNotFoundException("Session capacity plan not found for session ID: 300"));

        mockMvc.perform(get("/api/v1/sessions/300/capacity-plans/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("GET /capacity-plans/{revision} returns specific historical revision")
    void getCapacityPlanByRevision_success() throws Exception {
        when(capacityPlanService.getCapacityPlanByRevision(300L, 1))
                .thenReturn(samplePlanRev1);

        mockMvc.perform(get("/api/v1/sessions/300/capacity-plans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision", is(1)))
                .andExpect(jsonPath("$.approvedCapacity", is(72)))
                .andExpect(jsonPath("$.approvedBy", is("admin-1")));

        verify(capacityPlanService).getCapacityPlanByRevision(300L, 1);
    }

    @Test
    @DisplayName("GET /capacity-plans/{revision} returns 404 when revision not found")
    void getCapacityPlanByRevision_notFound() throws Exception {
        when(capacityPlanService.getCapacityPlanByRevision(300L, 99))
                .thenThrow(new ResourceNotFoundException("Session capacity plan revision 99 not found for session ID: 300"));

        mockMvc.perform(get("/api/v1/sessions/300/capacity-plans/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
