package com.se2030.vaccinationportal.campaign.session.recurrence;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.core.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.core.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.center.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.center.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.core.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.center.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.session.recurrence.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.session.recurrence.service.SessionOccurrenceGenerationService;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SessionSeriesChangeSecurityIntegrationTests {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private SessionOccurrenceGenerationService occurrenceGenerationService;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private SessionRecurrenceRule rule;

    private static final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);
    private static final Instant CAMPAIGN_REG_START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant CAMPAIGN_REG_END = Instant.parse("2026-10-30T00:00:00Z");

    @BeforeEach
    void setUp() {
        VaccineType vaccine = vaccineTypeRepository.save(new VaccineType("VAX-SERIES-SEC", "Series Security Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-SERIES-SEC", "Series Security Target"));

        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-SER-SEC-01", "Series Security Campaign", "Security integration testing for series",
                vaccine, targetGroup,
                CAMPAIGN_START, CAMPAIGN_END,
                CAMPAIGN_REG_START, CAMPAIGN_REG_END,
                CampaignStatus.DRAFT
        ));

        doseProgrammeRepository.save(new CampaignDoseProgramme(
                campaign, 1, DoseCategory.PRIMARY, "1", 21, false, 10,
                CAMPAIGN_START, CAMPAIGN_END
        ));

        center = centerRepository.save(new VaccinationCenter(
                "CTR-SER-SEC-01", "Series Center", "100 Security Road", "+94110000000",
                CenterStatus.ACTIVE, 100, 30, "UTC"
        ));

        assignmentRepository.save(new CampaignCenterAssignment(campaign, center, CAMPAIGN_START, CAMPAIGN_END));

        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.save(new OperatingHours(center, day, LocalTime.of(8, 0), LocalTime.of(18, 0), CAMPAIGN_START));
        }

        rule = ruleRepository.save(new SessionRecurrenceRule(
                campaign,
                center,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 16),
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                "UTC"
        ));

        occurrenceGenerationService.generateOccurrences(rule.getId());
    }

    private String createBearerToken(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    private String obtainSeriesCancellationPreviewToken(Long ruleId) throws Exception {
        String authHeader = createBearerToken(1L, "manager@health.gov", List.of("ROLE_CENTER_MANAGER"));
        MvcResult result = mockMvc.perform(post("/api/v1/recurrence-rules/" + ruleId + "/cancellation-preview")
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("previewToken").asText();
    }

    private String obtainSeriesChangePreviewToken(Long ruleId, String payload) throws Exception {
        String authHeader = createBearerToken(1L, "manager@health.gov", List.of("ROLE_CENTER_MANAGER"));
        MvcResult result = mockMvc.perform(post("/api/v1/recurrence-rules/" + ruleId + "/change-preview")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("previewToken").asText();
    }

    // =========================================================================
    // 1. ANONYMOUS CONFIRMATION -> 401 UNAUTHORIZED
    // =========================================================================

    @Test
    @DisplayName("Anonymous POST /api/v1/recurrence-rules/{id}/change returns 401 Unauthorized")
    void anonymousSeriesChangeReturns401() throws Exception {
        String previewPayload = """
                {
                    "weekdays": ["MONDAY", "WEDNESDAY"],
                    "startDate": "2026-10-05",
                    "endDate": "2026-10-16",
                    "startTime": "10:00:00",
                    "endTime": "13:00:00",
                    "timezone": "UTC"
                }
                """;
        String token = obtainSeriesChangePreviewToken(rule.getId(), previewPayload);

        String confirmPayload = String.format("""
                {
                    "weekdays": ["MONDAY", "WEDNESDAY"],
                    "startDate": "2026-10-05",
                    "endDate": "2026-10-16",
                    "startTime": "10:00:00",
                    "endTime": "13:00:00",
                    "timezone": "UTC",
                    "previewToken": "%s",
                    "reason": "Shift schedule adjustment"
                }
                """, token);

        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmPayload))
                .andExpect(status().isUnauthorized());

        assertEquals(0, sessionChangeEventRepository.findAll().size());
    }

    @Test
    @DisplayName("Anonymous POST /api/v1/recurrence-rules/{id}/cancel returns 401 Unauthorized")
    void anonymousSeriesCancelReturns401() throws Exception {
        String token = obtainSeriesCancellationPreviewToken(rule.getId());

        String payload = String.format("""
                {
                    "previewToken": "%s",
                    "reason": "Severe weather emergency"
                }
                """, token);

        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        assertEquals(0, sessionChangeEventRepository.findAll().size());
    }

    // =========================================================================
    // 2. AUTHENTICATED SERIES CHANGE & SPOOFING / OMISSION PROOFS
    // =========================================================================

    @Test
    @DisplayName("Authenticated series change persists principal userId string as actorId across all sessions")
    void authenticatedSeriesChangePersistsPrincipalUserIdAcrossSessions() throws Exception {
        String previewPayload = """
                {
                    "weekdays": ["MONDAY", "WEDNESDAY"],
                    "startDate": "2026-10-05",
                    "endDate": "2026-10-16",
                    "startTime": "10:00:00",
                    "endTime": "13:00:00",
                    "timezone": "UTC"
                }
                """;
        String token = obtainSeriesChangePreviewToken(rule.getId(), previewPayload);
        String authHeader = createBearerToken(42L, "officer42@health.gov", List.of("ROLE_CENTER_MANAGER"));

        String confirmPayload = String.format("""
                {
                    "weekdays": ["MONDAY", "WEDNESDAY"],
                    "startDate": "2026-10-05",
                    "endDate": "2026-10-16",
                    "startTime": "10:00:00",
                    "endTime": "13:00:00",
                    "timezone": "UTC",
                    "previewToken": "%s",
                    "reason": "Staff shift optimization"
                }
                """, token);

        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/change")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rule.id", is(rule.getId().intValue())));

        List<SessionChangeEvent> events = sessionChangeEventRepository.findAll();
        assertTrue(events.size() > 0, "Events should be persisted for changed occurrences");
        for (SessionChangeEvent event : events) {
            assertEquals("42", event.getActorId(), "Every event must use the trusted principal user ID");
            assertEquals("Staff shift optimization", event.getReason());
        }
    }

    @Test
    @DisplayName("Authenticated series change ignores spoofed body actorId and persists principal userId string")
    void authenticatedSeriesChangeIgnoresSpoofedActorId() throws Exception {
        String previewPayload = """
                {
                    "weekdays": ["MONDAY", "WEDNESDAY"],
                    "startDate": "2026-10-05",
                    "endDate": "2026-10-16",
                    "startTime": "10:00:00",
                    "endTime": "13:00:00",
                    "timezone": "UTC"
                }
                """;
        String token = obtainSeriesChangePreviewToken(rule.getId(), previewPayload);
        String authHeader = createBearerToken(42L, "officer42@health.gov", List.of("ROLE_CENTER_MANAGER"));

        String confirmPayload = String.format("""
                {
                    "weekdays": ["MONDAY", "WEDNESDAY"],
                    "startDate": "2026-10-05",
                    "endDate": "2026-10-16",
                    "startTime": "10:00:00",
                    "endTime": "13:00:00",
                    "timezone": "UTC",
                    "previewToken": "%s",
                    "reason": "Staff shift optimization",
                    "actorId": "spoofed-attacker-999"
                }
                """, token);

        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/change")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmPayload))
                .andExpect(status().isOk());

        List<SessionChangeEvent> events = sessionChangeEventRepository.findAll();
        assertTrue(events.size() > 0);
        for (SessionChangeEvent event : events) {
            assertEquals("42", event.getActorId(), "Spoofed actorId must be ignored in favor of principal userId");
        }
    }

    // =========================================================================
    // 3. AUTHENTICATED SERIES CANCELLATION & SPOOFING / OMISSION PROOFS
    // =========================================================================

    @Test
    @DisplayName("Authenticated series cancellation persists principal userId string across all sessions")
    void authenticatedSeriesCancelPersistsPrincipalUserIdAcrossSessions() throws Exception {
        String token = obtainSeriesCancellationPreviewToken(rule.getId());
        String authHeader = createBearerToken(88L, "director88@health.gov", List.of("ROLE_CENTER_MANAGER"));

        String payload = String.format("""
                {
                    "previewToken": "%s",
                    "reason": "Center maintenance closure"
                }
                """, token);

        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rule.id", is(rule.getId().intValue())))
                .andExpect(jsonPath("$.rule.status", is("CANCELLED")));

        List<SessionChangeEvent> events = sessionChangeEventRepository.findAll();
        assertTrue(events.size() > 0, "Events should be persisted for cancelled occurrences");
        for (SessionChangeEvent event : events) {
            assertEquals(SessionChangeType.SERIES_CANCELLED, event.getChangeType());
            assertEquals("88", event.getActorId(), "Every cancellation event must use the trusted principal user ID");
            assertEquals("Center maintenance closure", event.getReason());
        }
    }

    @Test
    @DisplayName("Authenticated series cancellation ignores spoofed body actorId and persists principal userId string")
    void authenticatedSeriesCancelIgnoresSpoofedActorId() throws Exception {
        String token = obtainSeriesCancellationPreviewToken(rule.getId());
        String authHeader = createBearerToken(88L, "director88@health.gov", List.of("ROLE_CENTER_MANAGER"));

        String payload = String.format("""
                {
                    "previewToken": "%s",
                    "reason": "Center maintenance closure",
                    "actorId": "admin@example.com"
                }
                """, token);

        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        List<SessionChangeEvent> events = sessionChangeEventRepository.findAll();
        assertTrue(events.size() > 0);
        for (SessionChangeEvent event : events) {
            assertEquals("88", event.getActorId(), "Spoofed actorId must be ignored in favor of principal userId");
        }
    }

    // =========================================================================
    // 4. PREVIEW & MANAGEMENT ENDPOINTS REQUIRE AUTHORIZATION
    // =========================================================================

    @Test
    @DisplayName("Anonymous POST change-preview is denied 403")
    void anonymousChangePreviewDenied() throws Exception {
        String previewPayload = """
                {
                    "weekdays": ["MONDAY"],
                    "startDate": "2026-10-05",
                    "endDate": "2026-10-16",
                    "startTime": "10:00:00",
                    "endTime": "13:00:00",
                    "timezone": "UTC"
                }
                """;

        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/change-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(previewPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authorized POST change-preview succeeds for ROLE_CENTER_MANAGER")
    void authorizedChangePreviewSucceeds() throws Exception {
        String authHeader = createBearerToken(1L, "manager@health.gov", List.of("ROLE_CENTER_MANAGER"));
        String previewPayload = """
                {
                    "weekdays": ["MONDAY"],
                    "startDate": "2026-10-05",
                    "endDate": "2026-10-16",
                    "startTime": "10:00:00",
                    "endTime": "13:00:00",
                    "timezone": "UTC"
                }
                """;

        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/change-preview")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(previewPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId", is(rule.getId().intValue())));
    }

    @Test
    @DisplayName("Anonymous POST cancellation-preview is denied 403")
    void anonymousCancellationPreviewDenied() throws Exception {
        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/cancellation-preview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authorized POST cancellation-preview succeeds for ROLE_CENTER_MANAGER")
    void authorizedCancellationPreviewSucceeds() throws Exception {
        String authHeader = createBearerToken(1L, "manager@health.gov", List.of("ROLE_CENTER_MANAGER"));
        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/cancellation-preview")
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId", is(rule.getId().intValue())));
    }

    @Test
    @DisplayName("Anonymous POST /api/v1/recurrence-rules (create rule) is denied 403")
    void anonymousCreateRuleDenied() throws Exception {
        String createPayload = String.format("""
                {
                    "campaignId": %d,
                    "centerId": %d,
                    "weekdays": ["TUESDAY", "THURSDAY"],
                    "startDate": "2026-10-20",
                    "endDate": "2026-10-30",
                    "startTime": "09:00:00",
                    "endTime": "12:00:00",
                    "timezone": "UTC"
                }
                """, campaign.getId(), center.getId());

        mockMvc.perform(post("/api/v1/recurrence-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authorized POST /api/v1/recurrence-rules (create rule) succeeds for ROLE_CENTER_MANAGER")
    void authorizedCreateRuleSucceeds() throws Exception {
        String authHeader = createBearerToken(1L, "manager@health.gov", List.of("ROLE_CENTER_MANAGER"));
        String createPayload = String.format("""
                {
                    "campaignId": %d,
                    "centerId": %d,
                    "weekdays": ["TUESDAY", "THURSDAY"],
                    "startDate": "2026-10-20",
                    "endDate": "2026-10-30",
                    "startTime": "09:00:00",
                    "endTime": "12:00:00",
                    "timezone": "UTC"
                }
                """, campaign.getId(), center.getId());

        mockMvc.perform(post("/api/v1/recurrence-rules")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Anonymous PUT /api/v1/recurrence-rules/{id} (update rule) is denied 403")
    void anonymousUpdateRuleDenied() throws Exception {
        SessionRecurrenceRule ungeneratedRule = ruleRepository.save(new SessionRecurrenceRule(
                campaign,
                center,
                EnumSet.of(DayOfWeek.TUESDAY),
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 27),
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                "UTC"
        ));

        String updatePayload = """
                {
                    "weekdays": ["TUESDAY", "THURSDAY"],
                    "startDate": "2026-10-20",
                    "endDate": "2026-10-27",
                    "startTime": "09:00:00",
                    "endTime": "12:00:00",
                    "timezone": "UTC"
                }
                """;

        mockMvc.perform(put("/api/v1/recurrence-rules/" + ungeneratedRule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous POST /api/v1/recurrence-rules/{id}/generate is denied 403")
    void anonymousGenerateDenied() throws Exception {
        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/generate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authorized POST /api/v1/recurrence-rules/{id}/generate succeeds for ROLE_CENTER_MANAGER")
    void authorizedGenerateSucceeds() throws Exception {
        String authHeader = createBearerToken(1L, "manager@health.gov", List.of("ROLE_CENTER_MANAGER"));
        mockMvc.perform(post("/api/v1/recurrence-rules/" + rule.getId() + "/generate")
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existingCount", is(6)));
    }
}
