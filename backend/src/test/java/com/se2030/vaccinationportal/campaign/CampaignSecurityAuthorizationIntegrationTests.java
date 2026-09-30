package com.se2030.vaccinationportal.campaign;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleChangeType;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignLifecycleEventRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import java.time.Instant;
import java.time.LocalDate;
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
 * Comprehensive H01 Security Integration Tests for Campaign and Campaign Configuration endpoints.
 * Validates role-based authorization, anonymous denial, actor spoofing prevention, and public read preservation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CampaignSecurityAuthorizationIntegrationTests {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private CampaignLifecycleEventRepository lifecycleEventRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private QuestionnaireRepository questionnaireRepository;
    @Autowired private QuestionnaireVersionRepository versionRepository;

    private VaccinationCampaign draftCampaign;
    private VaccinationCampaign activeCampaign;
    private VaccinationCampaign suspendedCampaign;
    private VaccinationCenter center;
    private VaccineType vaccine;
    private TargetGroup targetGroup;
    private QuestionnaireVersion publishedVersion;

    private static final LocalDate START_DATE = LocalDate.of(2026, 11, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 11, 30);
    private static final Instant REG_START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant REG_END = Instant.parse("2026-10-31T00:00:00Z");

    @BeforeEach
    void setUp() {
        vaccine = vaccineTypeRepository.save(new VaccineType("VAX-H01-CMP", "H01 Campaign Vaccine"));
        targetGroup = targetGroupRepository.save(new TargetGroup("TG-H01-CMP", "H01 Campaign Target Group"));

        Questionnaire q = questionnaireRepository.save(new Questionnaire("SEC-Q", "Security Questionnaire", "Desc"));
        publishedVersion = versionRepository.save(new QuestionnaireVersion(q, 1, QuestionnaireVersionStatus.PUBLISHED));

        draftCampaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-H01-DFT", "Draft Campaign", "Desc",
                vaccine, targetGroup, START_DATE, END_DATE, REG_START, REG_END,
                CampaignStatus.DRAFT
        ));

        activeCampaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-H01-ACT", "Active Campaign", "Desc",
                vaccine, targetGroup, START_DATE, END_DATE, REG_START, REG_END,
                CampaignStatus.ACTIVE
        ));

        suspendedCampaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-H01-SUS", "Suspended Campaign", "Desc",
                vaccine, targetGroup, START_DATE, END_DATE, REG_START, REG_END,
                CampaignStatus.SUSPENDED
        ));

        center = centerRepository.save(new VaccinationCenter(
                "CTR-H01-CMP", "H01 Test Center", "123 Main St", "contact@health.gov",
                com.se2030.vaccinationportal.campaign.model.CenterStatus.ACTIVE, 100, 200, "UTC"
        ));
    }

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    // =========================================================================
    // 1. ADMINISTRATIVE CAMPAIGN READS REQUIRE AUTHORIZED STAFF ROLES (MF02-R-001)
    // =========================================================================

    @Nested
    @DisplayName("Administrative Campaign Reads")
    class AdministrativeCampaignReads {

        @Test
        @DisplayName("Anonymous GET /api/v1/campaigns is denied 403")
        void anonymousListCampaignsDenied() throws Exception {
            mockMvc.perform(get("/api/v1/campaigns"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous GET /api/v1/campaigns/{id} is denied 403")
        void anonymousGetCampaignByIdDenied() throws Exception {
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN GET /api/v1/campaigns is denied 403")
        void citizenListCampaignsDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            mockMvc.perform(get("/api/v1/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN GET /api/v1/campaigns/{id} is denied 403")
        void citizenGetCampaignByIdDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_INVENTORY_OFFICER GET /api/v1/campaigns is denied 403")
        void inventoryOfficerListCampaignsDenied() throws Exception {
            String auth = token(601L, "inventory@portal.gov", List.of("ROLE_INVENTORY_OFFICER"));
            mockMvc.perform(get("/api/v1/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_INVENTORY_OFFICER GET /api/v1/campaigns/{id} is denied 403")
        void inventoryOfficerGetCampaignByIdDenied() throws Exception {
            String auth = token(601L, "inventory@portal.gov", List.of("ROLE_INVENTORY_OFFICER"));
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Authorized staff roles succeed on GET /api/v1/campaigns and GET /api/v1/campaigns/{id} with 200 OK")
        void authorizedStaffRolesSucceed() throws Exception {
            List<String> authorizedRoles = List.of(
                    "ROLE_ADMIN",
                    "ROLE_CAMPAIGN_OFFICER",
                    "ROLE_CENTER_MANAGER",
                    "ROLE_HEALTHCARE_OFFICER",
                    "ROLE_MANAGEMENT_USER"
            );

            long userId = 101L;
            for (String role : authorizedRoles) {
                String auth = token(userId++, role.toLowerCase() + "@portal.gov", List.of(role));

                mockMvc.perform(get("/api/v1/campaigns")
                                .header(HttpHeaders.AUTHORIZATION, auth))
                        .andExpect(status().isOk());

                mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId())
                                .header(HttpHeaders.AUTHORIZATION, auth))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id", is(draftCampaign.getId().intValue())));
            }
        }
    }

    // =========================================================================
    // 2. INTERNAL AUDIT READS REQUIRE STAFF ROLES
    // =========================================================================

    @Nested
    @DisplayName("Internal Audit Reads")
    class InternalAuditReads {

        @Test
        @DisplayName("Anonymous GET /lifecycle-events is denied with 403 Forbidden")
        void anonymousLifecycleEventsDenied() throws Exception {
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId() + "/lifecycle-events"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN GET /lifecycle-events is denied with 403 Forbidden")
        void citizenLifecycleEventsDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId() + "/lifecycle-events")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER GET /lifecycle-events succeeds with 200 OK")
        void campaignOfficerLifecycleEventsSucceeds() throws Exception {
            String auth = token(201L, "officer@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId() + "/lifecycle-events")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ROLE_MANAGEMENT_USER GET /lifecycle-events succeeds with 200 OK")
        void managementUserLifecycleEventsSucceeds() throws Exception {
            String auth = token(202L, "manager@portal.gov", List.of("ROLE_MANAGEMENT_USER"));
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId() + "/lifecycle-events")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // 3. CAMPAIGN MUTATIONS: ROLE ENFORCEMENT & ANONYMOUS DENIAL
    // =========================================================================

    @Nested
    @DisplayName("Campaign Mutation Role Authorization")
    class CampaignMutationAuthorization {

        @Test
        @DisplayName("Anonymous POST /api/v1/campaigns is denied with 403 Forbidden")
        void anonymousCreateCampaignDenied() throws Exception {
            String payload = String.format("""
                    {
                        "code": "CMP-ANON-TEST",
                        "name": "Anon Campaign",
                        "description": "Desc",
                        "vaccineTypeId": %d,
                        "targetGroupId": %d,
                        "serviceStartDate": "2026-11-01",
                        "serviceEndDate": "2026-11-30",
                        "bookingOpenAt": "2026-10-01T00:00:00Z",
                        "bookingCloseAt": "2026-10-31T00:00:00Z"
                    }
                    """, vaccine.getId(), targetGroup.getId());

            mockMvc.perform(post("/api/v1/campaigns")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN POST /api/v1/campaigns is denied with 403 Forbidden")
        void citizenCreateCampaignDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            String payload = String.format("""
                    {
                        "code": "CMP-CIT-TEST",
                        "name": "Citizen Campaign",
                        "description": "Desc",
                        "vaccineTypeId": %d,
                        "targetGroupId": %d,
                        "serviceStartDate": "2026-11-01",
                        "serviceEndDate": "2026-11-30",
                        "bookingOpenAt": "2026-10-01T00:00:00Z",
                        "bookingCloseAt": "2026-10-31T00:00:00Z"
                    }
                    """, vaccine.getId(), targetGroup.getId());

            mockMvc.perform(post("/api/v1/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_ADMIN POST /api/v1/campaigns succeeds with 201 Created")
        void adminCreateCampaignSucceeds() throws Exception {
            String auth = token(101L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            String payload = String.format("""
                    {
                        "code": "CMP-ADM-AUTH",
                        "name": "Admin Campaign",
                        "description": "Desc",
                        "vaccineTypeId": %d,
                        "targetGroupId": %d,
                        "serviceStartDate": "2026-11-01",
                        "serviceEndDate": "2026-11-30",
                        "bookingOpenAt": "2026-10-01T00:00:00Z",
                        "bookingCloseAt": "2026-10-31T00:00:00Z"
                    }
                    """, vaccine.getId(), targetGroup.getId());

            mockMvc.perform(post("/api/v1/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code", is("CMP-ADM-AUTH")));
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER POST /api/v1/campaigns succeeds with 201 Created")
        void campaignOfficerCreateCampaignSucceeds() throws Exception {
            String auth = token(102L, "officer@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            String payload = String.format("""
                    {
                        "code": "CMP-OFF-AUTH",
                        "name": "Officer Campaign",
                        "description": "Desc",
                        "vaccineTypeId": %d,
                        "targetGroupId": %d,
                        "serviceStartDate": "2026-11-01",
                        "serviceEndDate": "2026-11-30",
                        "bookingOpenAt": "2026-10-01T00:00:00Z",
                        "bookingCloseAt": "2026-10-31T00:00:00Z"
                    }
                    """, vaccine.getId(), targetGroup.getId());

            mockMvc.perform(post("/api/v1/campaigns")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code", is("CMP-OFF-AUTH")));
        }
    }

    // =========================================================================
    // 4. ACTOR TRUST & SPOOFING PREVENTION (SUSPEND / RESUME)
    // =========================================================================

    @Nested
    @DisplayName("Campaign Suspend/Resume Actor Trust & Anti-Spoofing")
    class ActorTrustAndSpoofing {

        @Test
        @DisplayName("Suspend ignores spoofed body actorId and derives actor from authenticated principal")
        void suspendIgnoresSpoofedActorId() throws Exception {
            String auth = token(42L, "officer42@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            String payload = """
                    {
                        "reason": "Temporary vaccine supply constraint",
                        "actorId": "spoofed-attacker-999"
                    }
                    """;

            mockMvc.perform(post("/api/v1/campaigns/" + activeCampaign.getId() + "/suspend")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("SUSPENDED")));

            List<CampaignLifecycleEvent> events = lifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(activeCampaign.getId());
            assertEquals(1, events.size(), "One lifecycle event should be persisted for suspension");
            CampaignLifecycleEvent event = events.get(0);
            assertEquals("42", event.getActorId(), "Persisted actorId must be principal userId (42), not spoofed (999)");
            assertEquals("Temporary vaccine supply constraint", event.getReason());
        }

        @Test
        @DisplayName("Resume ignores spoofed body actorId and derives actor from authenticated principal")
        void resumeIgnoresSpoofedActorId() throws Exception {
            String auth = token(88L, "director88@portal.gov", List.of("ROLE_ADMIN"));
            String payload = """
                    {
                        "reason": "Vaccine supply replenished",
                        "actorId": "spoofed-attacker-777"
                    }
                    """;

            mockMvc.perform(post("/api/v1/campaigns/" + suspendedCampaign.getId() + "/resume")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PUBLISHED")));

            List<CampaignLifecycleEvent> events = lifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(suspendedCampaign.getId());
            assertEquals(1, events.size(), "One lifecycle event should be persisted for resumption");
            CampaignLifecycleEvent event = events.get(0);
            assertEquals("88", event.getActorId(), "Persisted actorId must be principal userId (88), not spoofed (777)");
            assertEquals("Vaccine supply replenished", event.getReason());
        }

        @Test
        @DisplayName("Complete derives actorId from authenticated principal and creates COMPLETED event")
        void completeDerivesActorFromPrincipal() throws Exception {
            String auth = token(99L, "officer99@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            String payload = """
                    {
                        "reason": "Campaign completed successfully"
                    }
                    """;

            mockMvc.perform(post("/api/v1/campaigns/" + activeCampaign.getId() + "/complete")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")));

            List<CampaignLifecycleEvent> events = lifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(activeCampaign.getId());
            assertEquals(1, events.size(), "One lifecycle event should be persisted for completion");
            CampaignLifecycleEvent event = events.get(0);
            assertEquals("99", event.getActorId(), "Persisted actorId must be principal userId (99)");
            assertEquals("Campaign completed successfully", event.getReason());
            assertEquals(CampaignLifecycleChangeType.COMPLETED, event.getChangeType());
        }

        @Test
        @DisplayName("Cancel derives actorId from authenticated principal and creates CANCELLED event")
        void cancelDerivesActorFromPrincipal() throws Exception {
            String auth = token(100L, "admin100@portal.gov", List.of("ROLE_ADMIN"));
            String payload = """
                    {
                        "reason": "Campaign cancelled due to strategy change"
                    }
                    """;

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/cancel")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")));

            List<CampaignLifecycleEvent> events = lifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(draftCampaign.getId());
            assertEquals(1, events.size(), "One lifecycle event should be persisted for cancellation");
            CampaignLifecycleEvent event = events.get(0);
            assertEquals("100", event.getActorId(), "Persisted actorId must be principal userId (100)");
            assertEquals("Campaign cancelled due to strategy change", event.getReason());
            assertEquals(CampaignLifecycleChangeType.CANCELLED, event.getChangeType());
        }

        @Test
        @DisplayName("Archive derives actorId from authenticated principal and creates ARCHIVED event")
        void archiveDerivesActorFromPrincipal() throws Exception {
            // First cancel draft campaign so it is in CANCELLED status (archivable)
            draftCampaign.cancel();
            campaignRepository.save(draftCampaign);

            String auth = token(101L, "admin101@portal.gov", List.of("ROLE_ADMIN"));
            String payload = """
                    {
                        "reason": "Archived after regulatory retention period"
                    }
                    """;

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/archive")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ARCHIVED")));

            List<CampaignLifecycleEvent> events = lifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(draftCampaign.getId());
            assertEquals(1, events.size(), "One lifecycle event should be persisted for archival");
            CampaignLifecycleEvent event = events.get(0);
            assertEquals("101", event.getActorId(), "Persisted actorId must be principal userId (101)");
            assertEquals("Archived after regulatory retention period", event.getReason());
            assertEquals(CampaignLifecycleChangeType.ARCHIVED, event.getChangeType());
        }

        @Test
        @DisplayName("Only ROLE_ADMIN can archive a COMPLETED campaign; all other roles and anonymous are denied 403")
        void onlyAdminCanArchiveCompletedCampaign() throws Exception {
            VaccinationCampaign completedCampaign = campaignRepository.save(CampaignTestFixtures.campaign(
                    "CMP-ARCH-CMP", "Completed Campaign", "Desc",
                    vaccine, targetGroup, START_DATE, END_DATE, REG_START, REG_END,
                    CampaignStatus.COMPLETED
            ));

            String payload = """
                    {
                        "reason": "Archived after regulatory retention period"
                    }
                    """;

            // Anonymous -> 403
            mockMvc.perform(post("/api/v1/campaigns/" + completedCampaign.getId() + "/archive")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());

            // Non-admin roles -> 403
            List<String> unauthorizedRoles = List.of(
                    "ROLE_CAMPAIGN_OFFICER",
                    "ROLE_CENTER_MANAGER",
                    "ROLE_HEALTHCARE_OFFICER",
                    "ROLE_MANAGEMENT_USER",
                    "ROLE_INVENTORY_OFFICER",
                    "ROLE_CITIZEN"
            );

            long userId = 200L;
            for (String role : unauthorizedRoles) {
                String auth = token(userId++, role.toLowerCase() + "@portal.gov", List.of(role));
                mockMvc.perform(post("/api/v1/campaigns/" + completedCampaign.getId() + "/archive")
                                .header(HttpHeaders.AUTHORIZATION, auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isForbidden());
            }

            // Verify status remains COMPLETED before admin executes
            assertEquals(CampaignStatus.COMPLETED, campaignRepository.findById(completedCampaign.getId()).orElseThrow().getStatus());

            // ROLE_ADMIN -> 200 OK
            String adminAuth = token(100L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            mockMvc.perform(post("/api/v1/campaigns/" + completedCampaign.getId() + "/archive")
                            .header(HttpHeaders.AUTHORIZATION, adminAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ARCHIVED")));

            assertEquals(CampaignStatus.ARCHIVED, campaignRepository.findById(completedCampaign.getId()).orElseThrow().getStatus());
        }

        @Test
        @DisplayName("Only ROLE_ADMIN can archive a CANCELLED campaign; CAMPAIGN_OFFICER is denied 403")
        void campaignOfficerCannotArchiveCancelledCampaign() throws Exception {
            VaccinationCampaign cancelledCampaign = campaignRepository.save(CampaignTestFixtures.campaign(
                    "CMP-ARCH-CAN", "Cancelled Campaign", "Desc",
                    vaccine, targetGroup, START_DATE, END_DATE, REG_START, REG_END,
                    CampaignStatus.CANCELLED
            ));

            String payload = """
                    {
                        "reason": "Administrative archival"
                    }
                    """;

            // CAMPAIGN_OFFICER on CANCELLED fixture is denied 403 (explicit MF02-R-005 check)
            String officerAuth = token(250L, "officer@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            mockMvc.perform(post("/api/v1/campaigns/" + cancelledCampaign.getId() + "/archive")
                            .header(HttpHeaders.AUTHORIZATION, officerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());

            // Still CANCELLED
            assertEquals(CampaignStatus.CANCELLED, campaignRepository.findById(cancelledCampaign.getId()).orElseThrow().getStatus());

            // ROLE_ADMIN succeeds 200 OK
            String adminAuth = token(100L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            mockMvc.perform(post("/api/v1/campaigns/" + cancelledCampaign.getId() + "/archive")
                            .header(HttpHeaders.AUTHORIZATION, adminAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ARCHIVED")));

            assertEquals(CampaignStatus.ARCHIVED, campaignRepository.findById(cancelledCampaign.getId()).orElseThrow().getStatus());
        }

        @Test
        @DisplayName("Anonymous access to Complete, Cancel, Archive is denied 403")
        void anonymousTerminalActionsDenied() throws Exception {
            String payload = """
                    {
                        "reason": "Unauthorized attempt"
                    }
                    """;

            mockMvc.perform(post("/api/v1/campaigns/" + activeCampaign.getId() + "/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/archive")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN access to Complete, Cancel, Archive is denied 403")
        void citizenTerminalActionsDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            String payload = """
                    {
                        "reason": "Citizen attempt"
                    }
                    """;

            mockMvc.perform(post("/api/v1/campaigns/" + activeCampaign.getId() + "/complete")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/cancel")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/archive")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 5. CAMPAIGN CONFIGURATION CONTROLLER AUTHORIZATION
    // =========================================================================

    @Nested
    @DisplayName("Campaign Configuration Authorization")
    class CampaignConfigurationAuthorization {

        @Test
        @DisplayName("Anonymous POST dose-programmes is denied 403")
        void anonymousDoseProgrammeDenied() throws Exception {
            String payload = """
                    {
                        "doseCategory": "PRIMARY",
                        "permittedDoseNumbers": "1",
                        "minimumIntervalDays": 21,
                        "isPreviousEvidenceRequired": false,
                        "expectedServiceMinutes": 10,
                        "activeFrom": "2026-11-01"
                    }
                    """;

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/dose-programmes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_INVENTORY_OFFICER POST center-assignments is denied 403")
        void wrongRoleCenterAssignmentDenied() throws Exception {
            String auth = token(301L, "inventory@portal.gov", List.of("ROLE_INVENTORY_OFFICER"));
            String payload = String.format("""
                    {
                        "centerId": %d,
                        "activeFrom": "2026-11-01",
                        "activeTo": "2026-11-30"
                    }
                    """, center.getId());

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/center-assignments")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER POST center-assignments succeeds 201")
        void campaignOfficerCenterAssignmentSucceeds() throws Exception {
            String auth = token(102L, "officer@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            String payload = String.format("""
                    {
                        "centerId": %d,
                        "activeFrom": "2026-11-01",
                        "activeTo": "2026-11-30"
                    }
                    """, center.getId());

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/center-assignments")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.centerId", is(center.getId().intValue())));
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER POST questionnaire-assignments is denied 403")
        void campaignOfficerQuestionnaireAssignmentDenied() throws Exception {
            String auth = token(102L, "officer@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            String payload = String.format("""
                    {
                        "questionnaireVersionId": %d,
                        "effectiveStartDate": "%s"
                    }
                    """, publishedVersion.getId(), START_DATE);

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous POST questionnaire-assignments is denied 403")
        void anonymousQuestionnaireAssignmentDenied() throws Exception {
            String payload = String.format("""
                    {
                        "questionnaireVersionId": %d,
                        "effectiveStartDate": "%s"
                    }
                    """, publishedVersion.getId(), START_DATE);

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER POST questionnaire-assignments succeeds 201")
        void healthcareOfficerQuestionnaireAssignmentSucceeds() throws Exception {
            String auth = token(104L, "hco@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            String payload = String.format("""
                    {
                        "questionnaireVersionId": %d,
                        "effectiveStartDate": "%s"
                    }
                    """, publishedVersion.getId(), START_DATE);

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.questionnaireVersionId", is(publishedVersion.getId().intValue())));
        }

        @Test
        @DisplayName("ROLE_ADMIN POST questionnaire-assignments is denied 403")
        void adminQuestionnaireAssignmentDenied() throws Exception {
            String auth = token(101L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            String payload = String.format("""
                    {
                        "questionnaireVersionId": %d,
                        "effectiveStartDate": "%s"
                    }
                    """, publishedVersion.getId(), START_DATE);

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_SUPER_ADMIN POST questionnaire-assignments is denied 403")
        void superAdminQuestionnaireAssignmentDenied() throws Exception {
            String auth = token(110L, "superadmin@portal.gov", List.of("ROLE_SUPER_ADMIN"));
            String payload = String.format("""
                    {
                        "questionnaireVersionId": %d,
                        "effectiveStartDate": "%s"
                    }
                    """, publishedVersion.getId(), START_DATE);

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER POST questionnaire-assignments is denied 403")
        void centerManagerQuestionnaireAssignmentDenied() throws Exception {
            String auth = token(103L, "center_mgr@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            String payload = String.format("""
                    {
                        "questionnaireVersionId": %d,
                        "effectiveStartDate": "%s"
                    }
                    """, publishedVersion.getId(), START_DATE);

            mockMvc.perform(post("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER, ROLE_CAMPAIGN_OFFICER, ROLE_ADMIN, and ROLE_SUPER_ADMIN can GET questionnaire-assignments 200")
        void authorizedRolesGetQuestionnaireAssignmentsAllowed() throws Exception {
            String hcoAuth = token(104L, "hco@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .header(HttpHeaders.AUTHORIZATION, hcoAuth))
                    .andExpect(status().isOk());

            String coAuth = token(102L, "co@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .header(HttpHeaders.AUTHORIZATION, coAuth))
                    .andExpect(status().isOk());

            String adminAuth = token(101L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .header(HttpHeaders.AUTHORIZATION, adminAuth))
                    .andExpect(status().isOk());

            String superAdminAuth = token(110L, "superadmin@portal.gov", List.of("ROLE_SUPER_ADMIN"));
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .header(HttpHeaders.AUTHORIZATION, superAdminAuth))
                    .andExpect(status().isOk());

            String mgmtAuth = token(105L, "mgmt@portal.gov", List.of("ROLE_MANAGEMENT_USER"));
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .header(HttpHeaders.AUTHORIZATION, mgmtAuth))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Anonymous and ROLE_CITIZEN GET questionnaire-assignments is denied 403")
        void unauthorizedRolesGetQuestionnaireAssignmentsDenied() throws Exception {
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments"))
                    .andExpect(status().isForbidden());

            String citizenAuth = token(107L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            mockMvc.perform(get("/api/v1/campaigns/" + draftCampaign.getId() + "/questionnaire-assignments")
                            .header(HttpHeaders.AUTHORIZATION, citizenAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Campaign Update Authorization")
    class CampaignUpdateAuthorization {

        private String validUpdatePayload() {
            return String.format("""
                    {
                        "name": "Authorized Update Name",
                        "description": "Authorized Description",
                        "serviceStartDate": "%s",
                        "serviceEndDate": "%s",
                        "bookingOpenAt": "%s",
                        "bookingCloseAt": "%s"
                    }
                    """,
                    START_DATE,
                    END_DATE,
                    REG_START,
                    REG_END
            );
        }

        @Test
        @DisplayName("ROLE_ADMIN PUT update is authorized 200")
        void adminUpdateAuthorized() throws Exception {
            String auth = token(101L, "admin@portal.gov", List.of("ROLE_ADMIN"));

            mockMvc.perform(put("/api/v1/campaigns/" + draftCampaign.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUpdatePayload()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Authorized Update Name")));
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER PUT update is authorized 200")
        void campaignOfficerUpdateAuthorized() throws Exception {
            String auth = token(102L, "officer@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));

            mockMvc.perform(put("/api/v1/campaigns/" + draftCampaign.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUpdatePayload()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Authorized Update Name")));
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER PUT update is denied 403")
        void centerManagerUpdateDenied() throws Exception {
            String auth = token(103L, "center_mgr@portal.gov", List.of("ROLE_CENTER_MANAGER"));

            mockMvc.perform(put("/api/v1/campaigns/" + draftCampaign.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUpdatePayload()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER PUT update is denied 403")
        void healthcareOfficerUpdateDenied() throws Exception {
            String auth = token(104L, "health_officer@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));

            mockMvc.perform(put("/api/v1/campaigns/" + draftCampaign.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUpdatePayload()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_MANAGEMENT_USER PUT update is denied 403")
        void managementUserUpdateDenied() throws Exception {
            String auth = token(105L, "mgmt@portal.gov", List.of("ROLE_MANAGEMENT_USER"));

            mockMvc.perform(put("/api/v1/campaigns/" + draftCampaign.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUpdatePayload()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_INVENTORY_OFFICER PUT update is denied 403")
        void inventoryOfficerUpdateDenied() throws Exception {
            String auth = token(106L, "inventory@portal.gov", List.of("ROLE_INVENTORY_OFFICER"));

            mockMvc.perform(put("/api/v1/campaigns/" + draftCampaign.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUpdatePayload()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN PUT update is denied 403")
        void citizenUpdateDenied() throws Exception {
            String auth = token(107L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            mockMvc.perform(put("/api/v1/campaigns/" + draftCampaign.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUpdatePayload()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous PUT update is denied 403")
        void anonymousUpdateDenied() throws Exception {
            mockMvc.perform(put("/api/v1/campaigns/" + draftCampaign.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validUpdatePayload()))
                    .andExpect(status().isForbidden());
        }
    }
}
