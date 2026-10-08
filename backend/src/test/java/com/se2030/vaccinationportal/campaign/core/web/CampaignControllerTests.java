package com.se2030.vaccinationportal.campaign.core.web;

import com.se2030.vaccinationportal.campaign.web.GlobalCampaignExceptionHandler;


import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.core.dto.response.CampaignSummaryResponse;
import com.se2030.vaccinationportal.campaign.core.mapper.CampaignDtoMapper;
import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleChangeType;
import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.core.service.VaccinationCampaignService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class CampaignControllerTests {

    private MockMvc mockMvc;

    @Mock
    private VaccinationCampaignService campaignService;

    private VaccineType sampleVaccineType;
    private TargetGroup sampleTargetGroup;
    private VaccinationCampaign sampleCampaign;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CampaignController(campaignService))
                .setControllerAdvice(new GlobalCampaignExceptionHandler())
                .setValidator(validator)
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
                        return null;
                    }
                })
                .build();

        sampleVaccineType = new VaccineType("VT-COVID-01", "COVID-19 mRNA Vaccine", "dose", true);
        ReflectionTestUtils.setField(sampleVaccineType, "id", 10L);

        sampleTargetGroup = new TargetGroup("TG-ELDERLY", "Adults 65 and older", true);
        ReflectionTestUtils.setField(sampleTargetGroup, "id", 20L);

        sampleCampaign = new VaccinationCampaign(
                "CMP-2026-AUTUMN",
                "Autumn 2026 Campaign",
                "Community booster drive",
                sampleVaccineType,
                sampleTargetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 30),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2026-11-29T18:00:00Z")
        );
        ReflectionTestUtils.setField(sampleCampaign, "id", 100L);
        ReflectionTestUtils.setField(sampleCampaign, "createdAt", Instant.parse("2026-08-01T10:00:00Z"));
        ReflectionTestUtils.setField(sampleCampaign, "updatedAt", Instant.parse("2026-08-01T10:00:00Z"));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns creates campaign and returns 201 with Location header")
    void createCampaignSuccess() throws Exception {
        String jsonPayload = """
                {
                    "code": "CMP-2026-AUTUMN",
                    "name": "Autumn 2026 Campaign",
                    "description": "Community booster drive",
                    "vaccineTypeId": 10,
                    "targetGroupId": 20,
                    "serviceStartDate": "2026-09-01",
                    "serviceEndDate": "2026-11-30",
                    "bookingOpenAt": "2026-08-15T08:00:00Z",
                    "bookingCloseAt": "2026-11-29T18:00:00Z"
                }
                """;

        when(campaignService.createCampaign(
                eq("CMP-2026-AUTUMN"),
                eq("Autumn 2026 Campaign"),
                eq("Community booster drive"),
                eq(10L),
                eq(20L),
                eq(LocalDate.of(2026, 9, 1)),
                eq(LocalDate.of(2026, 11, 30)),
                eq(Instant.parse("2026-08-15T08:00:00Z")),
                eq(Instant.parse("2026-11-29T18:00:00Z"))
        )).thenReturn(sampleCampaign);

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/campaigns/100"))
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.code", is("CMP-2026-AUTUMN")))
                .andExpect(jsonPath("$.name", is("Autumn 2026 Campaign")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.vaccineType.code", is("VT-COVID-01")))
                .andExpect(jsonPath("$.targetGroup.code", is("TG-ELDERLY")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns returns 400 when required fields fail Bean Validation")
    void createCampaignValidationFailure() throws Exception {
        String invalidPayload = """
                {
                    "code": "",
                    "name": "",
                    "description": null,
                    "vaccineTypeId": null,
                    "targetGroupId": null,
                    "serviceStartDate": null,
                    "serviceEndDate": null,
                    "bookingOpenAt": null,
                    "bookingCloseAt": null
                }
                """;

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns returns 404 when master data ID is not found")
    void createCampaignMissingMasterData() throws Exception {
        String jsonPayload = """
                {
                    "code": "CMP-2026-AUTUMN",
                    "name": "Autumn 2026 Campaign",
                    "description": "Community booster drive",
                    "vaccineTypeId": 999,
                    "targetGroupId": 20,
                    "serviceStartDate": "2026-09-01",
                    "serviceEndDate": "2026-11-30",
                    "bookingOpenAt": "2026-08-15T08:00:00Z",
                    "bookingCloseAt": "2026-11-29T18:00:00Z"
                }
                """;

        when(campaignService.createCampaign(any(), any(), any(), eq(999L), any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("VaccineType not found with ID: 999"));

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("VaccineType not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns returns 400 when service date ordering violates MF02-BR-01")
    void createCampaignInvalidServiceDatesReturns400() throws Exception {
        String jsonPayload = """
                {
                    "code": "CMP-2026-INVALID",
                    "name": "Invalid Date Campaign",
                    "description": "Invalid date order",
                    "vaccineTypeId": 10,
                    "targetGroupId": 20,
                    "serviceStartDate": "2026-11-30",
                    "serviceEndDate": "2026-09-01",
                    "bookingOpenAt": "2026-08-15T08:00:00Z",
                    "bookingCloseAt": "2026-08-30T18:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("serviceEndDate")));
        verifyNoInteractions(campaignService);
    }

    @Test
    @DisplayName("PUT /api/v1/campaigns/{id} updates campaign and returns 200")
    void updateCampaignSuccess() throws Exception {
        String updateJson = """
                {
                    "name": "Updated Campaign Name",
                    "description": "Updated Description",
                    "serviceStartDate": "2026-09-01",
                    "serviceEndDate": "2026-11-30",
                    "bookingOpenAt": "2026-08-15T08:00:00Z",
                    "bookingCloseAt": "2026-11-29T18:00:00Z"
                }
                """;

        when(campaignService.updateCampaign(
                eq(100L),
                eq("Updated Campaign Name"),
                eq("Updated Description"),
                eq(LocalDate.of(2026, 9, 1)),
                eq(LocalDate.of(2026, 11, 30)),
                eq(Instant.parse("2026-08-15T08:00:00Z")),
                eq(Instant.parse("2026-11-29T18:00:00Z"))
        )).thenReturn(sampleCampaign);

        mockMvc.perform(put("/api/v1/campaigns/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.code", is("CMP-2026-AUTUMN")));
    }

    @Test
    @DisplayName("PUT /api/v1/campaigns/{id} returns 400 when date ordering is invalid")
    void updateCampaignInvalidDatesReturns400() throws Exception {
        String updateJson = """
                {
                    "name": "Updated Campaign",
                    "description": "Desc",
                    "serviceStartDate": "2026-11-30",
                    "serviceEndDate": "2026-09-01",
                    "bookingOpenAt": "2026-08-15T08:00:00Z",
                    "bookingCloseAt": "2026-11-29T18:00:00Z"
                }
                """;

        mockMvc.perform(put("/api/v1/campaigns/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("serviceEndDate")));
        verifyNoInteractions(campaignService);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "POST, serviceEndDate, 2026-09-01",
            "PUT, serviceEndDate, 2026-09-01",
            "POST, bookingCloseAt, 2026-08-15T08:00:00Z",
            "PUT, bookingCloseAt, 2026-08-15T08:00:00Z",
            "POST, bookingCloseAt, 2026-08-14T08:00:00Z",
            "PUT, bookingCloseAt, 2026-08-14T08:00:00Z",
            "POST, bookingCloseAt, 2026-12-01T00:00:00Z",
            "PUT, bookingCloseAt, 2026-12-01T00:00:00Z",
            "POST, bookingCloseAt, 2026-11-30T23:30:00-02:00",
            "PUT, bookingCloseAt, 2026-11-30T23:30:00-02:00",
            "POST, serviceStartDate, NULL",
            "PUT, serviceStartDate, NULL",
            "POST, serviceEndDate, NULL",
            "PUT, serviceEndDate, NULL",
            "POST, bookingOpenAt, NULL",
            "PUT, bookingOpenAt, NULL",
            "POST, bookingCloseAt, NULL",
            "PUT, bookingCloseAt, NULL"
    }, nullValues = "NULL")
    void invalidDatesAreRejectedBeforeCallingService(String method, String field, String value) throws Exception {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("name", "Campaign date validation");
        payload.put("serviceStartDate", "2026-09-01");
        payload.put("serviceEndDate", "2026-11-30");
        payload.put("bookingOpenAt", "2026-08-15T08:00:00Z");
        payload.put("bookingCloseAt", "2026-08-30T18:00:00Z");
        if (method.equals("POST")) {
            payload.put("code", "CMP-DATE-VALIDATION");
            payload.put("vaccineTypeId", 10);
            payload.put("targetGroupId", 20);
        }
        payload.put(field, value);

        mockMvc.perform((method.equals("POST") ? post("/api/v1/campaigns") : put("/api/v1/campaigns/100"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem(field)));
        verifyNoInteractions(campaignService);
    }

    @ParameterizedTest
    @CsvSource({
            "POST, 2026-11-30T23:59:59Z",
            "PUT, 2026-11-30T23:59:59Z",
            "POST, 2026-12-01T01:00:00+02:00",
            "PUT, 2026-12-01T01:00:00+02:00"
    })
    void bookingCanCloseOnServiceEndDateInUtc(String method, String closeAt) throws Exception {
        String payload = """
                {
                    "name": "Valid boundary dates",
                    "serviceStartDate": "2026-09-01",
                    "serviceEndDate": "2026-11-30",
                    "bookingOpenAt": "2026-08-15T08:00:00Z",
                    "bookingCloseAt": "%s"%s
                }
                """.formatted(closeAt, method.equals("POST")
                        ? ", \"code\": \"CMP-BOUNDARY\", \"vaccineTypeId\": 10, \"targetGroupId\": 20" : "");
        if (method.equals("POST")) {
            when(campaignService.createCampaign(any(), any(), any(), eq(10L), eq(20L), any(), any(), any(), any()))
                    .thenReturn(sampleCampaign);
        } else {
            when(campaignService.updateCampaign(eq(100L), any(), any(), any(), any(), any(), any()))
                    .thenReturn(sampleCampaign);
        }

        mockMvc.perform((method.equals("POST") ? post("/api/v1/campaigns") : put("/api/v1/campaigns/100"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(method.equals("POST") ? 201 : 200));
    }

    @Test
    @DisplayName("GET /api/v1/campaigns/{id} returns 200 with detailed campaign")
    void getCampaignByIdSuccess() throws Exception {
        when(campaignService.getRequiredCampaignById(100L)).thenReturn(sampleCampaign);

        mockMvc.perform(get("/api/v1/campaigns/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.code", is("CMP-2026-AUTUMN")))
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    @DisplayName("GET /api/v1/campaigns/{id} returns 404 when campaign does not exist")
    void getCampaignByIdNotFound() throws Exception {
        when(campaignService.getRequiredCampaignById(999L))
                .thenThrow(new ResourceNotFoundException("Campaign not found with ID: 999"));

        mockMvc.perform(get("/api/v1/campaigns/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Campaign not found with ID: 999")));
    }

    @Test
    @DisplayName("GET /api/v1/campaigns returns 200 with summary list")
    void getCampaignsListSuccess() throws Exception {
        CampaignSummaryResponse summary = CampaignDtoMapper.toSummaryResponse(sampleCampaign, null);
        when(campaignService.getCampaignSummaries(null)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(100)))
                .andExpect(jsonPath("$[0].code", is("CMP-2026-AUTUMN")))
                .andExpect(jsonPath("$[0].vaccineTypeCode", is("VT-COVID-01")))
                .andExpect(jsonPath("$[0].targetGroupCode", is("TG-ELDERLY")));
    }

    @Test
    @DisplayName("GET /api/v1/campaigns?status=DRAFT filters campaigns by status")
    void getCampaignsWithStatusFilter() throws Exception {
        CampaignSummaryResponse summary = CampaignDtoMapper.toSummaryResponse(sampleCampaign, null);
        when(campaignService.getCampaignSummaries(CampaignStatus.DRAFT)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/campaigns").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("DRAFT")));

        verify(campaignService).getCampaignSummaries(CampaignStatus.DRAFT);
    }

    @Test
    @DisplayName("GET /api/v1/campaigns?status=INVALID returns 400 Bad Request")
    void getCampaignsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns").param("status", "NOT_A_VALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("GET /api/v1/campaigns/{id}/lifecycle-events returns 200 with audit events")
    void getLifecycleEventsSuccess() throws Exception {
        CampaignLifecycleEvent event = new CampaignLifecycleEvent(
                sampleCampaign,
                CampaignLifecycleChangeType.SUSPENDED,
                CampaignStatus.PUBLISHED,
                CampaignStatus.SUSPENDED,
                "Adverse weather condition",
                "OFFICER-01",
                Instant.parse("2026-08-10T12:00:00Z")
        );
        ReflectionTestUtils.setField(event, "id", 1L);

        when(campaignService.getCampaignLifecycleEvents(100L)).thenReturn(List.of(event));

        mockMvc.perform(get("/api/v1/campaigns/100/lifecycle-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].campaignId", is(100)))
                .andExpect(jsonPath("$[0].changeType", is("SUSPENDED")))
                .andExpect(jsonPath("$[0].reason", is("Adverse weather condition")))
                .andExpect(jsonPath("$[0].actorId", is("OFFICER-01")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/schedule transitions to SCHEDULED")
    void scheduleCampaignSuccess() throws Exception {
        ReflectionTestUtils.setField(sampleCampaign, "status", CampaignStatus.SCHEDULED);
        when(campaignService.scheduleCampaign(100L)).thenReturn(sampleCampaign);

        mockMvc.perform(post("/api/v1/campaigns/100/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.status", is("SCHEDULED")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/schedule returns 409 on lifecycle conflict")
    void scheduleCampaignConflict() throws Exception {
        when(campaignService.scheduleCampaign(100L))
                .thenThrow(new IllegalStateException("Cannot schedule campaign in ACTIVE status"));

        mockMvc.perform(post("/api/v1/campaigns/100/schedule"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Cannot schedule campaign in ACTIVE status")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/publish transitions to PUBLISHED")
    void publishCampaignSuccess() throws Exception {
        ReflectionTestUtils.setField(sampleCampaign, "status", CampaignStatus.PUBLISHED);
        when(campaignService.publishCampaign(100L)).thenReturn(sampleCampaign);

        mockMvc.perform(post("/api/v1/campaigns/100/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.status", is("PUBLISHED")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/activate transitions to ACTIVE")
    void activateCampaignSuccess() throws Exception {
        ReflectionTestUtils.setField(sampleCampaign, "status", CampaignStatus.ACTIVE);
        when(campaignService.activateCampaign(100L)).thenReturn(sampleCampaign);

        mockMvc.perform(post("/api/v1/campaigns/100/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/suspend transitions to SUSPENDED")
    void suspendCampaignSuccess() throws Exception {
        String payload = """
                {
                    "reason": "Adverse weather",
                    "actorId": "OFFICER-01"
                }
                """;
        ReflectionTestUtils.setField(sampleCampaign, "status", CampaignStatus.SUSPENDED);
        when(campaignService.suspendCampaign(100L, "Adverse weather", "OFFICER-01")).thenReturn(sampleCampaign);

        mockMvc.perform(post("/api/v1/campaigns/100/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.status", is("SUSPENDED")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/suspend returns 400 when reason or actorId is blank")
    void suspendCampaignValidationFailure() throws Exception {
        String invalidPayload = """
                {
                    "reason": "",
                    "actorId": ""
                }
                """;

        mockMvc.perform(post("/api/v1/campaigns/100/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/resume transitions to ACTIVE or PUBLISHED")
    void resumeCampaignSuccess() throws Exception {
        String payload = """
                {
                    "reason": "Weather cleared",
                    "actorId": "OFFICER-01"
                }
                """;
        ReflectionTestUtils.setField(sampleCampaign, "status", CampaignStatus.ACTIVE);
        when(campaignService.resumeCampaign(100L, "Weather cleared", "OFFICER-01")).thenReturn(sampleCampaign);

        mockMvc.perform(post("/api/v1/campaigns/100/resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/resume returns 400 when reason or actorId is blank")
    void resumeCampaignValidationFailure() throws Exception {
        String invalidPayload = """
                {
                    "reason": "",
                    "actorId": ""
                }
                """;

        mockMvc.perform(post("/api/v1/campaigns/100/resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns returns 400 on malformed JSON payload")
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed json: true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/schedule returns 404 when campaign does not exist")
    void scheduleCampaignNotFound() throws Exception {
        when(campaignService.scheduleCampaign(999L))
                .thenThrow(new ResourceNotFoundException("Campaign not found with ID: 999"));

        mockMvc.perform(post("/api/v1/campaigns/999/schedule"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Campaign not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/publish returns 404 when campaign does not exist")
    void publishCampaignNotFound() throws Exception {
        when(campaignService.publishCampaign(999L))
                .thenThrow(new ResourceNotFoundException("Campaign not found with ID: 999"));

        mockMvc.perform(post("/api/v1/campaigns/999/publish"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Campaign not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/activate returns 404 when campaign does not exist")
    void activateCampaignNotFound() throws Exception {
        when(campaignService.activateCampaign(999L))
                .thenThrow(new ResourceNotFoundException("Campaign not found with ID: 999"));

        mockMvc.perform(post("/api/v1/campaigns/999/activate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Campaign not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/suspend returns 404 when campaign does not exist")
    void suspendCampaignNotFound() throws Exception {
        String payload = """
                {
                    "reason": "Adverse weather",
                    "actorId": "OFFICER-01"
                }
                """;
        when(campaignService.suspendCampaign(eq(999L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Campaign not found with ID: 999"));

        mockMvc.perform(post("/api/v1/campaigns/999/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Campaign not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/resume returns 404 when campaign does not exist")
    void resumeCampaignNotFound() throws Exception {
        String payload = """
                {
                    "reason": "Weather cleared",
                    "actorId": "OFFICER-01"
                }
                """;
        when(campaignService.resumeCampaign(eq(999L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Campaign not found with ID: 999"));

        mockMvc.perform(post("/api/v1/campaigns/999/resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Campaign not found with ID: 999")));
    }
}
