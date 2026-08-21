package com.se2030.vaccinationportal.campaign.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.service.SessionResourceAssignmentService;
import com.se2030.vaccinationportal.campaign.service.StaffAssignmentService;
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

@ExtendWith(MockitoExtension.class)
class SessionOperationalAssignmentControllerTests {

    private MockMvc mockMvc;

    @Mock
    private SessionResourceAssignmentService resourceAssignmentService;

    @Mock
    private StaffAssignmentService staffAssignmentService;

    private VaccinationCampaign sampleCampaign;
    private VaccinationCenter sampleCenter;
    private VaccinationSession sampleSession;
    private OperationalResource sampleResource;
    private OperationalStaffRoster sampleRoster;
    private SessionResourceAssignment sampleResourceAssignment;
    private StaffAssignment sampleStaffAssignment;

    private final Instant sessionStart = Instant.parse("2026-09-01T08:00:00Z");
    private final Instant sessionEnd = Instant.parse("2026-09-01T16:00:00Z");
    private final Instant assignmentStart = Instant.parse("2026-09-01T08:00:00Z");
    private final Instant assignmentEnd = Instant.parse("2026-09-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new SessionOperationalAssignmentController(resourceAssignmentService, staffAssignmentService))
                .setControllerAdvice(new GlobalCampaignExceptionHandler())
                .setValidator(validator)
                .build();

        VaccineType sampleVaccine = new VaccineType("VAX-01", "Polio", "dose", true);
        TargetGroup sampleTarget = new TargetGroup("TG-01", "Children", true);
        sampleCampaign = new VaccinationCampaign(
                "NP-2026",
                "National Polio 2026",
                "Description",
                sampleVaccine,
                sampleTarget,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-30T23:59:59Z")
        );
        ReflectionTestUtils.setField(sampleCampaign, "id", 1L);

        sampleCenter = new VaccinationCenter(
                "CTR-COLOMBO-01",
                "Colombo Central Vaccination Center",
                "123 Main Street, Colombo 01",
                "+94 11 2345678",
                CenterStatus.ACTIVE,
                150,
                50,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(sampleCenter, "id", 10L);

        sampleSession = new VaccinationSession(
                sampleCampaign,
                sampleCenter,
                sessionStart,
                sessionEnd,
                sessionStart.minusSeconds(3600),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(sampleSession, "id", 100L);

        sampleResource = new OperationalResource(
                sampleCenter,
                "STATION-01",
                ResourceType.VACCINATION_STATION,
                "Vaccination Booth 1",
                OperationalResourceStatus.ACTIVE
        );
        ReflectionTestUtils.setField(sampleResource, "id", 200L);

        sampleRoster = new OperationalStaffRoster(
                sampleCenter,
                501L,
                StaffRole.VACCINATOR,
                OperationalStaffStatus.ACTIVE
        );
        ReflectionTestUtils.setField(sampleRoster, "id", 300L);

        sampleResourceAssignment = new SessionResourceAssignment(
                sampleSession,
                sampleResource,
                assignmentStart,
                assignmentEnd
        );
        ReflectionTestUtils.setField(sampleResourceAssignment, "id", 400L);
        ReflectionTestUtils.setField(sampleResourceAssignment, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(sampleResourceAssignment, "updatedAt", Instant.parse("2026-08-20T10:00:00Z"));

        sampleStaffAssignment = new StaffAssignment(
                sampleSession,
                sampleRoster,
                StaffRole.VACCINATOR,
                assignmentStart,
                assignmentEnd
        );
        ReflectionTestUtils.setField(sampleStaffAssignment, "id", 500L);
        ReflectionTestUtils.setField(sampleStaffAssignment, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(sampleStaffAssignment, "updatedAt", Instant.parse("2026-08-20T10:00:00Z"));
    }

    // ==========================================
    // RESOURCE ASSIGNMENT TESTS
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/resource-assignments - valid payload creates assignment and returns 201 with Location header")
    void createResourceAssignment_withValidPayload_returns201() throws Exception {
        when(resourceAssignmentService.createAssignment(eq(100L), eq(200L), eq(assignmentStart), eq(assignmentEnd)))
                .thenReturn(sampleResourceAssignment);

        String json = """
                {
                    "resourceId": 200,
                    "startAt": "2026-09-01T08:00:00Z",
                    "endAt": "2026-09-01T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/100/resource-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/sessions/100/resource-assignments/400"))
                .andExpect(jsonPath("$.id", is(400)))
                .andExpect(jsonPath("$.sessionId", is(100)))
                .andExpect(jsonPath("$.resourceId", is(200)))
                .andExpect(jsonPath("$.resourceCode", is("STATION-01")))
                .andExpect(jsonPath("$.resourceType", is("VACCINATION_STATION")))
                .andExpect(jsonPath("$.resourceName", is("Vaccination Booth 1")))
                .andExpect(jsonPath("$.startAt", is("2026-09-01T08:00:00Z")))
                .andExpect(jsonPath("$.endAt", is("2026-09-01T12:00:00Z")))
                .andExpect(jsonPath("$.createdAt", is(notNullValue())));

        verify(resourceAssignmentService).createAssignment(eq(100L), eq(200L), eq(assignmentStart), eq(assignmentEnd));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/resource-assignments - missing required fields returns 400")
    void createResourceAssignment_withMissingFields_returns400() throws Exception {
        String json = "{}";

        mockMvc.perform(post("/api/v1/sessions/100/resource-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors", hasSize(3)));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/resource-assignments - invalid interval (start >= end) returns 400")
    void createResourceAssignment_withInvalidInterval_returns400() throws Exception {
        when(resourceAssignmentService.createAssignment(eq(100L), eq(200L), eq(assignmentEnd), eq(assignmentStart)))
                .thenThrow(new IllegalArgumentException("Assignment start time must be before end time"));

        String json = """
                {
                    "resourceId": 200,
                    "startAt": "2026-09-01T12:00:00Z",
                    "endAt": "2026-09-01T08:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/100/resource-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Assignment start time must be before end time")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/resource-assignments - session not found returns 404")
    void createResourceAssignment_whenSessionNotFound_returns404() throws Exception {
        when(resourceAssignmentService.createAssignment(eq(999L), eq(200L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Vaccination session not found with ID: 999"));

        String json = """
                {
                    "resourceId": 200,
                    "startAt": "2026-09-01T08:00:00Z",
                    "endAt": "2026-09-01T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/999/resource-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination session not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/resource-assignments - non-DRAFT session returns 409")
    void createResourceAssignment_whenNonDraftSession_returns409() throws Exception {
        when(resourceAssignmentService.createAssignment(eq(100L), eq(200L), any(), any()))
                .thenThrow(new IllegalStateException("Operational resource assignments can only be configured for DRAFT sessions"));

        String json = """
                {
                    "resourceId": 200,
                    "startAt": "2026-09-01T08:00:00Z",
                    "endAt": "2026-09-01T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/100/resource-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("DRAFT sessions")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/resource-assignments - overlapping resource assignment returns 409")
    void createResourceAssignment_whenOverlapping_returns409() throws Exception {
        when(resourceAssignmentService.createAssignment(eq(100L), eq(200L), any(), any()))
                .thenThrow(new IllegalStateException("Operational resource 200 is already assigned to an overlapping active session window"));

        String json = """
                {
                    "resourceId": 200,
                    "startAt": "2026-09-01T08:00:00Z",
                    "endAt": "2026-09-01T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/100/resource-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("overlapping")));
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{sessionId}/resource-assignments - returns list of assignments")
    void listResourceAssignments_returns200() throws Exception {
        when(resourceAssignmentService.getAssignmentsForSession(eq(100L)))
                .thenReturn(List.of(sampleResourceAssignment));

        mockMvc.perform(get("/api/v1/sessions/100/resource-assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(400)))
                .andExpect(jsonPath("$[0].resourceCode", is("STATION-01")));
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{sessionId}/resource-assignments/{assignmentId} - returns single assignment")
    void getResourceAssignment_returns200() throws Exception {
        when(resourceAssignmentService.getAssignment(eq(100L), eq(400L)))
                .thenReturn(sampleResourceAssignment);

        mockMvc.perform(get("/api/v1/sessions/100/resource-assignments/400"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(400)))
                .andExpect(jsonPath("$.resourceName", is("Vaccination Booth 1")));
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{sessionId}/resource-assignments/{assignmentId} - wrong session ownership returns 404")
    void getResourceAssignment_whenWrongSession_returns404() throws Exception {
        when(resourceAssignmentService.getAssignment(eq(100L), eq(400L)))
                .thenThrow(new ResourceNotFoundException("Resource assignment with ID 400 does not belong to session 100"));

        mockMvc.perform(get("/api/v1/sessions/100/resource-assignments/400"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("does not belong to session 100")));
    }

    @Test
    @DisplayName("DELETE /api/v1/sessions/{sessionId}/resource-assignments/{assignmentId} - DRAFT session returns 204")
    void deleteResourceAssignment_whenDraft_returns204() throws Exception {
        doNothing().when(resourceAssignmentService).deleteAssignment(eq(100L), eq(400L));

        mockMvc.perform(delete("/api/v1/sessions/100/resource-assignments/400"))
                .andExpect(status().isNoContent());

        verify(resourceAssignmentService).deleteAssignment(eq(100L), eq(400L));
    }

    @Test
    @DisplayName("DELETE /api/v1/sessions/{sessionId}/resource-assignments/{assignmentId} - non-DRAFT session returns 409")
    void deleteResourceAssignment_whenNonDraft_returns409() throws Exception {
        doThrow(new IllegalStateException("Operational resource assignments can only be deleted from DRAFT sessions"))
                .when(resourceAssignmentService).deleteAssignment(eq(100L), eq(400L));

        mockMvc.perform(delete("/api/v1/sessions/100/resource-assignments/400"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("DRAFT sessions")));
    }

    // ==========================================
    // STAFF ASSIGNMENT TESTS
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/staff-assignments - valid payload creates assignment and returns 201 with Location header")
    void createStaffAssignment_withValidPayload_returns201() throws Exception {
        when(staffAssignmentService.createAssignment(eq(100L), eq(300L), eq(StaffRole.VACCINATOR), eq(assignmentStart), eq(assignmentEnd)))
                .thenReturn(sampleStaffAssignment);

        String json = """
                {
                    "staffRosterId": 300,
                    "role": "VACCINATOR",
                    "startAt": "2026-09-01T08:00:00Z",
                    "endAt": "2026-09-01T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/100/staff-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/sessions/100/staff-assignments/500"))
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.sessionId", is(100)))
                .andExpect(jsonPath("$.staffRosterId", is(300)))
                .andExpect(jsonPath("$.userId", is(501)))
                .andExpect(jsonPath("$.role", is("VACCINATOR")))
                .andExpect(jsonPath("$.startAt", is("2026-09-01T08:00:00Z")))
                .andExpect(jsonPath("$.endAt", is("2026-09-01T12:00:00Z")))
                .andExpect(jsonPath("$.createdAt", is(notNullValue())));

        verify(staffAssignmentService).createAssignment(eq(100L), eq(300L), eq(StaffRole.VACCINATOR), eq(assignmentStart), eq(assignmentEnd));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/staff-assignments - missing required fields returns 400")
    void createStaffAssignment_withMissingFields_returns400() throws Exception {
        String json = "{}";

        mockMvc.perform(post("/api/v1/sessions/100/staff-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors", hasSize(4)));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/staff-assignments - role mismatch returns 409")
    void createStaffAssignment_whenRoleMismatch_returns409() throws Exception {
        when(staffAssignmentService.createAssignment(eq(100L), eq(300L), eq(StaffRole.SUPERVISOR), any(), any()))
                .thenThrow(new IllegalStateException("Requested assignment role SUPERVISOR does not match roster qualification VACCINATOR"));

        String json = """
                {
                    "staffRosterId": 300,
                    "role": "SUPERVISOR",
                    "startAt": "2026-09-01T08:00:00Z",
                    "endAt": "2026-09-01T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/100/staff-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("does not match roster qualification")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/staff-assignments - overlapping staff user returns 409")
    void createStaffAssignment_whenOverlapping_returns409() throws Exception {
        when(staffAssignmentService.createAssignment(eq(100L), eq(300L), eq(StaffRole.VACCINATOR), any(), any()))
                .thenThrow(new IllegalStateException("Staff member with user ID 501 is already assigned to an overlapping active session window"));

        String json = """
                {
                    "staffRosterId": 300,
                    "role": "VACCINATOR",
                    "startAt": "2026-09-01T08:00:00Z",
                    "endAt": "2026-09-01T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/100/staff-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already assigned to an overlapping active session window")));
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{sessionId}/staff-assignments - returns list of assignments")
    void listStaffAssignments_returns200() throws Exception {
        when(staffAssignmentService.getAssignmentsForSession(eq(100L)))
                .thenReturn(List.of(sampleStaffAssignment));

        mockMvc.perform(get("/api/v1/sessions/100/staff-assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(500)))
                .andExpect(jsonPath("$[0].userId", is(501)))
                .andExpect(jsonPath("$[0].role", is("VACCINATOR")));
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{sessionId}/staff-assignments/{assignmentId} - returns single assignment")
    void getStaffAssignment_returns200() throws Exception {
        when(staffAssignmentService.getAssignment(eq(100L), eq(500L)))
                .thenReturn(sampleStaffAssignment);

        mockMvc.perform(get("/api/v1/sessions/100/staff-assignments/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.role", is("VACCINATOR")));
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{sessionId}/staff-assignments/{assignmentId} - wrong session ownership returns 404")
    void getStaffAssignment_whenWrongSession_returns404() throws Exception {
        when(staffAssignmentService.getAssignment(eq(100L), eq(500L)))
                .thenThrow(new ResourceNotFoundException("Staff assignment with ID 500 does not belong to session 100"));

        mockMvc.perform(get("/api/v1/sessions/100/staff-assignments/500"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("does not belong to session 100")));
    }

    @Test
    @DisplayName("DELETE /api/v1/sessions/{sessionId}/staff-assignments/{assignmentId} - DRAFT session returns 204")
    void deleteStaffAssignment_whenDraft_returns204() throws Exception {
        doNothing().when(staffAssignmentService).deleteAssignment(eq(100L), eq(500L));

        mockMvc.perform(delete("/api/v1/sessions/100/staff-assignments/500"))
                .andExpect(status().isNoContent());

        verify(staffAssignmentService).deleteAssignment(eq(100L), eq(500L));
    }

    @Test
    @DisplayName("DELETE /api/v1/sessions/{sessionId}/staff-assignments/{assignmentId} - non-DRAFT session returns 409")
    void deleteStaffAssignment_whenNonDraft_returns409() throws Exception {
        doThrow(new IllegalStateException("Staff assignments can only be deleted from DRAFT sessions"))
                .when(staffAssignmentService).deleteAssignment(eq(100L), eq(500L));

        mockMvc.perform(delete("/api/v1/sessions/100/staff-assignments/500"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("DRAFT sessions")));
    }
}
