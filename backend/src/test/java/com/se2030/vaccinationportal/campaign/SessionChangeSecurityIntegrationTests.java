package com.se2030.vaccinationportal.campaign;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
class SessionChangeSecurityIntegrationTests {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;

    private VaccinationCampaign campaign;
    private VaccinationCenter centerA;
    private VaccinationCenter centerB;
    private VaccinationSession session;

    private static final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);
    private static final Instant CAMPAIGN_REG_START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant CAMPAIGN_REG_END = Instant.parse("2026-10-30T00:00:00Z");

    @BeforeEach
    void setUp() {
        VaccineType vaccine = vaccineTypeRepository.save(new VaccineType("VAX-CHANGE-SEC", "Change Security Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-CHANGE-SEC", "Change Security Target"));

        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-CHG-SEC-01", "Change Security Campaign", "Security integration testing",
                vaccine, targetGroup,
                CAMPAIGN_START, CAMPAIGN_END,
                CAMPAIGN_REG_START, CAMPAIGN_REG_END,
                CampaignStatus.DRAFT
        ));

        doseProgrammeRepository.save(new CampaignDoseProgramme(
                campaign, 1, DoseCategory.PRIMARY, "1", 21, false, 10,
                CAMPAIGN_START, CAMPAIGN_END
        ));

        centerA = centerRepository.save(new VaccinationCenter(
                "CTR-CHG-SEC-A", "Center A", "101 Alpha Road", "+94110000001",
                CenterStatus.ACTIVE, 100, 30, "UTC"
        ));
        centerB = centerRepository.save(new VaccinationCenter(
                "CTR-CHG-SEC-B", "Center B", "202 Beta Road", "+94110000002",
                CenterStatus.ACTIVE, 100, 30, "UTC"
        ));

        assignmentRepository.save(new CampaignCenterAssignment(campaign, centerA, CAMPAIGN_START, CAMPAIGN_END));
        assignmentRepository.save(new CampaignCenterAssignment(campaign, centerB, CAMPAIGN_START, CAMPAIGN_END));

        for (DayOfWeek day : DayOfWeek.values()) {
            operatingHoursRepository.save(new OperatingHours(centerA, day, LocalTime.of(8, 0), LocalTime.of(18, 0), CAMPAIGN_START));
            operatingHoursRepository.save(new OperatingHours(centerB, day, LocalTime.of(8, 0), LocalTime.of(18, 0), CAMPAIGN_START));
        }

        session = sessionRepository.save(new VaccinationSession(
                campaign,
                centerA,
                Instant.parse("2026-10-10T09:00:00Z"),
                Instant.parse("2026-10-10T12:00:00Z"),
                Instant.parse("2026-10-10T08:00:00Z"),
                "UTC"
        ));
    }

    private String createBearerToken(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    private String obtainCancellationPreviewToken(Long sessionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/cancellation-preview"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("previewToken").asText();
    }

    private String obtainReschedulePreviewToken(Long sessionId, Instant start, Instant end, Instant bookingClose) throws Exception {
        String payload = String.format("""
                {
                    "newStartAt": "%s",
                    "newEndAt": "%s",
                    "newBookingCloseAt": "%s"
                }
                """, start, end, bookingClose);
        MvcResult result = mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/reschedule-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("previewToken").asText();
    }

    private String obtainTransferPreviewToken(Long sessionId, Long targetCenterId, Instant start, Instant end, Instant bookingClose) throws Exception {
        String payload = String.format("""
                {
                    "newCenterId": %d,
                    "newStartAt": "%s",
                    "newEndAt": "%s",
                    "newBookingCloseAt": "%s"
                }
                """, targetCenterId, start, end, bookingClose);
        MvcResult result = mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/transfer-preview")
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
    @DisplayName("Anonymous POST /cancel returns 401 Unauthorized")
    void anonymousCancelReturns401() throws Exception {
        String token = obtainCancellationPreviewToken(session.getId());
        String payload = String.format("""
                {
                    "previewToken": "%s",
                    "reason": "Emergency cancellation"
                }
                """, token);

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        assertEquals(0, sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId()).size());
    }

    @Test
    @DisplayName("Anonymous POST /reschedule returns 401 Unauthorized")
    void anonymousRescheduleReturns401() throws Exception {
        Instant newStart = Instant.parse("2026-10-11T10:00:00Z");
        Instant newEnd = Instant.parse("2026-10-11T13:00:00Z");
        Instant newClose = Instant.parse("2026-10-11T09:00:00Z");
        String token = obtainReschedulePreviewToken(session.getId(), newStart, newEnd, newClose);

        String payload = String.format("""
                {
                    "newStartAt": "%s",
                    "newEndAt": "%s",
                    "newBookingCloseAt": "%s",
                    "previewToken": "%s",
                    "reason": "Maintenance reschedule"
                }
                """, newStart, newEnd, newClose, token);

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        assertEquals(0, sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId()).size());
    }

    @Test
    @DisplayName("Anonymous POST /transfer returns 401 Unauthorized")
    void anonymousTransferReturns401() throws Exception {
        Instant newStart = Instant.parse("2026-10-11T10:00:00Z");
        Instant newEnd = Instant.parse("2026-10-11T13:00:00Z");
        Instant newClose = Instant.parse("2026-10-11T09:00:00Z");
        String token = obtainTransferPreviewToken(session.getId(), centerB.getId(), newStart, newEnd, newClose);

        String payload = String.format("""
                {
                    "newCenterId": %d,
                    "newStartAt": "%s",
                    "newEndAt": "%s",
                    "newBookingCloseAt": "%s",
                    "previewToken": "%s",
                    "reason": "Facility transfer"
                }
                """, centerB.getId(), newStart, newEnd, newClose, token);

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        assertEquals(0, sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId()).size());
    }

    // =========================================================================
    // 2. AUTHENTICATED CANCELLATION & SPOOFING / OMISSION PROOFS
    // =========================================================================

    @Test
    @DisplayName("Authenticated cancel persists principal userId string as actorId")
    void authenticatedCancelPersistsPrincipalUserId() throws Exception {
        String token = obtainCancellationPreviewToken(session.getId());
        String authHeader = createBearerToken(42L, "officer42@health.gov", List.of("ROLE_USER"));

        String payload = String.format("""
                {
                    "previewToken": "%s",
                    "reason": "Inclement weather shutdown"
                }
                """, token);

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(session.getId().intValue())))
                .andExpect(jsonPath("$.lifecycleStatus", is("CANCELLED")));

        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(1, events.size());
        SessionChangeEvent event = events.get(0);
        assertEquals(SessionChangeType.CANCELLED, event.getChangeType());
        assertEquals("42", event.getActorId());
        assertEquals("Inclement weather shutdown", event.getReason());
    }

    @Test
    @DisplayName("Authenticated cancel ignores spoofed body actorId and persists principal userId string")
    void authenticatedCancelIgnoresSpoofedActorId() throws Exception {
        String token = obtainCancellationPreviewToken(session.getId());
        String authHeader = createBearerToken(42L, "officer42@health.gov", List.of("ROLE_USER"));

        String payload = String.format("""
                {
                    "previewToken": "%s",
                    "reason": "Inclement weather shutdown",
                    "actorId": "spoofed-attacker-999"
                }
                """, token);

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(session.getId().intValue())))
                .andExpect(jsonPath("$.lifecycleStatus", is("CANCELLED")));

        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(1, events.size());
        assertEquals("42", events.get(0).getActorId());
    }

    // =========================================================================
    // 3. AUTHENTICATED RESCHEDULE & SPOOFING / OMISSION PROOFS
    // =========================================================================

    @Test
    @DisplayName("Authenticated reschedule persists principal userId string and ignores spoofed actorId")
    void authenticatedReschedulePersistsPrincipalUserIdAndIgnoresSpoof() throws Exception {
        Instant newStart = Instant.parse("2026-10-11T10:00:00Z");
        Instant newEnd = Instant.parse("2026-10-11T13:00:00Z");
        Instant newClose = Instant.parse("2026-10-11T09:00:00Z");
        String token = obtainReschedulePreviewToken(session.getId(), newStart, newEnd, newClose);
        String authHeader = createBearerToken(77L, "coordinator77@health.gov", List.of("ROLE_USER"));

        String payload = String.format("""
                {
                    "newStartAt": "%s",
                    "newEndAt": "%s",
                    "newBookingCloseAt": "%s",
                    "previewToken": "%s",
                    "reason": "Facility deep clean",
                    "actorId": "admin@example.com"
                }
                """, newStart, newEnd, newClose, token);

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/reschedule")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(session.getId().intValue())))
                .andExpect(jsonPath("$.startAt", is("2026-10-11T10:00:00Z")))
                .andExpect(jsonPath("$.endAt", is("2026-10-11T13:00:00Z")));

        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(1, events.size());
        SessionChangeEvent event = events.get(0);
        assertEquals(SessionChangeType.RESCHEDULED, event.getChangeType());
        assertEquals("77", event.getActorId());
        assertEquals("Facility deep clean", event.getReason());
    }

    @Test
    @DisplayName("Authenticated reschedule succeeds when actorId field is omitted from request body")
    void authenticatedRescheduleOmittedActorAccepted() throws Exception {
        Instant newStart = Instant.parse("2026-10-11T10:00:00Z");
        Instant newEnd = Instant.parse("2026-10-11T13:00:00Z");
        Instant newClose = Instant.parse("2026-10-11T09:00:00Z");
        String token = obtainReschedulePreviewToken(session.getId(), newStart, newEnd, newClose);
        String authHeader = createBearerToken(77L, "coordinator77@health.gov", List.of("ROLE_USER"));

        String payload = String.format("""
                {
                    "newStartAt": "%s",
                    "newEndAt": "%s",
                    "newBookingCloseAt": "%s",
                    "previewToken": "%s",
                    "reason": "Facility deep clean"
                }
                """, newStart, newEnd, newClose, token);

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/reschedule")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(1, events.size());
        assertEquals("77", events.get(0).getActorId());
    }

    // =========================================================================
    // 4. AUTHENTICATED TRANSFER & SPOOFING / OMISSION PROOFS
    // =========================================================================

    @Test
    @DisplayName("Authenticated transfer persists principal userId string and ignores spoofed actorId")
    void authenticatedTransferPersistsPrincipalUserIdAndIgnoresSpoof() throws Exception {
        Instant newStart = Instant.parse("2026-10-11T10:00:00Z");
        Instant newEnd = Instant.parse("2026-10-11T13:00:00Z");
        Instant newClose = Instant.parse("2026-10-11T09:00:00Z");
        String token = obtainTransferPreviewToken(session.getId(), centerB.getId(), newStart, newEnd, newClose);
        String authHeader = createBearerToken(99L, "director99@health.gov", List.of("ROLE_USER"));

        String payload = String.format("""
                {
                    "newCenterId": %d,
                    "newStartAt": "%s",
                    "newEndAt": "%s",
                    "newBookingCloseAt": "%s",
                    "previewToken": "%s",
                    "reason": "Center relocation due to renovation",
                    "actorId": "spoofed-999"
                }
                """, centerB.getId(), newStart, newEnd, newClose, token);

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/transfer")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(session.getId().intValue())))
                .andExpect(jsonPath("$.centerId", is(centerB.getId().intValue())));

        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(1, events.size());
        SessionChangeEvent event = events.get(0);
        assertEquals(SessionChangeType.CENTER_TRANSFERRED, event.getChangeType());
        assertEquals("99", event.getActorId());
        assertEquals("Center relocation due to renovation", event.getReason());
    }

    @Test
    @DisplayName("Authenticated transfer succeeds when actorId field is omitted from request body")
    void authenticatedTransferOmittedActorAccepted() throws Exception {
        Instant newStart = Instant.parse("2026-10-11T10:00:00Z");
        Instant newEnd = Instant.parse("2026-10-11T13:00:00Z");
        Instant newClose = Instant.parse("2026-10-11T09:00:00Z");
        String token = obtainTransferPreviewToken(session.getId(), centerB.getId(), newStart, newEnd, newClose);
        String authHeader = createBearerToken(99L, "director99@health.gov", List.of("ROLE_USER"));

        String payload = String.format("""
                {
                    "newCenterId": %d,
                    "newStartAt": "%s",
                    "newEndAt": "%s",
                    "newBookingCloseAt": "%s",
                    "previewToken": "%s",
                    "reason": "Center relocation due to renovation"
                }
                """, centerB.getId(), newStart, newEnd, newClose, token);

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/transfer")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(session.getId().intValue())))
                .andExpect(jsonPath("$.centerId", is(centerB.getId().intValue())));

        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertEquals(1, events.size());
        assertEquals("99", events.get(0).getActorId());
    }

    // =========================================================================
    // 5. CHANGE HISTORY ENDPOINT RETURNS SERVER-DERIVED ACTOR ID
    // =========================================================================

    @Test
    @DisplayName("GET /change-events returns server-derived actor ID in event list")
    void getChangeEventsReturnsServerDerivedActorId() throws Exception {
        String token = obtainCancellationPreviewToken(session.getId());
        String authHeader = createBearerToken(42L, "officer42@health.gov", List.of("ROLE_USER"));

        String payload = String.format("""
                {
                    "previewToken": "%s",
                    "reason": "Severe weather closure",
                    "actorId": "spoofed-input-ignored"
                }
                """, token);

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/change-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sessionId", is(session.getId().intValue())))
                .andExpect(jsonPath("$[0].changeType", is("CANCELLED")))
                .andExpect(jsonPath("$[0].reason", is("Severe weather closure")))
                .andExpect(jsonPath("$[0].actorId", is("42")));
    }
}
