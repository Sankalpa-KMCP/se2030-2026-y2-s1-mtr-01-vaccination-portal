package com.se2030.vaccinationportal.campaign.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.dto.response.SessionOperationalReadinessResponse;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.OperationalResourceService;
import com.se2030.vaccinationportal.campaign.service.OperationalStaffRosterService;
import com.se2030.vaccinationportal.campaign.service.SessionCapacityPlanService;
import com.se2030.vaccinationportal.campaign.service.SessionOperationalReadinessService;
import com.se2030.vaccinationportal.campaign.service.SessionResourceAssignmentService;
import com.se2030.vaccinationportal.campaign.service.StaffAssignmentService;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SessionReadinessQueryIntegrationTests {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired private SessionOperationalReadinessService readinessService;
    @Autowired private SessionCapacityPlanService capacityPlanService;
    @Autowired private SessionResourceAssignmentService resourceAssignmentService;
    @Autowired private StaffAssignmentService staffAssignmentService;
    @Autowired private OperationalResourceService resourceService;
    @Autowired private OperationalStaffRosterService rosterService;

    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignCenterAssignmentRepository centerAssignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private OperationalStaffRosterRepository rosterRepository;
    @Autowired private SessionResourceAssignmentRepository sessionResourceAssignmentRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private SessionCapacityPlanRepository capacityPlanRepository;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private EntityManager entityManager;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private VaccinationSession session;
    private OperationalResource stationResource;
    private OperationalStaffRoster vaccinatorRoster;

    private final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);
    private final Instant SESSION_START = Instant.parse("2026-10-15T09:00:00Z");
    private final Instant SESSION_END = Instant.parse("2026-10-15T12:00:00Z"); // 180 min
    private final Instant BOOKING_CLOSE = Instant.parse("2026-10-15T08:00:00Z");

    @BeforeEach
    void setUp() {
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-RDY-INT", "Readiness Test Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.saveAndFlush(new TargetGroup("TG-RDY-INT", "Readiness Test Target Group"));

        center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-RDY-INT", "Readiness Test Center", "123 Health Ave", "0112345678",
                CenterStatus.ACTIVE, 50, 100, "UTC"
        ));

        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center, dow, LocalTime.of(8, 0), LocalTime.of(18, 0), CAMPAIGN_START, CAMPAIGN_END
            ));
        }

        campaign = campaignRepository.saveAndFlush(new VaccinationCampaign(
                "CMP-RDY-INT", "Readiness Test Campaign", "Description", vaccine, targetGroup,
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-31T23:59:59Z")
        ));
        campaign.publish();
        campaign = campaignRepository.saveAndFlush(campaign);

        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)
        ));

        doseProgrammeRepository.saveAndFlush(new CampaignDoseProgramme(
                campaign, 1, DoseCategory.PRIMARY, "1", 0, false, 10, CAMPAIGN_START, null
        ));

        session = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, SESSION_START, SESSION_END, BOOKING_CLOSE, "UTC"
        ));

        stationResource = resourceService.createResource(
                center.getId(), "STATION-01", ResourceType.VACCINATION_STATION, "Station 1", OperationalResourceStatus.ACTIVE
        );

        vaccinatorRoster = rosterService.registerStaffMember(
                center.getId(), 6001L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE
        );
    }

    private void configureFullReadiness() {
        resourceAssignmentService.createAssignment(
                session.getId(), stationResource.getId(), SESSION_START, SESSION_END
        );
        staffAssignmentService.createAssignment(
                session.getId(), vaccinatorRoster.getId(), StaffRole.VACCINATOR, SESSION_START, SESSION_END
        );
        capacityPlanService.createCapacityPlan(
                session.getId(),
                new CreateSessionCapacityPlanRequest(1, 18, 50, 15, 0.0, null, "Planner")
        );
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/readiness for fully ready session returns 200 OK with field-for-field evaluator parity")
    void getSessionReadiness_readySession_returns200WithEvaluatorParity() throws Exception {
        configureFullReadiness();

        // 1. Direct authoritative service evaluation
        SessionOperationalReadinessService.OperationalReadinessResult directResult =
                readinessService.evaluateReadiness(session.getId());
        assertThat(directResult.ready()).isTrue();
        assertThat(directResult.violations()).isEmpty();

        // 2. REST query evaluation
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(session.getId().intValue())))
                .andExpect(jsonPath("$.ready", is(true)))
                .andExpect(jsonPath("$.violations", hasSize(0)))
                .andExpect(jsonPath("$.assignedVaccinators", is(1)))
                .andExpect(jsonPath("$.availableStations", is(1)))
                .andExpect(jsonPath("$.effectiveStations", is(1)))
                .andExpect(jsonPath("$.recommendedThroughput", is(18)))
                .andExpect(jsonPath("$.approvedCapacity", is(18)))
                .andExpect(jsonPath("$.revision", is(1)))
                .andReturn();

        SessionOperationalReadinessResponse response = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                SessionOperationalReadinessResponse.class
        );

        // 3. Field-for-field parity assertion
        assertThat(response.sessionId()).isEqualTo(directResult.sessionId());
        assertThat(response.ready()).isEqualTo(directResult.ready());
        assertThat(response.violations()).isEqualTo(directResult.violations());
        assertThat(response.assignedVaccinators()).isEqualTo(directResult.assignedVaccinators());
        assertThat(response.availableStations()).isEqualTo(directResult.availableStations());
        assertThat(response.effectiveStations()).isEqualTo(directResult.effectiveStations());
        assertThat(response.recommendedThroughput()).isEqualTo(directResult.recommendedThroughput());
        assertThat(response.approvedCapacity()).isEqualTo(directResult.approvedCapacity());
        assertThat(response.revision()).isEqualTo(directResult.revision());
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/readiness for non-ready DRAFT session returns 200 OK (NOT 409) with all evaluator violations")
    void getSessionReadiness_nonReadyDraftSession_returns200WithViolations() throws Exception {
        // Session has zero resources, zero staff, no capacity plan
        SessionOperationalReadinessService.OperationalReadinessResult directResult =
                readinessService.evaluateReadiness(session.getId());
        assertThat(directResult.ready()).isFalse();
        assertThat(directResult.violations()).isNotEmpty();

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(session.getId().intValue())))
                .andExpect(jsonPath("$.ready", is(false)))
                .andExpect(jsonPath("$.violations", hasItem("No active vaccination stations assigned")))
                .andExpect(jsonPath("$.violations", hasItem("No active vaccinators assigned")))
                .andExpect(jsonPath("$.violations", hasItem("No approved capacity plan exists")))
                .andExpect(jsonPath("$.assignedVaccinators", is(0)))
                .andExpect(jsonPath("$.availableStations", is(0)))
                .andExpect(jsonPath("$.effectiveStations", is(0)))
                .andExpect(jsonPath("$.recommendedThroughput", is(0)))
                .andReturn();

        SessionOperationalReadinessResponse response = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                SessionOperationalReadinessResponse.class
        );

        assertThat(response.sessionId()).isEqualTo(directResult.sessionId());
        assertThat(response.ready()).isEqualTo(directResult.ready());
        assertThat(response.violations()).isEqualTo(directResult.violations());
        assertThat(response.assignedVaccinators()).isEqualTo(directResult.assignedVaccinators());
        assertThat(response.availableStations()).isEqualTo(directResult.availableStations());
        assertThat(response.effectiveStations()).isEqualTo(directResult.effectiveStations());
        assertThat(response.recommendedThroughput()).isEqualTo(directResult.recommendedThroughput());
        assertThat(response.approvedCapacity()).isEqualTo(directResult.approvedCapacity());
        assertThat(response.revision()).isEqualTo(directResult.revision());
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/readiness for non-DRAFT session returns 200 OK diagnostic response per evaluator")
    void getSessionReadiness_nonDraftSession_returns200Diagnostic() throws Exception {
        configureFullReadiness();

        // Mutate session to SCHEDULED
        session = sessionRepository.findById(session.getId()).orElseThrow();
        session.schedule();
        session = sessionRepository.saveAndFlush(session);
        assertThat(session.getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.SCHEDULED);

        mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(session.getId().intValue())))
                .andExpect(jsonPath("$.ready", is(false)))
                .andExpect(jsonPath("$.violations", hasItem("Session is not in DRAFT status (current: SCHEDULED)")));
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/readiness for unknown session returns 404 Not Found ApiErrorResponse")
    void getSessionReadiness_unknownSession_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/999999/readiness"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination session not found with ID: 999999")));
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/readiness is side-effect-free and does not mutate any domain state")
    void getSessionReadiness_sideEffectFreeContract() throws Exception {
        configureFullReadiness();

        // Capture initial state
        VaccinationSession initialSession = sessionRepository.findById(session.getId()).orElseThrow();
        SessionLifecycleStatus initialStatus = initialSession.getLifecycleStatus();
        Instant initialCreatedAt = initialSession.getCreatedAt();
        long initialChangeEvents = sessionChangeEventRepository.count();
        long initialCapacityPlans = capacityPlanRepository.count();
        long initialResources = sessionResourceAssignmentRepository.count();
        long initialStaff = staffAssignmentRepository.count();

        // Perform GET readiness query
        mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready", is(true)));

        entityManager.clear();

        // Verify state is 100% unchanged
        VaccinationSession postSession = sessionRepository.findById(session.getId()).orElseThrow();
        assertThat(postSession.getLifecycleStatus()).isEqualTo(initialStatus);
        assertThat(postSession.getCreatedAt()).isEqualTo(initialCreatedAt);
        assertThat(sessionChangeEventRepository.count()).isEqualTo(initialChangeEvents);
        assertThat(capacityPlanRepository.count()).isEqualTo(initialCapacityPlans);
        assertThat(sessionResourceAssignmentRepository.count()).isEqualTo(initialResources);
        assertThat(staffAssignmentRepository.count()).isEqualTo(initialStaff);
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{id}/readiness repeated calls return stable and identical results")
    void getSessionReadiness_repeatedQueryStability() throws Exception {
        configureFullReadiness();

        MvcResult firstCall = mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/readiness"))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult secondCall = mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/readiness"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(firstCall.getResponse().getContentAsString())
                .isEqualTo(secondCall.getResponse().getContentAsString());
    }
}
