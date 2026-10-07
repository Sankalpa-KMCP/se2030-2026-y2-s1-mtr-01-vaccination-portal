package com.se2030.vaccinationportal.campaign.session.planning.web;

import com.se2030.vaccinationportal.campaign.web.GlobalCampaignExceptionHandler;


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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.service.SessionChangePreview;
import com.se2030.vaccinationportal.campaign.session.planning.service.SessionBookingStatusService;
import com.se2030.vaccinationportal.campaign.session.planning.service.SessionOperationalReadinessService;
import com.se2030.vaccinationportal.campaign.session.planning.service.VaccinationSessionService;

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
class SessionControllerChangeTests {

    private MockMvc mockMvc;

    @Mock
    private VaccinationSessionService sessionService;

    @Mock
    private SessionOperationalReadinessService readinessService;

    @Mock
    private SessionBookingStatusService bookingStatusService;

    private VaccinationCampaign sampleCampaign;
    private VaccinationCenter sampleCenter;
    private VaccinationCenter targetCenter;
    private VaccinationSession sampleSession;

    private SessionScheduleSnapshot oldSnapshot;
    private SessionScheduleSnapshot newRescheduleSnapshot;
    private SessionScheduleSnapshot newTransferSnapshot;
    private SessionScheduleSnapshot newCancelledSnapshot;

    private SessionChangePreview reschedulePreview;
    private SessionChangePreview transferPreview;
    private SessionChangePreview cancelPreview;

    private org.springframework.security.core.Authentication sampleAuth;
    private com.se2030.vaccinationportal.shared.auth.security.UserPrincipal samplePrincipal;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        samplePrincipal = com.se2030.vaccinationportal.shared.auth.security.UserPrincipal.fromClaims(
                42L,
                "officer-01@vaccination.gov",
                List.of("ROLE_CENTER_MANAGER")
        );
        sampleAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                samplePrincipal,
                null,
                samplePrincipal.getAuthorities()
        );

        mockMvc = MockMvcBuilders.standaloneSetup(new SessionController(sessionService, readinessService, bookingStatusService))
                .setCustomArgumentResolvers(new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {

                        return parameter.hasParameterAnnotation(org.springframework.security.core.annotation.AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(
                            org.springframework.core.MethodParameter parameter,
                            org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                            org.springframework.web.context.request.NativeWebRequest webRequest,
                            org.springframework.web.bind.support.WebDataBinderFactory binderFactory
                    ) {
                        java.security.Principal principal = webRequest.getUserPrincipal();
                        if (principal instanceof org.springframework.security.core.Authentication auth) {
                            if (auth.getPrincipal() instanceof com.se2030.vaccinationportal.shared.auth.security.UserPrincipal userPrincipal) {
                                return userPrincipal;
                            }
                        }
                        return null;
                    }
                })
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

        targetCenter = new VaccinationCenter(
                "CTR-KANDY-01",
                "Kandy Regional Center",
                "456 Hill Street, Kandy",
                "+94 81 2345678",
                CenterStatus.ACTIVE,
                100,
                30,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(targetCenter, "id", 201L);

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

        oldSnapshot = new SessionScheduleSnapshot(
                200L,
                "Asia/Colombo",
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                SessionLifecycleStatus.DRAFT
        );

        newRescheduleSnapshot = new SessionScheduleSnapshot(
                200L,
                "Asia/Colombo",
                Instant.parse("2026-10-16T10:00:00Z"),
                Instant.parse("2026-10-16T13:00:00Z"),
                Instant.parse("2026-10-16T09:00:00Z"),
                SessionLifecycleStatus.DRAFT
        );

        newTransferSnapshot = new SessionScheduleSnapshot(
                201L,
                "Asia/Colombo",
                Instant.parse("2026-10-16T10:00:00Z"),
                Instant.parse("2026-10-16T13:00:00Z"),
                Instant.parse("2026-10-16T09:00:00Z"),
                SessionLifecycleStatus.DRAFT
        );

        newCancelledSnapshot = new SessionScheduleSnapshot(
                200L,
                "Asia/Colombo",
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                SessionLifecycleStatus.CANCELLED
        );

        reschedulePreview = new SessionChangePreview(
                500L,
                SessionChangeType.RESCHEDULED,
                oldSnapshot,
                newRescheduleSnapshot,
                0,
                0,
                "reschedule-sha256-token-123456"
        );

        transferPreview = new SessionChangePreview(
                500L,
                SessionChangeType.CENTER_TRANSFERRED,
                oldSnapshot,
                newTransferSnapshot,
                0,
                0,
                "transfer-sha256-token-123456"
        );

        cancelPreview = new SessionChangePreview(
                500L,
                SessionChangeType.CANCELLED,
                oldSnapshot,
                newCancelledSnapshot,
                0,
                0,
                "cancellation-sha256-token-123456"
        );
    }

    // =========================================================================
    // 1. POST /api/v1/sessions/{id}/cancellation-preview
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancellation-preview returns 200 OK with preview payload")
    void previewCancellationSuccess() throws Exception {
        when(sessionService.previewCancellation(500L)).thenReturn(cancelPreview);

        mockMvc.perform(post("/api/v1/sessions/500/cancellation-preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(500)))
                .andExpect(jsonPath("$.changeType", is("CANCELLED")))
                .andExpect(jsonPath("$.oldSnapshot.centerId", is(200)))
                .andExpect(jsonPath("$.oldSnapshot.lifecycleStatus", is("DRAFT")))
                .andExpect(jsonPath("$.newSnapshot.centerId", is(200)))
                .andExpect(jsonPath("$.newSnapshot.lifecycleStatus", is("CANCELLED")))
                .andExpect(jsonPath("$.affectedAppointmentCount", is(0)))
                .andExpect(jsonPath("$.affectedWaitlistCount", is(0)))
                .andExpect(jsonPath("$.previewToken", is("cancellation-sha256-token-123456")));

        verify(sessionService).previewCancellation(500L);
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancellation-preview when session missing returns 404 Not Found")
    void previewCancellationSessionNotFound() throws Exception {
        when(sessionService.previewCancellation(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination session not found with ID: 999"));

        mockMvc.perform(post("/api/v1/sessions/999/cancellation-preview"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination session not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancellation-preview when session is already cancelled returns 400 Bad Request")
    void previewCancellationAlreadyCancelled() throws Exception {
        when(sessionService.previewCancellation(500L))
                .thenThrow(new IllegalArgumentException("Session is already cancelled"));

        mockMvc.perform(post("/api/v1/sessions/500/cancellation-preview"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Session is already cancelled")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancellation-preview when impact reader absent for SCHEDULED session returns 409 Conflict")
    void previewCancellationMissingImpactReaderScheduled() throws Exception {
        when(sessionService.previewCancellation(500L))
                .thenThrow(new IllegalStateException("Session impact reader is not available to assess SCHEDULED session changes"));

        mockMvc.perform(post("/api/v1/sessions/500/cancellation-preview"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Session impact reader is not available")));
    }

    // =========================================================================
    // 2. POST /api/v1/sessions/{id}/cancel
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancel confirms cancellation with trusted principal actorId and returns 200 OK")
    void confirmCancellationSuccess() throws Exception {
        VaccinationSession cancelledSession = new VaccinationSession(
                sampleCampaign,
                sampleCenter,
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(cancelledSession, "id", 500L);
        ReflectionTestUtils.setField(cancelledSession, "lifecycleStatus", SessionLifecycleStatus.CANCELLED);

        when(sessionService.confirmCancellation(
                eq(500L),
                eq("cancellation-sha256-token-123456"),
                eq("Severe weather warning"),
                eq("42")
        )).thenReturn(cancelledSession);

        String payload = """
                {
                    "previewToken": "cancellation-sha256-token-123456",
                    "reason": "Severe weather warning"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/cancel")
                        .principal(sampleAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.lifecycleStatus", is("CANCELLED")));

        verify(sessionService).confirmCancellation(500L, "cancellation-sha256-token-123456", "Severe weather warning", "42");
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancel anonymously returns 401 Unauthorized")
    void confirmCancellationAnonymousReturns401() throws Exception {
        String payload = """
                {
                    "previewToken": "cancellation-sha256-token-123456",
                    "reason": "Severe weather warning"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancel ignores client-supplied spoofed actorId and uses trusted principal userId")
    void confirmCancellationSpoofedActorIgnored() throws Exception {
        VaccinationSession cancelledSession = new VaccinationSession(
                sampleCampaign,
                sampleCenter,
                Instant.parse("2026-10-15T09:00:00Z"),
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T08:00:00Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(cancelledSession, "id", 500L);
        ReflectionTestUtils.setField(cancelledSession, "lifecycleStatus", SessionLifecycleStatus.CANCELLED);

        when(sessionService.confirmCancellation(
                eq(500L),
                eq("cancellation-sha256-token-123456"),
                eq("Severe weather warning"),
                eq("42")
        )).thenReturn(cancelledSession);

        String payload = """
                {
                    "previewToken": "cancellation-sha256-token-123456",
                    "reason": "Severe weather warning",
                    "actorId": "spoofed-attacker-999"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/cancel")
                        .principal(sampleAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)));

        verify(sessionService).confirmCancellation(500L, "cancellation-sha256-token-123456", "Severe weather warning", "42");
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancel with blank token or reason returns 400 Bad Request")
    void confirmCancellationValidationFailure() throws Exception {
        String invalidPayload = """
                {
                    "previewToken": "",
                    "reason": ""
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/cancel")
                        .principal(sampleAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancel with reason > 500 chars returns 400 Bad Request")
    void confirmCancellationReasonTooLong() throws Exception {
        String longReason = "A".repeat(501);
        String invalidPayload = String.format("""
                {
                    "previewToken": "token-123",
                    "reason": "%s"
                }
                """, longReason);

        mockMvc.perform(post("/api/v1/sessions/500/cancel")
                        .principal(sampleAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("reason")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancel with stale token returns 409 Conflict")
    void confirmCancellationStaleToken() throws Exception {
        when(sessionService.confirmCancellation(
                eq(500L), any(), any(), any()
        )).thenThrow(new IllegalStateException("Preview token is stale or invalid. Re-preview the session change before confirming."));

        String payload = """
                {
                    "previewToken": "stale-token",
                    "reason": "Valid reason"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/cancel")
                        .principal(sampleAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Preview token is stale or invalid")));
    }

    // =========================================================================
    // 3. POST /api/v1/sessions/{id}/reschedule-preview
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/reschedule-preview returns 200 OK with preview payload")
    void previewRescheduleSuccess() throws Exception {
        when(sessionService.previewReschedule(
                eq(500L),
                eq(Instant.parse("2026-10-16T10:00:00Z")),
                eq(Instant.parse("2026-10-16T13:00:00Z")),
                eq(Instant.parse("2026-10-16T09:00:00Z"))
        )).thenReturn(reschedulePreview);

        String payload = """
                {
                    "newStartAt": "2026-10-16T10:00:00Z",
                    "newEndAt": "2026-10-16T13:00:00Z",
                    "newBookingCloseAt": "2026-10-16T09:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/reschedule-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(500)))
                .andExpect(jsonPath("$.changeType", is("RESCHEDULED")))
                .andExpect(jsonPath("$.oldSnapshot.centerId", is(200)))
                .andExpect(jsonPath("$.newSnapshot.centerId", is(200)))
                .andExpect(jsonPath("$.newSnapshot.startAt", is("2026-10-16T10:00:00Z")))
                .andExpect(jsonPath("$.previewToken", is("reschedule-sha256-token-123456")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/reschedule-preview with missing startAt returns 400 Bad Request")
    void previewRescheduleValidationFailure() throws Exception {
        String invalidPayload = """
                {
                    "newEndAt": "2026-10-16T13:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/reschedule-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/reschedule-preview with no-op schedule returns 400 Bad Request")
    void previewRescheduleNoOp() throws Exception {
        when(sessionService.previewReschedule(eq(500L), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Proposed session change makes no material change to center or schedule"));

        String payload = """
                {
                    "newStartAt": "2026-10-15T09:00:00Z",
                    "newEndAt": "2026-10-15T12:00:00Z",
                    "newBookingCloseAt": "2026-10-15T08:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/reschedule-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Proposed session change makes no material change")));
    }

    // =========================================================================
    // 4. POST /api/v1/sessions/{id}/reschedule
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/reschedule confirms reschedule with trusted principal actorId and returns 200 OK")
    void confirmRescheduleSuccess() throws Exception {
        VaccinationSession rescheduledSession = new VaccinationSession(
                sampleCampaign,
                sampleCenter,
                Instant.parse("2026-10-16T10:00:00Z"),
                Instant.parse("2026-10-16T13:00:00Z"),
                Instant.parse("2026-10-16T09:00:00Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(rescheduledSession, "id", 500L);

        when(sessionService.confirmReschedule(
                eq(500L),
                eq(Instant.parse("2026-10-16T10:00:00Z")),
                eq(Instant.parse("2026-10-16T13:00:00Z")),
                eq(Instant.parse("2026-10-16T09:00:00Z")),
                eq("reschedule-sha256-token-123456"),
                eq("Rescheduling for facility maintenance"),
                eq("42")
        )).thenReturn(rescheduledSession);

        String payload = """
                {
                    "newStartAt": "2026-10-16T10:00:00Z",
                    "newEndAt": "2026-10-16T13:00:00Z",
                    "newBookingCloseAt": "2026-10-16T09:00:00Z",
                    "previewToken": "reschedule-sha256-token-123456",
                    "reason": "Rescheduling for facility maintenance"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/reschedule")
                        .principal(sampleAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.startAt", is("2026-10-16T10:00:00Z")))
                .andExpect(jsonPath("$.endAt", is("2026-10-16T13:00:00Z")));

        verify(sessionService).confirmReschedule(
                eq(500L),
                eq(Instant.parse("2026-10-16T10:00:00Z")),
                eq(Instant.parse("2026-10-16T13:00:00Z")),
                eq(Instant.parse("2026-10-16T09:00:00Z")),
                eq("reschedule-sha256-token-123456"),
                eq("Rescheduling for facility maintenance"),
                eq("42")
        );
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/reschedule anonymously returns 401 Unauthorized")
    void confirmRescheduleAnonymousReturns401() throws Exception {
        String payload = """
                {
                    "newStartAt": "2026-10-16T10:00:00Z",
                    "newEndAt": "2026-10-16T13:00:00Z",
                    "previewToken": "reschedule-sha256-token-123456",
                    "reason": "Rescheduling for facility maintenance"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/reschedule ignores client-supplied spoofed actorId and uses trusted principal userId")
    void confirmRescheduleSpoofedActorIgnored() throws Exception {
        VaccinationSession rescheduledSession = new VaccinationSession(
                sampleCampaign,
                sampleCenter,
                Instant.parse("2026-10-16T10:00:00Z"),
                Instant.parse("2026-10-16T13:00:00Z"),
                Instant.parse("2026-10-16T09:00:00Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(rescheduledSession, "id", 500L);

        when(sessionService.confirmReschedule(
                eq(500L),
                eq(Instant.parse("2026-10-16T10:00:00Z")),
                eq(Instant.parse("2026-10-16T13:00:00Z")),
                eq(Instant.parse("2026-10-16T09:00:00Z")),
                eq("reschedule-sha256-token-123456"),
                eq("Rescheduling for facility maintenance"),
                eq("42")
        )).thenReturn(rescheduledSession);

        String payload = """
                {
                    "newStartAt": "2026-10-16T10:00:00Z",
                    "newEndAt": "2026-10-16T13:00:00Z",
                    "newBookingCloseAt": "2026-10-16T09:00:00Z",
                    "previewToken": "reschedule-sha256-token-123456",
                    "reason": "Rescheduling for facility maintenance",
                    "actorId": "spoofed-admin"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/reschedule")
                        .principal(sampleAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)));

        verify(sessionService).confirmReschedule(
                eq(500L),
                eq(Instant.parse("2026-10-16T10:00:00Z")),
                eq(Instant.parse("2026-10-16T13:00:00Z")),
                eq(Instant.parse("2026-10-16T09:00:00Z")),
                eq("reschedule-sha256-token-123456"),
                eq("Rescheduling for facility maintenance"),
                eq("42")
        );
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/reschedule with stale token returns 409 Conflict")
    void confirmRescheduleStaleToken() throws Exception {
        when(sessionService.confirmReschedule(eq(500L), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Preview token is stale or invalid. Re-preview the session change before confirming."));

        String payload = """
                {
                    "newStartAt": "2026-10-16T10:00:00Z",
                    "newEndAt": "2026-10-16T13:00:00Z",
                    "previewToken": "stale-token",
                    "reason": "Maintenance"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/reschedule")
                        .principal(sampleAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Preview token is stale or invalid")));
    }

    // =========================================================================
    // 5. POST /api/v1/sessions/{id}/transfer-preview
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/transfer-preview returns 200 OK with preview payload")
    void previewTransferSuccess() throws Exception {
        when(sessionService.previewCenterTransfer(
                eq(500L),
                eq(201L),
                eq(Instant.parse("2026-10-16T10:00:00Z")),
                eq(Instant.parse("2026-10-16T13:00:00Z")),
                eq(Instant.parse("2026-10-16T09:00:00Z"))
        )).thenReturn(transferPreview);

        String payload = """
                {
                    "newCenterId": 201,
                    "newStartAt": "2026-10-16T10:00:00Z",
                    "newEndAt": "2026-10-16T13:00:00Z",
                    "newBookingCloseAt": "2026-10-16T09:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/transfer-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(500)))
                .andExpect(jsonPath("$.changeType", is("CENTER_TRANSFERRED")))
                .andExpect(jsonPath("$.oldSnapshot.centerId", is(200)))
                .andExpect(jsonPath("$.newSnapshot.centerId", is(201)))
                .andExpect(jsonPath("$.previewToken", is("transfer-sha256-token-123456")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/transfer-preview with nonexistent target center returns 404 Not Found")
    void previewTransferTargetCenterNotFound() throws Exception {
        when(sessionService.previewCenterTransfer(eq(500L), eq(999L), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        String payload = """
                {
                    "newCenterId": 999,
                    "newStartAt": "2026-10-16T10:00:00Z",
                    "newEndAt": "2026-10-16T13:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/transfer-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination center not found with ID: 999")));
    }

    // =========================================================================
    // 6. POST /api/v1/sessions/{id}/transfer
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/transfer confirms center transfer with trusted principal actorId and returns 200 OK")
    void confirmTransferSuccess() throws Exception {
        VaccinationSession transferredSession = new VaccinationSession(
                sampleCampaign,
                targetCenter,
                Instant.parse("2026-10-16T10:00:00Z"),
                Instant.parse("2026-10-16T13:00:00Z"),
                Instant.parse("2026-10-16T09:00:00Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(transferredSession, "id", 500L);

        when(sessionService.confirmCenterTransfer(
                eq(500L),
                eq(201L),
                eq(Instant.parse("2026-10-16T10:00:00Z")),
                eq(Instant.parse("2026-10-16T13:00:00Z")),
                eq(Instant.parse("2026-10-16T09:00:00Z")),
                eq("transfer-sha256-token-123456"),
                eq("Center relocation due to renovation"),
                eq("42")
        )).thenReturn(transferredSession);

        String payload = """
                {
                    "newCenterId": 201,
                    "newStartAt": "2026-10-16T10:00:00Z",
                    "newEndAt": "2026-10-16T13:00:00Z",
                    "newBookingCloseAt": "2026-10-16T09:00:00Z",
                    "previewToken": "transfer-sha256-token-123456",
                    "reason": "Center relocation due to renovation"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/transfer")
                        .principal(sampleAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.centerId", is(201)))
                .andExpect(jsonPath("$.startAt", is("2026-10-16T10:00:00Z")));

        verify(sessionService).confirmCenterTransfer(
                eq(500L),
                eq(201L),
                eq(Instant.parse("2026-10-16T10:00:00Z")),
                eq(Instant.parse("2026-10-16T13:00:00Z")),
                eq(Instant.parse("2026-10-16T09:00:00Z")),
                eq("transfer-sha256-token-123456"),
                eq("Center relocation due to renovation"),
                eq("42")
        );
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/transfer anonymously returns 401 Unauthorized")
    void confirmTransferAnonymousReturns401() throws Exception {
        String payload = """
                {
                    "newCenterId": 201,
                    "newStartAt": "2026-10-16T10:00:00Z",
                    "newEndAt": "2026-10-16T13:00:00Z",
                    "previewToken": "transfer-sha256-token-123456",
                    "reason": "Center relocation due to renovation"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/transfer ignores client-supplied spoofed actorId and uses trusted principal userId")
    void confirmTransferSpoofedActorIgnored() throws Exception {
        VaccinationSession transferredSession = new VaccinationSession(
                sampleCampaign,
                targetCenter,
                Instant.parse("2026-10-16T10:00:00Z"),
                Instant.parse("2026-10-16T13:00:00Z"),
                Instant.parse("2026-10-16T09:00:00Z"),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(transferredSession, "id", 500L);

        when(sessionService.confirmCenterTransfer(
                eq(500L),
                eq(201L),
                eq(Instant.parse("2026-10-16T10:00:00Z")),
                eq(Instant.parse("2026-10-16T13:00:00Z")),
                eq(Instant.parse("2026-10-16T09:00:00Z")),
                eq("transfer-sha256-token-123456"),
                eq("Center relocation due to renovation"),
                eq("42")
        )).thenReturn(transferredSession);

        String payload = """
                {
                    "newCenterId": 201,
                    "newStartAt": "2026-10-16T10:00:00Z",
                    "newEndAt": "2026-10-16T13:00:00Z",
                    "newBookingCloseAt": "2026-10-16T09:00:00Z",
                    "previewToken": "transfer-sha256-token-123456",
                    "reason": "Center relocation due to renovation",
                    "actorId": "spoofed-999"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/transfer")
                        .principal(sampleAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)));

        verify(sessionService).confirmCenterTransfer(
                eq(500L),
                eq(201L),
                eq(Instant.parse("2026-10-16T10:00:00Z")),
                eq(Instant.parse("2026-10-16T13:00:00Z")),
                eq(Instant.parse("2026-10-16T09:00:00Z")),
                eq("transfer-sha256-token-123456"),
                eq("Center relocation due to renovation"),
                eq("42")
        );
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/transfer with missing newCenterId returns 400 Bad Request")
    void confirmTransferValidationMissingCenterId() throws Exception {
        String invalidPayload = """
                {
                    "newStartAt": "2026-10-16T10:00:00Z",
                    "newEndAt": "2026-10-16T13:00:00Z",
                    "previewToken": "transfer-sha256-token-123456",
                    "reason": "Center relocation"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/500/transfer")
                        .principal(sampleAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("newCenterId")));
    }

    // =========================================================================
    // 7. GET /api/v1/sessions/{id}/change-events
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/change-events returns 200 OK with list of change events")
    void getSessionChangeEventsSuccess() throws Exception {
        SessionChangeEvent event1 = new SessionChangeEvent(
                sampleSession,
                SessionChangeType.RESCHEDULED,
                oldSnapshot,
                newRescheduleSnapshot,
                "Schedule adjustment",
                0,
                0,
                "officer-01"
        );
        ReflectionTestUtils.setField(event1, "id", 1001L);
        ReflectionTestUtils.setField(event1, "createdAt", Instant.parse("2026-08-05T09:00:00Z"));

        SessionChangeEvent event2 = new SessionChangeEvent(
                sampleSession,
                SessionChangeType.CANCELLED,
                newRescheduleSnapshot,
                newCancelledSnapshot,
                "Emergency weather cancellation",
                0,
                0,
                "officer-02"
        );
        ReflectionTestUtils.setField(event2, "id", 1002L);
        ReflectionTestUtils.setField(event2, "createdAt", Instant.parse("2026-08-10T14:00:00Z"));

        when(sessionService.getSessionChangeEvents(500L)).thenReturn(List.of(event1, event2));

        mockMvc.perform(get("/api/v1/sessions/500/change-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1001)))
                .andExpect(jsonPath("$[0].sessionId", is(500)))
                .andExpect(jsonPath("$[0].changeType", is("RESCHEDULED")))
                .andExpect(jsonPath("$[0].reason", is("Schedule adjustment")))
                .andExpect(jsonPath("$[0].actorId", is("officer-01")))
                .andExpect(jsonPath("$[0].createdAt", is("2026-08-05T09:00:00Z")))
                .andExpect(jsonPath("$[1].id", is(1002)))
                .andExpect(jsonPath("$[1].sessionId", is(500)))
                .andExpect(jsonPath("$[1].changeType", is("CANCELLED")))
                .andExpect(jsonPath("$[1].reason", is("Emergency weather cancellation")))
                .andExpect(jsonPath("$[1].actorId", is("officer-02")))
                .andExpect(jsonPath("$[1].createdAt", is("2026-08-10T14:00:00Z")));

        verify(sessionService).getSessionChangeEvents(500L);
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/change-events for session with no events returns 200 OK with empty array")
    void getSessionChangeEventsEmpty() throws Exception {
        when(sessionService.getSessionChangeEvents(500L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/sessions/500/change-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(sessionService).getSessionChangeEvents(500L);
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/change-events for nonexistent session returns 404 Not Found")
    void getSessionChangeEventsSessionNotFound() throws Exception {
        when(sessionService.getSessionChangeEvents(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination session not found with ID: 999"));

        mockMvc.perform(get("/api/v1/sessions/999/change-events"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination session not found with ID: 999")));
    }
}
