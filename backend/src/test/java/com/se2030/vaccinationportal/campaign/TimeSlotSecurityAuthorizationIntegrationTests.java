package com.se2030.vaccinationportal.campaign;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Security Integration Tests for TimeSlot REST endpoints.
 * Validates role-based authorization matrix and unauthenticated access behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TimeSlotSecurityAuthorizationIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private TimeSlotRepository timeSlotRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    private VaccinationSession session;
    private TimeSlot slot;
    private Instant slotStart;
    private Instant slotEnd;

    @BeforeEach
    void setUp() {
        VaccinationCenter center = centerRepository.save(new VaccinationCenter(
                "CTR-TS-SEC-01", "TimeSlot Security Center", "123 Defense Way", "+12345",
                CenterStatus.ACTIVE, 50, 100, "UTC"
        ));

        VaccineType vaccine = vaccineTypeRepository.save(new VaccineType("VAX-TS-SEC-01", "Security Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-TS-SEC-01", "Security Target"));

        VaccinationCampaign campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-TS-SEC-01", "Security Campaign", "Testing TimeSlot Security",
                vaccine, targetGroup,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                CampaignStatus.ACTIVE
        ));

        Instant sessionStart = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant sessionEnd = sessionStart.plus(8, ChronoUnit.HOURS);

        session = new VaccinationSession(
                campaign,
                center,
                sessionStart,
                sessionEnd,
                sessionStart.minus(2, ChronoUnit.HOURS),
                "UTC"
        );
        session.schedule();
        session = sessionRepository.save(session);

        slotStart = sessionStart.plus(1, ChronoUnit.HOURS);
        slotEnd = sessionStart.plus(2, ChronoUnit.HOURS);

        slot = timeSlotRepository.save(new TimeSlot(session, slotStart, slotEnd, 10));
    }

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    // =========================================================================
    // 1. GET /api/v1/sessions/{sessionId}/time-slots
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/sessions/{sessionId}/time-slots Authorization")
    class GetTimeSlotsSecurity {

        @Test
        @DisplayName("ROLE_ADMIN is authorized (200 OK)")
        void adminCanListSlots() throws Exception {
            String auth = token(1L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/time-slots")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id", is(slot.getId().intValue())));
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER is authorized (200 OK)")
        void managerCanListSlots() throws Exception {
            String auth = token(2L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/time-slots")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id", is(slot.getId().intValue())));
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER is authorized (200 OK)")
        void healthcareOfficerCanListSlots() throws Exception {
            String auth = token(3L, "officer@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/time-slots")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id", is(slot.getId().intValue())));
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER is denied (403 Forbidden)")
        void campaignOfficerDenied() throws Exception {
            String auth = token(4L, "campaign@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/time-slots")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_MANAGEMENT_USER is denied (403 Forbidden)")
        void managementUserDenied() throws Exception {
            String auth = token(5L, "mgmt@portal.gov", List.of("ROLE_MANAGEMENT_USER"));
            mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/time-slots")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN is denied (403 Forbidden)")
        void citizenDenied() throws Exception {
            String auth = token(6L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/time-slots")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous access is denied (401 or 403)")
        void anonymousDenied() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/time-slots"))
                    .andExpect(status().is4xxClientError());
        }
    }

    // =========================================================================
    // 2. POST /api/v1/sessions/{sessionId}/time-slots
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/sessions/{sessionId}/time-slots Authorization")
    class CreateTimeSlotSecurity {

        @Test
        @DisplayName("ROLE_ADMIN is authorized (201 Created)")
        void adminCanCreateSlot() throws Exception {
            String auth = token(1L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            String payload = String.format("""
                    {
                        "startTime": "%s",
                        "endTime": "%s",
                        "capacity": 15
                    }
                    """, slotEnd, slotEnd.plus(1, ChronoUnit.HOURS));

            mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/time-slots")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.capacity", is(15)));
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER is authorized (201 Created)")
        void managerCanCreateSlot() throws Exception {
            String auth = token(2L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            String payload = String.format("""
                    {
                        "startTime": "%s",
                        "endTime": "%s",
                        "capacity": 12
                    }
                    """, slotEnd, slotEnd.plus(1, ChronoUnit.HOURS));

            mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/time-slots")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.capacity", is(12)));
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER is denied (403 Forbidden)")
        void healthcareOfficerCannotCreateSlot() throws Exception {
            String auth = token(3L, "officer@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            String payload = String.format("""
                    {
                        "startTime": "%s",
                        "endTime": "%s",
                        "capacity": 10
                    }
                    """, slotEnd, slotEnd.plus(1, ChronoUnit.HOURS));

            mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/time-slots")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER is denied (403 Forbidden)")
        void campaignOfficerCannotCreateSlot() throws Exception {
            String auth = token(4L, "campaign@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            String payload = String.format("""
                    {
                        "startTime": "%s",
                        "endTime": "%s",
                        "capacity": 10
                    }
                    """, slotEnd, slotEnd.plus(1, ChronoUnit.HOURS));

            mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/time-slots")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous access is denied (401 or 403)")
        void anonymousCannotCreateSlot() throws Exception {
            String payload = String.format("""
                    {
                        "startTime": "%s",
                        "endTime": "%s",
                        "capacity": 10
                    }
                    """, slotEnd, slotEnd.plus(1, ChronoUnit.HOURS));

            mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/time-slots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().is4xxClientError());
        }
    }

    // =========================================================================
    // 3. PUT /api/v1/sessions/{sessionId}/time-slots/{slotId}/capacity
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/sessions/{sessionId}/time-slots/{slotId}/capacity Authorization")
    class UpdateTimeSlotCapacitySecurity {

        @Test
        @DisplayName("ROLE_ADMIN is authorized (200 OK)")
        void adminCanUpdateCapacity() throws Exception {
            String auth = token(1L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            String payload = """
                    {
                        "capacity": 25
                    }
                    """;

            mockMvc.perform(put("/api/v1/sessions/" + session.getId() + "/time-slots/" + slot.getId() + "/capacity")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.capacity", is(25)));
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER is authorized (200 OK)")
        void managerCanUpdateCapacity() throws Exception {
            String auth = token(2L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            String payload = """
                    {
                        "capacity": 20
                    }
                    """;

            mockMvc.perform(put("/api/v1/sessions/" + session.getId() + "/time-slots/" + slot.getId() + "/capacity")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.capacity", is(20)));
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER is denied (403 Forbidden)")
        void healthcareOfficerCannotUpdateCapacity() throws Exception {
            String auth = token(3L, "officer@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            String payload = """
                    {
                        "capacity": 20
                    }
                    """;

            mockMvc.perform(put("/api/v1/sessions/" + session.getId() + "/time-slots/" + slot.getId() + "/capacity")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN is denied (403 Forbidden)")
        void citizenCannotUpdateCapacity() throws Exception {
            String auth = token(6L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            String payload = """
                    {
                        "capacity": 20
                    }
                    """;

            mockMvc.perform(put("/api/v1/sessions/" + session.getId() + "/time-slots/" + slot.getId() + "/capacity")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous access is denied (401 or 403)")
        void anonymousCannotUpdateCapacity() throws Exception {
            String payload = """
                    {
                        "capacity": 20
                    }
                    """;

            mockMvc.perform(put("/api/v1/sessions/" + session.getId() + "/time-slots/" + slot.getId() + "/capacity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().is4xxClientError());
        }
    }
}
