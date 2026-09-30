package com.se2030.vaccinationportal.campaign.core.web;

import com.se2030.vaccinationportal.campaign.web.GlobalCampaignExceptionHandler;


import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.core.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.core.model.CampaignLocationTarget;
import com.se2030.vaccinationportal.campaign.questionnaire.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.core.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.core.model.LocationTargetType;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.center.service.CampaignCenterAssignmentService;
import com.se2030.vaccinationportal.campaign.core.service.CampaignDoseProgrammeService;
import com.se2030.vaccinationportal.campaign.core.service.CampaignLocationTargetService;
import com.se2030.vaccinationportal.campaign.questionnaire.service.CampaignQuestionnaireAssignmentService;
import com.se2030.vaccinationportal.campaign.core.service.VaccinationCampaignService;
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
class CampaignConfigurationControllerTests {

    private MockMvc mockMvc;

    @Mock private CampaignDoseProgrammeService doseProgrammeService;
    @Mock private CampaignQuestionnaireAssignmentService questionnaireAssignmentService;
    @Mock private CampaignCenterAssignmentService centerAssignmentService;
    @Mock private CampaignLocationTargetService locationTargetService;
    @Mock private VaccinationCampaignService campaignService;

    private VaccinationCampaign sampleCampaign;
    private VaccinationCenter sampleCenter;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CampaignConfigurationController(
                doseProgrammeService,
                questionnaireAssignmentService,
                centerAssignmentService,
                locationTargetService,
                campaignService
        ))
        .setControllerAdvice(new GlobalCampaignExceptionHandler())
        .setValidator(validator)
        .build();

        VaccineType vaccineType = new VaccineType("COVID-19", "mRNA");
        TargetGroup targetGroup = new TargetGroup("ELDERLY", "60+");
        sampleCampaign = new VaccinationCampaign(
                "National COVID Campaign",
                "COVID-2026",
                "Description",
                vaccineType,
                targetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-15T00:00:00Z"),
                Instant.parse("2026-12-30T23:59:59Z")
        );
        ReflectionTestUtils.setField(sampleCampaign, "id", 1L);

        sampleCenter = new VaccinationCenter("CTR-001", "Main Center", "123 Main St", "contact@test.com", CenterStatus.ACTIVE, 50, 100, "Asia/Colombo");
        ReflectionTestUtils.setField(sampleCenter, "id", 10L);
    }

    // ---------------------------------------------------------------------------------------------
    // 1. POST /api/v1/campaigns/{id}/dose-programmes
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("POST /dose-programmes creates initial dose programme and returns 201 with Location header")
    void createInitialDoseProgramme_success() throws Exception {
        CampaignDoseProgramme programme = new CampaignDoseProgramme(
                sampleCampaign, 1, DoseCategory.PRIMARY, "1,2", 28, false, 15, LocalDate.of(2026, 9, 1), null
        );
        ReflectionTestUtils.setField(programme, "id", 100L);

        when(doseProgrammeService.createInitialProgramme(
                eq(1L), eq(DoseCategory.PRIMARY), eq("1,2"), eq(28), eq(false), eq(15), eq(LocalDate.of(2026, 9, 1))
        )).thenReturn(programme);

        mockMvc.perform(post("/api/v1/campaigns/1/dose-programmes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doseCategory": "PRIMARY",
                                  "permittedDoseNumbers": "1,2",
                                  "minimumIntervalDays": 28,
                                  "previousEvidenceRequired": false,
                                  "expectedServiceMinutes": 15,
                                  "activeFrom": "2026-09-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/campaigns/1/dose-programmes"))
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.campaignId", is(1)))
                .andExpect(jsonPath("$.versionNumber", is(1)))
                .andExpect(jsonPath("$.doseCategory", is("PRIMARY")))
                .andExpect(jsonPath("$.permittedDoseNumbers", is("1,2")))
                .andExpect(jsonPath("$.minimumIntervalDays", is(28)));
    }

    @Test
    @DisplayName("POST /dose-programmes returns 400 when validation fails")
    void createInitialDoseProgramme_validationFailure() throws Exception {
        mockMvc.perform(post("/api/v1/campaigns/1/dose-programmes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doseCategory": null,
                                  "permittedDoseNumbers": "",
                                  "minimumIntervalDays": -1,
                                  "previousEvidenceRequired": false,
                                  "expectedServiceMinutes": null,
                                  "activeFrom": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    @Test
    @DisplayName("POST /dose-programmes returns 404 when campaign not found")
    void createInitialDoseProgramme_campaignNotFound() throws Exception {
        when(doseProgrammeService.createInitialProgramme(any(), any(), any(), any(Integer.class), any(Boolean.class), any(), any()))
                .thenThrow(new ResourceNotFoundException("Campaign not found with ID: 999"));

        mockMvc.perform(post("/api/v1/campaigns/999/dose-programmes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doseCategory": "PRIMARY",
                                  "permittedDoseNumbers": "1,2",
                                  "minimumIntervalDays": 28,
                                  "previousEvidenceRequired": false,
                                  "expectedServiceMinutes": 15,
                                  "activeFrom": "2026-09-01"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Campaign not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /dose-programmes returns 409 when initial programme already exists")
    void createInitialDoseProgramme_alreadyExists() throws Exception {
        when(doseProgrammeService.createInitialProgramme(any(), any(), any(), any(Integer.class), any(Boolean.class), any(), any()))
                .thenThrow(new IllegalStateException("Initial dose programme already exists for campaign ID: 1"));

        mockMvc.perform(post("/api/v1/campaigns/1/dose-programmes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doseCategory": "PRIMARY",
                                  "permittedDoseNumbers": "1,2",
                                  "minimumIntervalDays": 28,
                                  "previousEvidenceRequired": false,
                                  "expectedServiceMinutes": 15,
                                  "activeFrom": "2026-09-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Initial dose programme already exists for campaign ID: 1")));
    }

    // ---------------------------------------------------------------------------------------------
    // 2. POST /api/v1/campaigns/{id}/dose-programmes/versions
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("POST /dose-programmes/versions creates new version and returns 201")
    void createNewDoseProgrammeVersion_success() throws Exception {
        CampaignDoseProgramme version2 = new CampaignDoseProgramme(
                sampleCampaign, 2, DoseCategory.BOOSTER, "3", 90, true, 20, LocalDate.of(2026, 11, 1), null
        );
        ReflectionTestUtils.setField(version2, "id", 101L);

        when(doseProgrammeService.createNewVersion(
                eq(1L), eq(DoseCategory.BOOSTER), eq("3"), eq(90), eq(true), eq(20), eq(LocalDate.of(2026, 11, 1))
        )).thenReturn(version2);

        mockMvc.perform(post("/api/v1/campaigns/1/dose-programmes/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doseCategory": "BOOSTER",
                                  "permittedDoseNumbers": "3",
                                  "minimumIntervalDays": 90,
                                  "previousEvidenceRequired": true,
                                  "expectedServiceMinutes": 20,
                                  "effectiveStartDate": "2026-11-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/campaigns/1/dose-programmes"))
                .andExpect(jsonPath("$.id", is(101)))
                .andExpect(jsonPath("$.versionNumber", is(2)))
                .andExpect(jsonPath("$.doseCategory", is("BOOSTER")));
    }

    // ---------------------------------------------------------------------------------------------
    // 3. GET /api/v1/campaigns/{id}/dose-programmes
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("GET /dose-programmes returns history list ordered by version descending")
    void getDoseProgrammeHistory_success() throws Exception {
        when(campaignService.getRequiredCampaignById(1L)).thenReturn(sampleCampaign);

        CampaignDoseProgramme v2 = new CampaignDoseProgramme(
                sampleCampaign, 2, DoseCategory.BOOSTER, "3", 90, true, 20, LocalDate.of(2026, 11, 1), null
        );
        ReflectionTestUtils.setField(v2, "id", 101L);

        CampaignDoseProgramme v1 = new CampaignDoseProgramme(
                sampleCampaign, 1, DoseCategory.PRIMARY, "1,2", 28, false, 15, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1)
        );
        ReflectionTestUtils.setField(v1, "id", 100L);

        when(doseProgrammeService.getProgrammeHistory(1L)).thenReturn(List.of(v2, v1));

        mockMvc.perform(get("/api/v1/campaigns/1/dose-programmes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].versionNumber", is(2)))
                .andExpect(jsonPath("$[1].versionNumber", is(1)));
    }

    // ---------------------------------------------------------------------------------------------
    // 4. POST /api/v1/campaigns/{id}/questionnaire-assignments
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("POST /questionnaire-assignments assigns questionnaire version and returns 201")
    void assignQuestionnaireVersion_success() throws Exception {
        CampaignQuestionnaireAssignment assignment = new CampaignQuestionnaireAssignment(
                sampleCampaign, 55L, LocalDate.of(2026, 9, 1), null
        );
        ReflectionTestUtils.setField(assignment, "id", 200L);

        when(questionnaireAssignmentService.assignQuestionnaireVersion(1L, 55L, LocalDate.of(2026, 9, 1)))
                .thenReturn(assignment);

        mockMvc.perform(post("/api/v1/campaigns/1/questionnaire-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionnaireVersionId": 55,
                                  "effectiveStartDate": "2026-09-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/campaigns/1/questionnaire-assignments"))
                .andExpect(jsonPath("$.id", is(200)))
                .andExpect(jsonPath("$.questionnaireVersionId", is(55)));
    }

    // ---------------------------------------------------------------------------------------------
    // 5. GET /api/v1/campaigns/{id}/questionnaire-assignments
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("GET /questionnaire-assignments returns assignment history")
    void getQuestionnaireAssignmentHistory_success() throws Exception {
        when(campaignService.getRequiredCampaignById(1L)).thenReturn(sampleCampaign);

        CampaignQuestionnaireAssignment a = new CampaignQuestionnaireAssignment(
                sampleCampaign, 55L, LocalDate.of(2026, 9, 1), null
        );
        ReflectionTestUtils.setField(a, "id", 200L);

        when(questionnaireAssignmentService.getAssignmentHistory(1L)).thenReturn(List.of(a));

        mockMvc.perform(get("/api/v1/campaigns/1/questionnaire-assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].questionnaireVersionId", is(55)));
    }

    // ---------------------------------------------------------------------------------------------
    // 6. POST /api/v1/campaigns/{id}/center-assignments
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("POST /center-assignments creates assignment and returns 201 with item Location header")
    void createCenterAssignment_success() throws Exception {
        CampaignCenterAssignment assignment = new CampaignCenterAssignment(
                sampleCampaign, sampleCenter, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31)
        );
        ReflectionTestUtils.setField(assignment, "id", 300L);

        when(centerAssignmentService.createAssignment(1L, 10L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(assignment);

        mockMvc.perform(post("/api/v1/campaigns/1/center-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "centerId": 10,
                                  "activeFrom": "2026-09-01",
                                  "activeTo": "2026-12-31"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/campaigns/1/center-assignments/300"))
                .andExpect(jsonPath("$.id", is(300)))
                .andExpect(jsonPath("$.centerId", is(10)))
                .andExpect(jsonPath("$.centerCode", is("CTR-001")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    // ---------------------------------------------------------------------------------------------
    // 7. GET /api/v1/campaigns/{id}/center-assignments
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("GET /center-assignments returns list of center assignments")
    void getCenterAssignments_success() throws Exception {
        when(campaignService.getRequiredCampaignById(1L)).thenReturn(sampleCampaign);

        CampaignCenterAssignment assignment = new CampaignCenterAssignment(
                sampleCampaign, sampleCenter, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31)
        );
        ReflectionTestUtils.setField(assignment, "id", 300L);

        when(centerAssignmentService.getAssignmentsForCampaign(1L)).thenReturn(List.of(assignment));

        mockMvc.perform(get("/api/v1/campaigns/1/center-assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].centerCode", is("CTR-001")));
    }

    // ---------------------------------------------------------------------------------------------
    // 8. PUT /api/v1/campaigns/{id}/center-assignments/{assignmentId}
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("PUT /center-assignments/{assignmentId} updates future assignment")
    void updateFutureCenterAssignment_success() throws Exception {
        CampaignCenterAssignment assignment = new CampaignCenterAssignment(
                sampleCampaign, sampleCenter, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 12, 15)
        );
        ReflectionTestUtils.setField(assignment, "id", 300L);

        when(centerAssignmentService.updateFutureAssignment(1L, 300L, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 12, 15)))
                .thenReturn(assignment);

        mockMvc.perform(put("/api/v1/campaigns/1/center-assignments/300")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activeFrom": "2026-09-15",
                                  "activeTo": "2026-12-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(300)))
                .andExpect(jsonPath("$.activeFrom", is("2026-09-15")));
    }

    // ---------------------------------------------------------------------------------------------
    // 9. POST /api/v1/campaigns/{id}/center-assignments/{assignmentId}/end-date
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("POST /center-assignments/{assignmentId}/end-date ends effective assignment early")
    void endCenterAssignment_success() throws Exception {
        CampaignCenterAssignment assignment = new CampaignCenterAssignment(
                sampleCampaign, sampleCenter, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31)
        );
        ReflectionTestUtils.setField(assignment, "id", 300L);

        when(centerAssignmentService.endAssignment(1L, 300L, LocalDate.of(2026, 10, 31)))
                .thenReturn(assignment);

        mockMvc.perform(post("/api/v1/campaigns/1/center-assignments/300/end-date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activeTo": "2026-10-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(300)))
                .andExpect(jsonPath("$.activeTo", is("2026-10-31")));
    }

    // ---------------------------------------------------------------------------------------------
    // 10. POST /api/v1/campaigns/{id}/center-assignments/{assignmentId}/cancel
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("POST /center-assignments/{assignmentId}/cancel cancels future assignment")
    void cancelFutureCenterAssignment_success() throws Exception {
        CampaignCenterAssignment assignment = new CampaignCenterAssignment(
                sampleCampaign, sampleCenter, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31)
        );
        ReflectionTestUtils.setField(assignment, "id", 300L);
        assignment.cancel();

        when(centerAssignmentService.cancelFutureAssignment(1L, 300L)).thenReturn(assignment);

        mockMvc.perform(post("/api/v1/campaigns/1/center-assignments/300/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(300)))
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    // ---------------------------------------------------------------------------------------------
    // 11. POST /api/v1/campaigns/{id}/location-targets
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("POST /location-targets adds location target and returns 201 with Location header")
    void addLocationTarget_success() throws Exception {
        CampaignLocationTarget target = new CampaignLocationTarget(
                sampleCampaign, LocationTargetType.DISTRICT, "COLOMBO"
        );
        ReflectionTestUtils.setField(target, "id", 400L);

        when(locationTargetService.addTarget(1L, LocationTargetType.DISTRICT, "COLOMBO")).thenReturn(target);

        mockMvc.perform(post("/api/v1/campaigns/1/location-targets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "DISTRICT",
                                  "targetCode": "COLOMBO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/campaigns/1/location-targets"))
                .andExpect(jsonPath("$.id", is(400)))
                .andExpect(jsonPath("$.targetType", is("DISTRICT")))
                .andExpect(jsonPath("$.targetCode", is("COLOMBO")));
    }

    // ---------------------------------------------------------------------------------------------
    // 12. GET /api/v1/campaigns/{id}/location-targets
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("GET /location-targets returns all targets when type is not specified")
    void getLocationTargets_all_success() throws Exception {
        when(campaignService.getRequiredCampaignById(1L)).thenReturn(sampleCampaign);

        CampaignLocationTarget t1 = new CampaignLocationTarget(sampleCampaign, LocationTargetType.PROVINCE, "WP");
        ReflectionTestUtils.setField(t1, "id", 401L);
        CampaignLocationTarget t2 = new CampaignLocationTarget(sampleCampaign, LocationTargetType.DISTRICT, "COLOMBO");
        ReflectionTestUtils.setField(t2, "id", 402L);

        when(locationTargetService.getTargetsForCampaign(1L)).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/api/v1/campaigns/1/location-targets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].targetCode", is("WP")))
                .andExpect(jsonPath("$[1].targetCode", is("COLOMBO")));
    }

    @Test
    @DisplayName("GET /location-targets with ?type=DISTRICT filters targets")
    void getLocationTargets_filtered_success() throws Exception {
        when(campaignService.getRequiredCampaignById(1L)).thenReturn(sampleCampaign);

        CampaignLocationTarget t2 = new CampaignLocationTarget(sampleCampaign, LocationTargetType.DISTRICT, "COLOMBO");
        ReflectionTestUtils.setField(t2, "id", 402L);

        when(locationTargetService.getTargetsForCampaignAndType(1L, LocationTargetType.DISTRICT)).thenReturn(List.of(t2));

        mockMvc.perform(get("/api/v1/campaigns/1/location-targets").param("type", "DISTRICT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetType", is("DISTRICT")))
                .andExpect(jsonPath("$[0].targetCode", is("COLOMBO")));
    }
}
