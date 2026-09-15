package com.se2030.vaccinationportal.campaign.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.service.SessionBookingStatusService;
import com.se2030.vaccinationportal.campaign.service.SessionOperationalReadinessService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;

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
class SessionControllerTests {

    private MockMvc mockMvc;

    @Mock
    private VaccinationSessionService sessionService;

    @Mock
    private SessionOperationalReadinessService readinessService;

    @Mock
    private SessionBookingStatusService bookingStatusService;

    private VaccinationCampaign sampleCampaign;
    private VaccinationCenter sampleCenter;
    private VaccinationSession sampleSession;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new SessionController(sessionService, readinessService, bookingStatusService))
                .setControllerAdvice(new GlobalCampaignExceptionHandler())
                .setValidator(validator)
                .build();


        sampleCampaign = new VaccinationCampaign(
                "CMP-2026-AUTUMN",
                "Autumn 2026 Campaign",
                "Community booster drive",
                null,
                null,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 30),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-11-29T18:00:00Z")
        );
        ReflectionTestUtils.setField(sampleCampaign, "id", 100L);

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
        ReflectionTestUtils.setField(sampleCenter, "id", 200L);

        sampleSession = new VaccinationSession(
                sampleCampaign,
                sampleCenter,
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(sampleSession, "id", 500L);
        ReflectionTestUtils.setField(sampleSession, "createdAt", Instant.parse("2026-08-01T10:00:00Z"));
        ReflectionTestUtils.setField(sampleSession, "updatedAt", Instant.parse("2026-08-01T10:00:00Z"));
    }

    // ==========================================
    // 1. POST /api/v1/sessions (Create One-Off)
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/sessions creates draft session and returns 201 with Location header")
    void createSessionSuccess() throws Exception {
        String jsonPayload = """
                {
                    "campaignId": 100,
                    "centerId": 200,
                    "startAt": "2026-10-15T09:00:00Z",
                    "endAt": "2026-10-15T12:00:00Z",
                    "bookingCloseAt": "2026-10-15T08:00:00Z"
                }
                """;

        when(sessionService.createOneOffDraftSession(
                eq(100L),
                eq(200L),
                eq(Instant.parse("2026-10-15T09:00:00Z")),
                eq(Instant.parse("2026-10-15T12:00:00Z")),
                eq(Instant.parse("2026-10-15T08:00:00Z"))
        )).thenReturn(sampleSession);

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/sessions/500"))
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.campaignId", is(100)))
                .andExpect(jsonPath("$.centerId", is(200)))
                .andExpect(jsonPath("$.startAt", is("2026-10-15T09:00:00Z")))
                .andExpect(jsonPath("$.endAt", is("2026-10-15T12:00:00Z")))
                .andExpect(jsonPath("$.bookingCloseAt", is("2026-10-15T08:00:00Z")))
                .andExpect(jsonPath("$.scheduleTimezone", is("Asia/Colombo")))
                .andExpect(jsonPath("$.lifecycleStatus", is("DRAFT")))
                .andExpect(jsonPath("$.generated", is(false)))
                .andExpect(jsonPath("$.createdAt", is("2026-08-01T10:00:00Z")))
                .andExpect(jsonPath("$.updatedAt", is("2026-08-01T10:00:00Z")));

        verify(sessionService).createOneOffDraftSession(
                eq(100L),
                eq(200L),
                eq(Instant.parse("2026-10-15T09:00:00Z")),
                eq(Instant.parse("2026-10-15T12:00:00Z")),
                eq(Instant.parse("2026-10-15T08:00:00Z"))
        );
    }

    @Test
    @DisplayName("POST /api/v1/sessions with missing required fields returns 400 Bad Request")
    void createSessionValidationFailure() throws Exception {
        String invalidPayload = """
                {
                    "campaignId": null,
                    "centerId": null
                }
                """;

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/sessions with nonexistent campaign returns 404 Not Found")
    void createSessionMissingCampaignNotFound() throws Exception {
        String jsonPayload = """
                {
                    "campaignId": 999,
                    "centerId": 200,
                    "startAt": "2026-10-15T09:00:00Z",
                    "endAt": "2026-10-15T12:00:00Z",
                    "bookingCloseAt": "2026-10-15T08:00:00Z"
                }
                """;

        when(sessionService.createOneOffDraftSession(
                eq(999L), eq(200L), any(Instant.class), any(Instant.class), any(Instant.class)
        )).thenThrow(new ResourceNotFoundException("Campaign not found with ID: 999"));

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Campaign not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions with nonexistent center returns 404 Not Found")
    void createSessionMissingCenterNotFound() throws Exception {
        String jsonPayload = """
                {
                    "campaignId": 100,
                    "centerId": 999,
                    "startAt": "2026-10-15T09:00:00Z",
                    "endAt": "2026-10-15T12:00:00Z",
                    "bookingCloseAt": "2026-10-15T08:00:00Z"
                }
                """;

        when(sessionService.createOneOffDraftSession(
                eq(100L), eq(999L), any(Instant.class), any(Instant.class), any(Instant.class)
        )).thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination center not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions with domain invariant rejection returns 400 Bad Request")
    void createSessionDomainInvariantRejection() throws Exception {
        String jsonPayload = """
                {
                    "campaignId": 100,
                    "centerId": 200,
                    "startAt": "2026-10-15T12:00:00Z",
                    "endAt": "2026-10-15T09:00:00Z",
                    "bookingCloseAt": "2026-10-15T08:00:00Z"
                }
                """;

        when(sessionService.createOneOffDraftSession(
                eq(100L), eq(200L), any(Instant.class), any(Instant.class), any(Instant.class)
        )).thenThrow(new IllegalArgumentException("Session start must be before session end"));

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Session start must be before session end")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions with malformed JSON body returns 400 Bad Request")
    void createSessionMalformedJson() throws Exception {
        String malformedJson = "{ campaignId: 100, invalid }";

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Malformed JSON request body")));
    }

    // ==========================================
    // 2. GET /api/v1/sessions/{id}
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/sessions/{id} returns 200 OK with session details")
    void getSessionByIdSuccess() throws Exception {
        when(sessionService.getRequiredSessionById(500L)).thenReturn(sampleSession);

        mockMvc.perform(get("/api/v1/sessions/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.campaignId", is(100)))
                .andExpect(jsonPath("$.centerId", is(200)))
                .andExpect(jsonPath("$.startAt", is("2026-10-15T09:00:00Z")))
                .andExpect(jsonPath("$.endAt", is("2026-10-15T12:00:00Z")))
                .andExpect(jsonPath("$.bookingCloseAt", is("2026-10-15T08:00:00Z")))
                .andExpect(jsonPath("$.scheduleTimezone", is("Asia/Colombo")))
                .andExpect(jsonPath("$.lifecycleStatus", is("DRAFT")))
                .andExpect(jsonPath("$.generated", is(false)));

        verify(sessionService).getRequiredSessionById(500L);
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id} when session does not exist returns 404 Not Found")
    void getSessionByIdNotFound() throws Exception {
        when(sessionService.getRequiredSessionById(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination session not found with ID: 999"));

        mockMvc.perform(get("/api/v1/sessions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination session not found with ID: 999")));
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id} with non-numeric ID returns 400 Bad Request")
    void getSessionByIdTypeMismatch() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid parameter value: invalid-id")));
    }

    // ==========================================
    // 3. GET /api/v1/sessions (List with Filters)
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/sessions with no filters returns 200 OK and list of summaries")
    void getSessionsNoFilters() throws Exception {
        when(sessionService.getSessions(null, null, null)).thenReturn(List.of(sampleSession));

        mockMvc.perform(get("/api/v1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(500)))
                .andExpect(jsonPath("$[0].campaignId", is(100)))
                .andExpect(jsonPath("$[0].centerId", is(200)))
                .andExpect(jsonPath("$[0].startAt", is("2026-10-15T09:00:00Z")))
                .andExpect(jsonPath("$[0].endAt", is("2026-10-15T12:00:00Z")))
                .andExpect(jsonPath("$[0].bookingCloseAt", is("2026-10-15T08:00:00Z")))
                .andExpect(jsonPath("$[0].scheduleTimezone", is("Asia/Colombo")))
                .andExpect(jsonPath("$[0].lifecycleStatus", is("DRAFT")))
                .andExpect(jsonPath("$[0].generated", is(false)));

        verify(sessionService).getSessions(null, null, null);
    }

    @Test
    @DisplayName("GET /api/v1/sessions with campaignId, centerId, and status filters passes parameters correctly")
    void getSessionsWithCombinedFilters() throws Exception {
        when(sessionService.getSessions(100L, 200L, SessionLifecycleStatus.DRAFT))
                .thenReturn(List.of(sampleSession));

        mockMvc.perform(get("/api/v1/sessions")
                        .param("campaignId", "100")
                        .param("centerId", "200")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(500)));

        verify(sessionService).getSessions(100L, 200L, SessionLifecycleStatus.DRAFT);
    }

    @Test
    @DisplayName("GET /api/v1/sessions when no sessions match returns 200 OK with empty array")
    void getSessionsEmptyResult() throws Exception {
        when(sessionService.getSessions(999L, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/sessions").param("campaignId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(sessionService).getSessions(999L, null, null);
    }

    @Test
    @DisplayName("GET /api/v1/sessions with invalid status enum returns 400 Bad Request")
    void getSessionsInvalidStatusEnum() throws Exception {
        mockMvc.perform(get("/api/v1/sessions").param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid parameter value: INVALID_STATUS")));
    }

    // ==========================================
    // 4. POST /api/v1/sessions/{id}/schedule
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/schedule transitions to SCHEDULED and returns 200 OK")
    void scheduleSessionSuccess() throws Exception {
        VaccinationSession scheduledSession = sampleSession;
        ReflectionTestUtils.setField(scheduledSession, "lifecycleStatus", SessionLifecycleStatus.SCHEDULED);
        when(sessionService.scheduleSession(500L)).thenReturn(scheduledSession);

        mockMvc.perform(post("/api/v1/sessions/500/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.lifecycleStatus", is("SCHEDULED")));

        verify(sessionService).scheduleSession(500L);
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/schedule returns 409 on operational readiness failure")
    void scheduleSessionOperationalReadinessFailure() throws Exception {
        String message = "No approved capacity plan exists for session 500";
        when(sessionService.scheduleSession(500L)).thenThrow(new IllegalStateException(message));

        mockMvc.perform(post("/api/v1/sessions/500/schedule"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is(message)))
                .andExpect(jsonPath("$.path", is("/api/v1/sessions/500/schedule")));

        verify(sessionService).scheduleSession(500L);
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/schedule for nonexistent session returns 404 Not Found")
    void scheduleSessionNotFound() throws Exception {
        when(sessionService.scheduleSession(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination session not found with ID: 999"));

        mockMvc.perform(post("/api/v1/sessions/999/schedule"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination session not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/schedule when already in target lifecycle returns 409 Conflict")
    void scheduleSessionConflict() throws Exception {
        when(sessionService.scheduleSession(500L))
                .thenThrow(new IllegalStateException("Session is already in SCHEDULED lifecycle status"));

        mockMvc.perform(post("/api/v1/sessions/500/schedule"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Session is already in SCHEDULED lifecycle status")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/schedule when campaign is suspended returns 400 Bad Request")
    void scheduleSessionSuspendedCampaign() throws Exception {
        when(sessionService.scheduleSession(500L))
                .thenThrow(new IllegalArgumentException("Cannot schedule session for suspended campaign with ID: 100"));

        mockMvc.perform(post("/api/v1/sessions/500/schedule"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Cannot schedule session for suspended campaign")));
    }

    // ==========================================
    // 5. POST /api/v1/sessions/{id}/start
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/start starts SCHEDULED session and returns 200 OK")
    void startSessionSuccess() throws Exception {
        VaccinationSession inProgressSession = new VaccinationSession(
                sampleCampaign,
                sampleCenter,
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(inProgressSession, "id", 500L);
        ReflectionTestUtils.setField(inProgressSession, "lifecycleStatus", SessionLifecycleStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(inProgressSession, "createdAt", Instant.parse("2026-08-01T10:00:00Z"));
        ReflectionTestUtils.setField(inProgressSession, "updatedAt", Instant.parse("2026-08-01T10:10:00Z"));

        when(sessionService.startSession(500L)).thenReturn(inProgressSession);

        mockMvc.perform(post("/api/v1/sessions/500/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.lifecycleStatus", is("IN_PROGRESS")));

        verify(sessionService).startSession(500L);
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/start for nonexistent session returns 404 Not Found")
    void startSessionNotFound() throws Exception {
        when(sessionService.startSession(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination session not found with ID: 999"));

        mockMvc.perform(post("/api/v1/sessions/999/start"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination session not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/start with invalid lifecycle status returns 409 Conflict")
    void startSessionInvalidLifecycleConflict() throws Exception {
        when(sessionService.startSession(500L))
                .thenThrow(new IllegalStateException("Invalid session lifecycle transition from DRAFT to IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/sessions/500/start"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Invalid session lifecycle transition from DRAFT to IN_PROGRESS")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/start with forbidden parent campaign status returns 409 Conflict")
    void startSessionCampaignLifecycleConflict() throws Exception {
        when(sessionService.startSession(500L))
                .thenThrow(new IllegalStateException("Cannot start vaccination session for campaign with ID 10 in SUSPENDED status"));

        mockMvc.perform(post("/api/v1/sessions/500/start"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Cannot start vaccination session for campaign with ID 10 in SUSPENDED status")));
    }

    // ==========================================
    // 6. POST /api/v1/sessions/{id}/complete
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/complete completes IN_PROGRESS session and returns 200 OK")
    void completeSessionSuccess() throws Exception {
        VaccinationSession completedSession = new VaccinationSession(
                sampleCampaign,
                sampleCenter,
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(completedSession, "id", 500L);
        ReflectionTestUtils.setField(completedSession, "lifecycleStatus", SessionLifecycleStatus.COMPLETED);
        ReflectionTestUtils.setField(completedSession, "createdAt", Instant.parse("2026-08-01T10:00:00Z"));
        ReflectionTestUtils.setField(completedSession, "updatedAt", Instant.parse("2026-08-01T10:15:00Z"));

        when(sessionService.completeSession(500L)).thenReturn(completedSession);

        mockMvc.perform(post("/api/v1/sessions/500/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.lifecycleStatus", is("COMPLETED")));

        verify(sessionService).completeSession(500L);
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/complete for nonexistent session returns 404 Not Found")
    void completeSessionNotFound() throws Exception {
        when(sessionService.completeSession(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination session not found with ID: 999"));

        mockMvc.perform(post("/api/v1/sessions/999/complete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination session not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/complete with invalid lifecycle status returns 409 Conflict")
    void completeSessionInvalidLifecycleConflict() throws Exception {
        when(sessionService.completeSession(500L))
                .thenThrow(new IllegalStateException("Invalid session lifecycle transition from SCHEDULED to COMPLETED"));

        mockMvc.perform(post("/api/v1/sessions/500/complete"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Invalid session lifecycle transition from SCHEDULED to COMPLETED")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/complete with forbidden parent campaign status returns 409 Conflict")
    void completeSessionCampaignLifecycleConflict() throws Exception {
        when(sessionService.completeSession(500L))
                .thenThrow(new IllegalStateException("Cannot complete vaccination session for campaign with ID 10 in COMPLETED status"));

        mockMvc.perform(post("/api/v1/sessions/500/complete"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Cannot complete vaccination session for campaign with ID 10 in COMPLETED status")));
    }

    // ==========================================
    // 7. GET /api/v1/sessions/{id}/readiness
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/readiness for fully ready DRAFT session returns 200 OK with ready=true and empty violations")
    void getSessionReadinessReadySuccess() throws Exception {
        SessionOperationalReadinessService.OperationalReadinessResult result =
                new SessionOperationalReadinessService.OperationalReadinessResult(
                        500L,
                        true,
                        List.of(),
                        2,
                        2,
                        2,
                        36,
                        30,
                        1
                );

        when(readinessService.evaluateReadiness(500L)).thenReturn(result);

        mockMvc.perform(get("/api/v1/sessions/500/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(500)))
                .andExpect(jsonPath("$.ready", is(true)))
                .andExpect(jsonPath("$.violations", hasSize(0)))
                .andExpect(jsonPath("$.assignedVaccinators", is(2)))
                .andExpect(jsonPath("$.availableStations", is(2)))
                .andExpect(jsonPath("$.effectiveStations", is(2)))
                .andExpect(jsonPath("$.recommendedThroughput", is(36)))
                .andExpect(jsonPath("$.approvedCapacity", is(30)))
                .andExpect(jsonPath("$.revision", is(1)));

        verify(readinessService).evaluateReadiness(500L);
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/readiness for non-ready DRAFT session returns 200 OK (NOT 409) with ready=false and violations")
    void getSessionReadinessNonReadyDraftSuccess() throws Exception {
        SessionOperationalReadinessService.OperationalReadinessResult result =
                new SessionOperationalReadinessService.OperationalReadinessResult(
                        500L,
                        false,
                        List.of(
                                "No active vaccination stations assigned",
                                "No active vaccinators assigned",
                                "No approved capacity plan exists"
                        ),
                        0,
                        0,
                        0,
                        0,
                        null,
                        null
                );

        when(readinessService.evaluateReadiness(500L)).thenReturn(result);

        mockMvc.perform(get("/api/v1/sessions/500/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(500)))
                .andExpect(jsonPath("$.ready", is(false)))
                .andExpect(jsonPath("$.violations", hasSize(3)))
                .andExpect(jsonPath("$.violations[0]", is("No active vaccination stations assigned")))
                .andExpect(jsonPath("$.violations[1]", is("No active vaccinators assigned")))
                .andExpect(jsonPath("$.violations[2]", is("No approved capacity plan exists")))
                .andExpect(jsonPath("$.assignedVaccinators", is(0)))
                .andExpect(jsonPath("$.availableStations", is(0)))
                .andExpect(jsonPath("$.effectiveStations", is(0)))
                .andExpect(jsonPath("$.recommendedThroughput", is(0)))
                .andExpect(jsonPath("$.approvedCapacity").doesNotExist())
                .andExpect(jsonPath("$.revision").doesNotExist());

        verify(readinessService).evaluateReadiness(500L);
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/readiness for non-DRAFT session returns 200 OK diagnostic response according to evaluator")
    void getSessionReadinessNonDraftDiagnosticSuccess() throws Exception {
        SessionOperationalReadinessService.OperationalReadinessResult result =
                new SessionOperationalReadinessService.OperationalReadinessResult(
                        500L,
                        false,
                        List.of("Session is not in DRAFT status (current: SCHEDULED)"),
                        1,
                        1,
                        1,
                        18,
                        18,
                        1
                );

        when(readinessService.evaluateReadiness(500L)).thenReturn(result);

        mockMvc.perform(get("/api/v1/sessions/500/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(500)))
                .andExpect(jsonPath("$.ready", is(false)))
                .andExpect(jsonPath("$.violations", hasSize(1)))
                .andExpect(jsonPath("$.violations[0]", is("Session is not in DRAFT status (current: SCHEDULED)")))
                .andExpect(jsonPath("$.assignedVaccinators", is(1)))
                .andExpect(jsonPath("$.availableStations", is(1)))
                .andExpect(jsonPath("$.effectiveStations", is(1)))
                .andExpect(jsonPath("$.recommendedThroughput", is(18)))
                .andExpect(jsonPath("$.approvedCapacity", is(18)))
                .andExpect(jsonPath("$.revision", is(1)));

        verify(readinessService).evaluateReadiness(500L);
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/readiness for unknown session returns 404 Not Found")
    void getSessionReadinessNotFound() throws Exception {
        when(readinessService.evaluateReadiness(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination session not found with ID: 999"));

        mockMvc.perform(get("/api/v1/sessions/999/readiness"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination session not found with ID: 999")));

        verify(readinessService).evaluateReadiness(999L);
    }
}
