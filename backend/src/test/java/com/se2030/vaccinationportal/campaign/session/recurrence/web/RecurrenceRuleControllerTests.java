package com.se2030.vaccinationportal.campaign.session.recurrence.web;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.RecurrenceRuleDefinitionSnapshot;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SeriesOccurrenceCategory;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.recurrence.service.RecurrenceGenerationResult;
import com.se2030.vaccinationportal.campaign.session.recurrence.service.SeriesChangePreview;
import com.se2030.vaccinationportal.campaign.session.recurrence.service.SeriesChangeResult;
import com.se2030.vaccinationportal.campaign.session.recurrence.service.SeriesOccurrencePreviewItem;
import com.se2030.vaccinationportal.campaign.session.recurrence.service.SessionOccurrenceGenerationService;
import com.se2030.vaccinationportal.campaign.session.recurrence.service.SessionRecurrenceRuleService;
import com.se2030.vaccinationportal.campaign.session.recurrence.service.SessionSeriesChangeService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class RecurrenceRuleControllerTests {

    private MockMvc mockMvc;

    @Mock
    private SessionRecurrenceRuleService recurrenceRuleService;

    @Mock
    private SessionOccurrenceGenerationService occurrenceGenerationService;

    @Mock
    private SessionSeriesChangeService seriesChangeService;

    private VaccinationCampaign sampleCampaign;
    private VaccinationCenter sampleCenter;
    private SessionRecurrenceRule sampleRule;

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

        mockMvc = MockMvcBuilders.standaloneSetup(new RecurrenceRuleController(
                        recurrenceRuleService, occurrenceGenerationService, seriesChangeService))
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

        sampleRule = new SessionRecurrenceRule(
                sampleCampaign,
                sampleCenter,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 31),
                LocalTime.of(8, 30),
                LocalTime.of(16, 30),
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(sampleRule, "id", 1L);
        ReflectionTestUtils.setField(sampleRule, "generationKey", "gen-key-uuid-12345");
        ReflectionTestUtils.setField(sampleRule, "createdAt", Instant.parse("2026-08-01T10:00:00Z"));
        ReflectionTestUtils.setField(sampleRule, "updatedAt", Instant.parse("2026-08-01T10:00:00Z"));
    }

    @Nested
    @DisplayName("POST /api/v1/recurrence-rules - Create Recurrence Rule")
    class CreateRuleTests {

        @Test
        @DisplayName("201 Created with Location header and response body on valid payload")
        void createRuleSuccess() throws Exception {
            when(recurrenceRuleService.createRule(
                    eq(100L),
                    eq(200L),
                    eq(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
                    eq(LocalDate.of(2026, 9, 1)),
                    eq(LocalDate.of(2026, 10, 31)),
                    eq(LocalTime.of(8, 30)),
                    eq(LocalTime.of(16, 30)),
                    eq("Asia/Colombo")
            )).thenReturn(sampleRule);

            String requestJson = """
                    {
                        "campaignId": 100,
                        "centerId": 200,
                        "weekdays": ["MONDAY", "WEDNESDAY", "FRIDAY"],
                        "startDate": "2026-09-01",
                        "endDate": "2026-10-31",
                        "startTime": "08:30:00",
                        "endTime": "16:30:00",
                        "timezone": "Asia/Colombo"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/recurrence-rules/1"))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.campaignId", is(100)))
                    .andExpect(jsonPath("$.centerId", is(200)))
                    .andExpect(jsonPath("$.weekdays", hasSize(3)))
                    .andExpect(jsonPath("$.startDate", is("2026-09-01")))
                    .andExpect(jsonPath("$.endDate", is("2026-10-31")))
                    .andExpect(jsonPath("$.startTime", is("08:30:00")))
                    .andExpect(jsonPath("$.endTime", is("16:30:00")))
                    .andExpect(jsonPath("$.timezone", is("Asia/Colombo")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")))
                    .andExpect(jsonPath("$.generationKey", is("gen-key-uuid-12345")))
                    .andExpect(jsonPath("$.createdAt", notNullValue()))
                    .andExpect(jsonPath("$.updatedAt", notNullValue()));
        }

        @Test
        @DisplayName("400 Bad Request on validation failure (missing campaign, empty weekdays, blank timezone)")
        void createRuleValidationFailure() throws Exception {
            String requestJson = """
                    {
                        "weekdays": [],
                        "startDate": "2026-09-01",
                        "endDate": "2026-10-31",
                        "startTime": "08:30:00",
                        "endTime": "16:30:00",
                        "timezone": "   "
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors", notNullValue()));
        }

        @Test
        @DisplayName("400 Bad Request on domain validation violation (e.g. 366-day safety limit)")
        void createRuleDomainHorizonFailure() throws Exception {
            when(recurrenceRuleService.createRule(any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new IllegalArgumentException("Recurrence rule duration exceeds maximum allowed horizon of 366 days"));

            String requestJson = """
                    {
                        "campaignId": 100,
                        "centerId": 200,
                        "weekdays": ["MONDAY"],
                        "startDate": "2026-01-01",
                        "endDate": "2028-01-01",
                        "startTime": "08:30:00",
                        "endTime": "16:30:00",
                        "timezone": "Asia/Colombo"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("366 days")));
        }

        @Test
        @DisplayName("404 Not Found when campaign or center does not exist")
        void createRuleResourceNotFound() throws Exception {
            when(recurrenceRuleService.createRule(any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Vaccination campaign not found with ID: 999"));

            String requestJson = """
                    {
                        "campaignId": 999,
                        "centerId": 200,
                        "weekdays": ["MONDAY"],
                        "startDate": "2026-09-01",
                        "endDate": "2026-10-31",
                        "startTime": "08:30:00",
                        "endTime": "16:30:00",
                        "timezone": "Asia/Colombo"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("Vaccination campaign not found")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/recurrence-rules/{id} - Get Rule by ID")
    class GetRuleByIdTests {

        @Test
        @DisplayName("200 OK when rule exists")
        void getRuleByIdSuccess() throws Exception {
            when(recurrenceRuleService.getRequiredRuleById(1L)).thenReturn(sampleRule);

            mockMvc.perform(get("/api/v1/recurrence-rules/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.campaignId", is(100)))
                    .andExpect(jsonPath("$.centerId", is(200)))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("404 Not Found when rule does not exist")
        void getRuleByIdNotFound() throws Exception {
            when(recurrenceRuleService.getRequiredRuleById(999L))
                    .thenThrow(new ResourceNotFoundException("Session recurrence rule not found with ID: 999"));

            mockMvc.perform(get("/api/v1/recurrence-rules/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("Session recurrence rule not found with ID: 999")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/recurrence-rules - List and Filter Rules")
    class ListRulesTests {

        @Test
        @DisplayName("200 OK without filters returns all rules")
        void listAllRules() throws Exception {
            when(recurrenceRuleService.getRules(null, null, null)).thenReturn(List.of(sampleRule));

            mockMvc.perform(get("/api/v1/recurrence-rules"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(1)));
        }

        @Test
        @DisplayName("200 OK with filters passes parameters to service")
        void listRulesWithFilters() throws Exception {
            when(recurrenceRuleService.getRules(100L, 200L, SessionRecurrenceRuleStatus.ACTIVE))
                    .thenReturn(List.of(sampleRule));

            mockMvc.perform(get("/api/v1/recurrence-rules")
                            .param("campaignId", "100")
                            .param("centerId", "200")
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(recurrenceRuleService).getRules(100L, 200L, SessionRecurrenceRuleStatus.ACTIVE);
        }

        @Test
        @DisplayName("200 OK with empty result returns empty JSON array")
        void listRulesEmpty() throws Exception {
            when(recurrenceRuleService.getRules(any(), any(), any())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/recurrence-rules"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/recurrence-rules/{id} - Replace Future Recurrence Definition")
    class UpdateRuleTests {

        @Test
        @DisplayName("200 OK on valid update payload")
        void updateFutureRuleSuccess() throws Exception {
            when(recurrenceRuleService.updateFutureRule(
                    eq(1L),
                    eq(EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)),
                    eq(LocalDate.of(2026, 9, 15)),
                    eq(LocalDate.of(2026, 11, 15)),
                    eq(LocalTime.of(9, 0)),
                    eq(LocalTime.of(17, 0)),
                    eq("Asia/Colombo")
            )).thenReturn(sampleRule);

            String requestJson = """
                    {
                        "weekdays": ["TUESDAY", "THURSDAY"],
                        "startDate": "2026-09-15",
                        "endDate": "2026-11-15",
                        "startTime": "09:00:00",
                        "endTime": "17:00:00",
                        "timezone": "Asia/Colombo"
                    }
                    """;

            mockMvc.perform(put("/api/v1/recurrence-rules/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)));
        }

        @Test
        @DisplayName("400 Bad Request when update violates domain rules (sessions already generated)")
        void updateFutureRuleDomainError() throws Exception {
            when(recurrenceRuleService.updateFutureRule(any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new IllegalArgumentException("Cannot update rule definition directly because occurrences have already been generated; use series change workflow"));

            String requestJson = """
                    {
                        "weekdays": ["TUESDAY"],
                        "startDate": "2026-09-15",
                        "endDate": "2026-11-15",
                        "startTime": "09:00:00",
                        "endTime": "17:00:00",
                        "timezone": "Asia/Colombo"
                    }
                    """;

            mockMvc.perform(put("/api/v1/recurrence-rules/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("use series change workflow")));
        }

        @Test
        @DisplayName("404 Not Found when rule does not exist")
        void updateFutureRuleNotFound() throws Exception {
            when(recurrenceRuleService.updateFutureRule(any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Session recurrence rule not found with ID: 999"));

            String requestJson = """
                    {
                        "weekdays": ["TUESDAY"],
                        "startDate": "2026-09-15",
                        "endDate": "2026-11-15",
                        "startTime": "09:00:00",
                        "endTime": "17:00:00",
                        "timezone": "Asia/Colombo"
                    }
                    """;

            mockMvc.perform(put("/api/v1/recurrence-rules/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/recurrence-rules/{id}/generate - Generate Occurrences")
    class GenerateOccurrencesTests {

        @Test
        @DisplayName("200 OK without body generates full rule range")
        void generateOccurrencesNoBody() throws Exception {
            VaccinationSession session = new VaccinationSession(
                    sampleCampaign,
                    sampleCenter,
                    Instant.parse("2026-09-01T03:00:00Z"),
                    Instant.parse("2026-09-01T11:00:00Z"),
                    Instant.parse("2026-09-01T03:00:00Z"),
                    "Asia/Colombo"
            );
            ReflectionTestUtils.setField(session, "id", 501L);

            RecurrenceGenerationResult result = new RecurrenceGenerationResult(5, 2, List.of(session));
            when(occurrenceGenerationService.generateOccurrences(1L)).thenReturn(result);

            mockMvc.perform(post("/api/v1/recurrence-rules/1/generate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.createdCount", is(5)))
                    .andExpect(jsonPath("$.existingCount", is(2)))
                    .andExpect(jsonPath("$.sessions", hasSize(1)))
                    .andExpect(jsonPath("$.sessions[0].id", is(501)));

            verify(occurrenceGenerationService).generateOccurrences(1L);
        }

        @Test
        @DisplayName("200 OK with windowed request generates within requested window")
        void generateOccurrencesWithWindow() throws Exception {
            RecurrenceGenerationResult result = new RecurrenceGenerationResult(3, 0, Collections.emptyList());
            when(occurrenceGenerationService.generateOccurrences(
                    1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15))).thenReturn(result);

            String requestJson = """
                    {
                        "requestedStartDate": "2026-09-01",
                        "requestedEndDate": "2026-09-15"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.createdCount", is(3)))
                    .andExpect(jsonPath("$.existingCount", is(0)));

            verify(occurrenceGenerationService).generateOccurrences(
                    1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15));
        }

        @Test
        @DisplayName("404 Not Found when rule does not exist")
        void generateOccurrencesNotFound() throws Exception {
            when(occurrenceGenerationService.generateOccurrences(999L))
                    .thenThrow(new ResourceNotFoundException("Session recurrence rule not found with ID: 999"));

            mockMvc.perform(post("/api/v1/recurrence-rules/999/generate"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/recurrence-rules/{id}/change-preview - Preview Series Change")
    class PreviewSeriesChangeTests {

        @Test
        @DisplayName("200 OK on valid series change preview request")
        void previewSeriesChangeSuccess() throws Exception {
            RecurrenceRuleDefinitionSnapshot currentDef = new RecurrenceRuleDefinitionSnapshot(
                    EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 10, 31),
                    LocalTime.of(8, 30),
                    LocalTime.of(16, 30),
                    "Asia/Colombo"
            );
            RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                    EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 10, 31),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    "Asia/Colombo"
            );

            SeriesOccurrencePreviewItem item = new SeriesOccurrencePreviewItem(
                    "gen-key-uuid-12345:2026-09-01",
                    LocalDate.of(2026, 9, 1),
                    SeriesOccurrenceCategory.RETAINED,
                    501L,
                    new SessionScheduleSnapshot(200L, "Asia/Colombo", Instant.parse("2026-09-01T03:00:00Z"), Instant.parse("2026-09-01T11:00:00Z"), Instant.parse("2026-09-01T03:00:00Z"), SessionLifecycleStatus.DRAFT),
                    new SessionScheduleSnapshot(200L, "Asia/Colombo", Instant.parse("2026-09-01T03:30:00Z"), Instant.parse("2026-09-01T11:30:00Z"), Instant.parse("2026-09-01T03:30:00Z"), SessionLifecycleStatus.DRAFT),
                    0,
                    0
            );

            SeriesChangePreview preview = new SeriesChangePreview(
                    1L,
                    currentDef,
                    proposedDef,
                    List.of(item),
                    0,
                    0,
                    "sha256previewtoken1234567890abcdef"
            );

            when(seriesChangeService.previewSeriesChange(eq(1L), any(RecurrenceRuleDefinitionSnapshot.class)))
                    .thenReturn(preview);

            String requestJson = """
                    {
                        "weekdays": ["MONDAY", "FRIDAY"],
                        "startDate": "2026-09-01",
                        "endDate": "2026-10-31",
                        "startTime": "09:00:00",
                        "endTime": "17:00:00",
                        "timezone": "Asia/Colombo"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/change-preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ruleId", is(1)))
                    .andExpect(jsonPath("$.currentRuleDefinition.weekdays", hasSize(3)))
                    .andExpect(jsonPath("$.proposedRuleDefinition.weekdays", hasSize(2)))
                    .andExpect(jsonPath("$.occurrenceEntries", hasSize(1)))
                    .andExpect(jsonPath("$.occurrenceEntries[0].occurrenceKey", is("gen-key-uuid-12345:2026-09-01")))
                    .andExpect(jsonPath("$.occurrenceEntries[0].category", is("RETAINED")))
                    .andExpect(jsonPath("$.previewToken", is("sha256previewtoken1234567890abcdef")));
        }

        @Test
        @DisplayName("409 Conflict when impact reader is unavailable for SCHEDULED session change")
        void previewSeriesChangeImpactReaderConflict() throws Exception {
            when(seriesChangeService.previewSeriesChange(any(), any()))
                    .thenThrow(new IllegalStateException("Session impact reader is not available to assess SCHEDULED session changes"));

            String requestJson = """
                    {
                        "weekdays": ["MONDAY"],
                        "startDate": "2026-09-01",
                        "endDate": "2026-10-31",
                        "startTime": "09:00:00",
                        "endTime": "17:00:00",
                        "timezone": "Asia/Colombo"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/change-preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Session impact reader is not available")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/recurrence-rules/{id}/change - Confirm Series Change")
    class ConfirmSeriesChangeTests {

        @Test
        @DisplayName("200 OK on valid series change confirmation with authenticated principal")
        void confirmSeriesChangeSuccess() throws Exception {
            RecurrenceRuleDefinitionSnapshot currentDef = new RecurrenceRuleDefinitionSnapshot(
                    EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 10, 31),
                    LocalTime.of(8, 30),
                    LocalTime.of(16, 30),
                    "Asia/Colombo"
            );
            RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                    EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 10, 31),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    "Asia/Colombo"
            );
            SeriesChangePreview preview = new SeriesChangePreview(
                    1L, currentDef, proposedDef, Collections.emptyList(), 0, 0, "sha256previewtoken1234567890abcdef"
            );

            SeriesChangeResult result = new SeriesChangeResult(
                    sampleRule, preview, 10, 5, 2, 1, 0, 15
            );

            when(seriesChangeService.confirmSeriesChange(
                    eq(1L), any(RecurrenceRuleDefinitionSnapshot.class), eq("sha256previewtoken1234567890abcdef"), eq("Schedule optimization"), eq("42")
            )).thenReturn(result);

            String requestJson = """
                    {
                        "weekdays": ["MONDAY", "FRIDAY"],
                        "startDate": "2026-09-01",
                        "endDate": "2026-10-31",
                        "startTime": "09:00:00",
                        "endTime": "17:00:00",
                        "timezone": "Asia/Colombo",
                        "previewToken": "sha256previewtoken1234567890abcdef",
                        "reason": "Schedule optimization"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/change")
                            .principal(sampleAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rule.id", is(1)))
                    .andExpect(jsonPath("$.rescheduledCount", is(10)))
                    .andExpect(jsonPath("$.cancelledCount", is(5)))
                    .andExpect(jsonPath("$.newCount", is(2)))
                    .andExpect(jsonPath("$.preservedCount", is(1)))
                    .andExpect(jsonPath("$.ineligibleCount", is(0)))
                    .andExpect(jsonPath("$.eventsCreatedCount", is(15)));

            verify(seriesChangeService).confirmSeriesChange(
                    eq(1L), any(RecurrenceRuleDefinitionSnapshot.class), eq("sha256previewtoken1234567890abcdef"), eq("Schedule optimization"), eq("42")
            );
        }

        @Test
        @DisplayName("401 Unauthorized when unauthenticated/anonymous")
        void confirmSeriesChangeAnonymousReturns401() throws Exception {
            String requestJson = """
                    {
                        "weekdays": ["MONDAY", "FRIDAY"],
                        "startDate": "2026-09-01",
                        "endDate": "2026-10-31",
                        "startTime": "09:00:00",
                        "endTime": "17:00:00",
                        "timezone": "Asia/Colombo",
                        "previewToken": "sha256previewtoken1234567890abcdef",
                        "reason": "Schedule optimization"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("200 OK and ignores spoofed body actorId, forwarding authenticated principal userId")
        void confirmSeriesChangeIgnoresSpoofedActorId() throws Exception {
            RecurrenceRuleDefinitionSnapshot currentDef = new RecurrenceRuleDefinitionSnapshot(
                    EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 10, 31),
                    LocalTime.of(8, 30),
                    LocalTime.of(16, 30),
                    "Asia/Colombo"
            );
            SeriesChangePreview preview = new SeriesChangePreview(
                    1L, currentDef, currentDef, Collections.emptyList(), 0, 0, "sha256previewtoken1234567890abcdef"
            );
            SeriesChangeResult result = new SeriesChangeResult(
                    sampleRule, preview, 10, 5, 2, 1, 0, 15
            );

            when(seriesChangeService.confirmSeriesChange(
                    eq(1L), any(RecurrenceRuleDefinitionSnapshot.class), eq("sha256previewtoken1234567890abcdef"), eq("Schedule optimization"), eq("42")
            )).thenReturn(result);

            String requestJson = """
                    {
                        "weekdays": ["MONDAY", "FRIDAY"],
                        "startDate": "2026-09-01",
                        "endDate": "2026-10-31",
                        "startTime": "09:00:00",
                        "endTime": "17:00:00",
                        "timezone": "Asia/Colombo",
                        "previewToken": "sha256previewtoken1234567890abcdef",
                        "reason": "Schedule optimization",
                        "actorId": "spoofed-attacker-999"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/change")
                            .principal(sampleAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            verify(seriesChangeService).confirmSeriesChange(
                    eq(1L), any(RecurrenceRuleDefinitionSnapshot.class), eq("sha256previewtoken1234567890abcdef"), eq("Schedule optimization"), eq("42")
            );
        }

        @Test
        @DisplayName("409 Conflict on stale preview token during confirmation")
        void confirmSeriesChangeStaleTokenConflict() throws Exception {
            when(seriesChangeService.confirmSeriesChange(any(), any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("Preview is stale or session state has changed; please review the updated preview before confirming"));

            String requestJson = """
                    {
                        "weekdays": ["MONDAY", "FRIDAY"],
                        "startDate": "2026-09-01",
                        "endDate": "2026-10-31",
                        "startTime": "09:00:00",
                        "endTime": "17:00:00",
                        "timezone": "Asia/Colombo",
                        "previewToken": "stale-preview-token",
                        "reason": "Schedule optimization"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/change")
                            .principal(sampleAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Preview is stale")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/recurrence-rules/{id}/cancellation-preview - Preview Series Cancellation")
    class PreviewSeriesCancellationTests {

        @Test
        @DisplayName("200 OK with preview response and no request body")
        void previewSeriesCancellationSuccess() throws Exception {
            RecurrenceRuleDefinitionSnapshot currentDef = new RecurrenceRuleDefinitionSnapshot(
                    EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 10, 31),
                    LocalTime.of(8, 30),
                    LocalTime.of(16, 30),
                    "Asia/Colombo"
            );

            SeriesChangePreview preview = new SeriesChangePreview(
                    1L,
                    currentDef,
                    currentDef,
                    Collections.emptyList(),
                    0,
                    0,
                    "sha256canceltoken1234567890abcdef"
            );

            when(seriesChangeService.previewSeriesCancellation(1L)).thenReturn(preview);

            mockMvc.perform(post("/api/v1/recurrence-rules/1/cancellation-preview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ruleId", is(1)))
                    .andExpect(jsonPath("$.previewToken", is("sha256canceltoken1234567890abcdef")));
        }

        @Test
        @DisplayName("404 Not Found when rule does not exist")
        void previewSeriesCancellationNotFound() throws Exception {
            when(seriesChangeService.previewSeriesCancellation(999L))
                    .thenThrow(new ResourceNotFoundException("Session recurrence rule not found with ID: 999"));

            mockMvc.perform(post("/api/v1/recurrence-rules/999/cancellation-preview"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/recurrence-rules/{id}/cancel - Confirm Series Cancellation")
    class ConfirmSeriesCancellationTests {

        @Test
        @DisplayName("200 OK on valid cancellation confirmation with authenticated principal")
        void confirmSeriesCancellationSuccess() throws Exception {
            RecurrenceRuleDefinitionSnapshot currentDef = new RecurrenceRuleDefinitionSnapshot(
                    EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 10, 31),
                    LocalTime.of(8, 30),
                    LocalTime.of(16, 30),
                    "Asia/Colombo"
            );
            SeriesChangePreview preview = new SeriesChangePreview(
                    1L, currentDef, currentDef, Collections.emptyList(), 0, 0, "sha256canceltoken1234567890abcdef"
            );

            sampleRule.cancel();
            SeriesChangeResult result = new SeriesChangeResult(
                    sampleRule, preview, 0, 15, 0, 2, 1, 15
            );

            when(seriesChangeService.confirmSeriesCancellation(
                    1L, "sha256canceltoken1234567890abcdef", "Center emergency closure", "42"
            )).thenReturn(result);

            String requestJson = """
                    {
                        "previewToken": "sha256canceltoken1234567890abcdef",
                        "reason": "Center emergency closure"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/cancel")
                            .principal(sampleAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rule.id", is(1)))
                    .andExpect(jsonPath("$.rule.status", is("CANCELLED")))
                    .andExpect(jsonPath("$.cancelledCount", is(15)))
                    .andExpect(jsonPath("$.eventsCreatedCount", is(15)));

            verify(seriesChangeService).confirmSeriesCancellation(
                    1L, "sha256canceltoken1234567890abcdef", "Center emergency closure", "42"
            );
        }

        @Test
        @DisplayName("401 Unauthorized when unauthenticated/anonymous")
        void confirmSeriesCancellationAnonymousReturns401() throws Exception {
            String requestJson = """
                    {
                        "previewToken": "sha256canceltoken1234567890abcdef",
                        "reason": "Center emergency closure"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("200 OK and ignores spoofed body actorId, forwarding authenticated principal userId")
        void confirmSeriesCancellationIgnoresSpoofedActorId() throws Exception {
            RecurrenceRuleDefinitionSnapshot currentDef = new RecurrenceRuleDefinitionSnapshot(
                    EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 10, 31),
                    LocalTime.of(8, 30),
                    LocalTime.of(16, 30),
                    "Asia/Colombo"
            );
            SeriesChangePreview preview = new SeriesChangePreview(
                    1L, currentDef, currentDef, Collections.emptyList(), 0, 0, "sha256canceltoken1234567890abcdef"
            );
            sampleRule.cancel();
            SeriesChangeResult result = new SeriesChangeResult(
                    sampleRule, preview, 0, 15, 0, 2, 1, 15
            );

            when(seriesChangeService.confirmSeriesCancellation(
                    1L, "sha256canceltoken1234567890abcdef", "Center emergency closure", "42"
            )).thenReturn(result);

            String requestJson = """
                    {
                        "previewToken": "sha256canceltoken1234567890abcdef",
                        "reason": "Center emergency closure",
                        "actorId": "spoofed-999"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/cancel")
                            .principal(sampleAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            verify(seriesChangeService).confirmSeriesCancellation(
                    1L, "sha256canceltoken1234567890abcdef", "Center emergency closure", "42"
            );
        }

        @Test
        @DisplayName("400 Bad Request when confirmation fields are invalid (blank token, blank reason)")
        void confirmSeriesCancellationValidationFailure() throws Exception {
            String requestJson = """
                    {
                        "previewToken": "",
                        "reason": "  "
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/cancel")
                            .principal(sampleAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors", notNullValue()));
        }

        @Test
        @DisplayName("409 Conflict when preview token is stale during cancellation")
        void confirmSeriesCancellationStaleTokenConflict() throws Exception {
            when(seriesChangeService.confirmSeriesCancellation(any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("Preview token is stale or invalid. Re-preview the series cancellation before confirming."));

            String requestJson = """
                    {
                        "previewToken": "stale-preview-token",
                        "reason": "Center emergency closure"
                    }
                    """;

            mockMvc.perform(post("/api/v1/recurrence-rules/1/cancel")
                            .principal(sampleAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Preview token is stale")));
        }
    }
}
