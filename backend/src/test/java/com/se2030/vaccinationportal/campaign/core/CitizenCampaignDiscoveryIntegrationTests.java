package com.se2030.vaccinationportal.campaign.core;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for authenticated citizen campaign discovery:
 * GET /api/v1/citizen/campaigns (MF02-R-001).
 *
 * Validates:
 * 1. Role-based authorization (ROLE_CITIZEN only; anonymous and staff roles denied).
 * 2. Lifecycle status filtering (only PUBLISHED and ACTIVE returned; all other 6 excluded).
 * 3. Minimal projection schema (only approved fields exposed; sensitive/internal fields omitted).
 * 4. Deterministic booking-window boundary evaluation using injected Clock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(CitizenCampaignDiscoveryIntegrationTests.DynamicClockTestConfig.class)
public class CitizenCampaignDiscoveryIntegrationTests {

    private static final Instant DEFAULT_EVAL_INSTANT = Instant.parse("2026-10-15T12:00:00Z");
    private static final MutableClock mutableClock = new MutableClock(DEFAULT_EVAL_INSTANT, ZoneOffset.UTC);

    @TestConfiguration
    static class DynamicClockTestConfig {
        @Bean
        @Primary
        Clock citizenDiscoveryTestClock() {
            return mutableClock;
        }
    }

    static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneOffset zone;

        MutableClock(Instant instant, ZoneOffset zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, ZoneOffset.UTC);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private VaccineType vaccine;
    private TargetGroup targetGroup;

    private static final LocalDate SERVICE_START = LocalDate.of(2026, 11, 1);
    private static final LocalDate SERVICE_END = LocalDate.of(2026, 11, 30);
    private static final Instant BOOKING_OPEN = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant BOOKING_CLOSE = Instant.parse("2026-10-31T23:59:59Z");

    @BeforeEach
    void setUp() {
        mutableClock.setInstant(DEFAULT_EVAL_INSTANT);
        vaccine = vaccineTypeRepository.save(new VaccineType("VAX-CIT-01", "Citizen Discovery Vaccine"));
        targetGroup = targetGroupRepository.save(new TargetGroup("TG-CIT-01", "Citizen Discovery Target Group"));
    }

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    private VaccinationCampaign seedCampaign(String code, String name, CampaignStatus status) {
        return seedCampaign(code, name, status, BOOKING_OPEN, BOOKING_CLOSE);
    }

    private VaccinationCampaign seedCampaign(String code, String name, CampaignStatus status, Instant openAt, Instant closeAt) {
        return campaignRepository.save(CampaignTestFixtures.campaign(
                code, name, "Internal description for " + code,
                vaccine, targetGroup, SERVICE_START, SERVICE_END, openAt, closeAt, status
        ));
    }

    // =========================================================================
    // 1. ROLE AUTHORIZATION
    // =========================================================================

    @Nested
    @DisplayName("Role Authorization")
    class RoleAuthorization {

        @Test
        @DisplayName("Anonymous GET /api/v1/citizen/campaigns is denied 403")
        void anonymousRequestDenied() throws Exception {
            mockMvc.perform(get("/api/v1/citizen/campaigns"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Authenticated ROLE_CITIZEN succeeds with 200 OK")
        void citizenRequestSucceeds() throws Exception {
            seedCampaign("CMP-CIT-AUTH-1", "Citizen Visible Campaign", CampaignStatus.PUBLISHED);
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            mockMvc.perform(get("/api/v1/citizen/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("Non-Citizen staff roles are denied 403")
        void staffRolesDenied() throws Exception {
            List<String> staffRoles = List.of(
                    "ROLE_ADMIN",
                    "ROLE_CAMPAIGN_OFFICER",
                    "ROLE_CENTER_MANAGER",
                    "ROLE_HEALTHCARE_OFFICER",
                    "ROLE_MANAGEMENT_USER",
                    "ROLE_INVENTORY_OFFICER"
            );

            long userId = 1001L;
            for (String role : staffRoles) {
                String auth = token(userId++, role.toLowerCase() + "@portal.gov", List.of(role));
                mockMvc.perform(get("/api/v1/citizen/campaigns")
                                .header(HttpHeaders.AUTHORIZATION, auth))
                        .andExpect(status().isForbidden());
            }
        }
    }

    // =========================================================================
    // 2. LIFECYCLE STATUS FILTERING
    // =========================================================================

    @Nested
    @DisplayName("Lifecycle Status Filtering")
    class LifecycleStatusFiltering {

        @Test
        @DisplayName("Returns only PUBLISHED and ACTIVE campaigns; excludes all other 6 statuses")
        void returnsOnlyPublishedAndActive() throws Exception {
            // Seed all 8 lifecycle statuses
            seedCampaign("CMP-STATE-DFT", "Draft Campaign", CampaignStatus.DRAFT);
            seedCampaign("CMP-STATE-SCH", "Scheduled Campaign", CampaignStatus.SCHEDULED);
            VaccinationCampaign published = seedCampaign("CMP-STATE-PUB", "Published Campaign", CampaignStatus.PUBLISHED);
            VaccinationCampaign active = seedCampaign("CMP-STATE-ACT", "Active Campaign", CampaignStatus.ACTIVE);
            seedCampaign("CMP-STATE-SUS", "Suspended Campaign", CampaignStatus.SUSPENDED);
            seedCampaign("CMP-STATE-COM", "Completed Campaign", CampaignStatus.COMPLETED);
            seedCampaign("CMP-STATE-CAN", "Cancelled Campaign", CampaignStatus.CANCELLED);
            seedCampaign("CMP-STATE-ARC", "Archived Campaign", CampaignStatus.ARCHIVED);

            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            mockMvc.perform(get("/api/v1/citizen/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(published.getId().intValue())))
                    .andExpect(jsonPath("$[0].status", is("PUBLISHED")))
                    .andExpect(jsonPath("$[1].id", is(active.getId().intValue())))
                    .andExpect(jsonPath("$[1].status", is("ACTIVE")));
        }

        @Test
        @DisplayName("No caller-controlled status bypass exists")
        void statusQueryParameterHasNoBypassEffect() throws Exception {
            seedCampaign("CMP-BYP-DFT", "Draft Campaign", CampaignStatus.DRAFT);
            seedCampaign("CMP-BYP-SCH", "Scheduled Campaign", CampaignStatus.SCHEDULED);
            seedCampaign("CMP-BYP-PUB", "Published Campaign", CampaignStatus.PUBLISHED);

            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            // Attempt to query hidden status via request parameter
            mockMvc.perform(get("/api/v1/citizen/campaigns")
                            .param("status", "DRAFT")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("PUBLISHED")));

            // Attempt to query SCHEDULED
            mockMvc.perform(get("/api/v1/citizen/campaigns")
                            .param("status", "SCHEDULED")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("PUBLISHED")));
        }
    }

    // =========================================================================
    // 3. MINIMAL PROJECTION INTEGRITY
    // =========================================================================

    @Nested
    @DisplayName("Minimal Projection Contract")
    class MinimalProjectionContract {

        @Test
        @DisplayName("Response exposes only approved minimal fields and excludes internal metadata")
        void projectionExposesOnlyApprovedFields() throws Exception {
            VaccinationCampaign campaign = seedCampaign("CMP-PROJ-01", "Projection Campaign", CampaignStatus.PUBLISHED);
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            MvcResult result = mockMvc.perform(get("/api/v1/citizen/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(campaign.getId().intValue())))
                    .andExpect(jsonPath("$[0].code", is("CMP-PROJ-01")))
                    .andExpect(jsonPath("$[0].name", is("Projection Campaign")))
                    .andExpect(jsonPath("$[0].vaccineTypeCode", is("VAX-CIT-01")))
                    .andExpect(jsonPath("$[0].targetGroupCode", is("TG-CIT-01")))
                    .andExpect(jsonPath("$[0].serviceStartDate", is("2026-11-01")))
                    .andExpect(jsonPath("$[0].serviceEndDate", is("2026-11-30")))
                    .andExpect(jsonPath("$[0].status", is("PUBLISHED")))
                    .andExpect(jsonPath("$[0].currentlyAcceptingScreening", is(true)))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            JsonNode item = root.get(0);

            // Verify absence of sensitive and internal fields
            org.junit.jupiter.api.Assertions.assertFalse(item.has("description"), "description must not be exposed");
            org.junit.jupiter.api.Assertions.assertFalse(item.has("bookingOpenAt"), "bookingOpenAt must not be exposed");
            org.junit.jupiter.api.Assertions.assertFalse(item.has("bookingCloseAt"), "bookingCloseAt must not be exposed");
            org.junit.jupiter.api.Assertions.assertFalse(item.has("createdAt"), "createdAt must not be exposed");
            org.junit.jupiter.api.Assertions.assertFalse(item.has("updatedAt"), "updatedAt must not be exposed");
            org.junit.jupiter.api.Assertions.assertFalse(item.has("assignments"), "assignments must not be exposed");
            org.junit.jupiter.api.Assertions.assertFalse(item.has("capacity"), "capacity must not be exposed");
            org.junit.jupiter.api.Assertions.assertFalse(item.has("readiness"), "readiness must not be exposed");
        }
    }

    // =========================================================================
    // 4. DETERMINISTIC BOOKING-WINDOW BOUNDARY EVALUATION
    // =========================================================================

    @Nested
    @DisplayName("Booking-Window Boundary Evaluation")
    class BookingWindowBoundaryEvaluation {

        private VaccinationCampaign publishedCampaign;
        private VaccinationCampaign activeCampaign;

        @BeforeEach
        void seedWindowCampaigns() {
            // Window: 2026-10-01T00:00:00Z to 2026-10-31T23:59:59Z
            publishedCampaign = seedCampaign("CMP-WIN-PUB", "Published Window Campaign", CampaignStatus.PUBLISHED);
            activeCampaign = seedCampaign("CMP-WIN-ACT", "Active Window Campaign", CampaignStatus.ACTIVE);
        }

        @Test
        @DisplayName("Before opening: visible + currentlyAcceptingScreening=false")
        void beforeOpeningIsVisibleButNotAcceptingScreening() throws Exception {
            mutableClock.setInstant(BOOKING_OPEN.minusSeconds(1)); // 2026-09-30T23:59:59Z
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            mockMvc.perform(get("/api/v1/citizen/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].currentlyAcceptingScreening", is(false)))
                    .andExpect(jsonPath("$[1].currentlyAcceptingScreening", is(false)));
        }

        @Test
        @DisplayName("Exactly at opening: visible + currentlyAcceptingScreening=true")
        void exactlyAtOpeningAcceptsScreening() throws Exception {
            mutableClock.setInstant(BOOKING_OPEN); // 2026-10-01T00:00:00Z
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            mockMvc.perform(get("/api/v1/citizen/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].currentlyAcceptingScreening", is(true)))
                    .andExpect(jsonPath("$[1].currentlyAcceptingScreening", is(true)));
        }

        @Test
        @DisplayName("Inside window: visible + currentlyAcceptingScreening=true")
        void insideWindowAcceptsScreening() throws Exception {
            mutableClock.setInstant(Instant.parse("2026-10-15T12:00:00Z"));
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            mockMvc.perform(get("/api/v1/citizen/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].currentlyAcceptingScreening", is(true)))
                    .andExpect(jsonPath("$[1].currentlyAcceptingScreening", is(true)));
        }

        @Test
        @DisplayName("Exactly at closing: visible + currentlyAcceptingScreening=true (inclusive boundary)")
        void exactlyAtClosingAcceptsScreening() throws Exception {
            mutableClock.setInstant(BOOKING_CLOSE); // 2026-10-31T23:59:59Z
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            mockMvc.perform(get("/api/v1/citizen/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].currentlyAcceptingScreening", is(true)))
                    .andExpect(jsonPath("$[1].currentlyAcceptingScreening", is(true)));
        }

        @Test
        @DisplayName("After closing: visible if PUBLISHED/ACTIVE + currentlyAcceptingScreening=false")
        void afterClosingIsVisibleButNotAcceptingScreening() throws Exception {
            mutableClock.setInstant(BOOKING_CLOSE.plusSeconds(1)); // 2026-11-01T00:00:00Z
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            mockMvc.perform(get("/api/v1/citizen/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].currentlyAcceptingScreening", is(false)))
                    .andExpect(jsonPath("$[1].currentlyAcceptingScreening", is(false)));
        }
    }
}
