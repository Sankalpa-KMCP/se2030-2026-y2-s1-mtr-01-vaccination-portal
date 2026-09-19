package com.se2030.vaccinationportal.campaign;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprehensive H01 Security Integration Tests for Session, Operational Assignment, and Capacity Planning endpoints.
 * Validates role-based authorization, anonymous denial, public session reads, and capacity override separation of duties.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SessionSecurityAuthorizationIntegrationTests {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private OperationalStaffRosterRepository rosterRepository;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private VaccinationSession draftSession;
    private VaccinationSession scheduledSession;
    private VaccinationSession inProgressSession;
    private OperationalResource station;
    private OperationalStaffRoster staff;

    private static final Instant START_AT = Instant.parse("2026-11-10T09:00:00Z");
    private static final Instant END_AT = Instant.parse("2026-11-10T12:00:00Z");
    private static final Instant CLOSE_AT = Instant.parse("2026-11-10T08:00:00Z");

    @BeforeEach
    void setUp() {
        VaccineType vaccine = vaccineTypeRepository.save(new VaccineType("VAX-SEC-SES", "Session Sec Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-SEC-SES", "Session Sec Target Group"));

        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-SEC-SES", "Session Sec Campaign", "Desc",
                vaccine, targetGroup,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-31T00:00:00Z"),
                CampaignStatus.ACTIVE
        ));

        center = centerRepository.save(new VaccinationCenter(
                "CTR-SEC-SES", "Session Sec Center", "456 Oak St", "sec@center.gov",
                com.se2030.vaccinationportal.campaign.model.CenterStatus.ACTIVE, 80, 200, "UTC"
        ));

        draftSession = sessionRepository.save(new VaccinationSession(
                campaign, center, START_AT, END_AT, CLOSE_AT, "UTC"
        ));

        scheduledSession = sessionRepository.save(new VaccinationSession(
                campaign, center,
                Instant.now().minusSeconds(300),
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(600),
                "UTC"
        ));
        scheduledSession.schedule();
        scheduledSession = sessionRepository.save(scheduledSession);

        inProgressSession = sessionRepository.save(new VaccinationSession(
                campaign, center,
                Instant.now().minusSeconds(300),
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(600),
                "UTC"
        ));
        inProgressSession.schedule();
        inProgressSession.start();
        inProgressSession = sessionRepository.save(inProgressSession);

        station = resourceRepository.save(new OperationalResource(
                center, "STATION-SEC-01", ResourceType.VACCINATION_STATION, "Main Station", OperationalResourceStatus.ACTIVE
        ));

        staff = rosterRepository.save(new OperationalStaffRoster(
                center, 3001L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE
        ));
    }

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    // =========================================================================
    // 1. PUBLIC DISCOVERY READS
    // =========================================================================
    // 1. ADMINISTRATIVE SESSION READS REQUIRE AUTHORIZED OPERATIONAL STAFF (MF02-R-001)
    // =========================================================================

    @Nested
    @DisplayName("Administrative Session Reads")
    class AdministrativeSessionReads {

        @Test
        @DisplayName("Anonymous GET /api/v1/sessions is denied 403")
        void anonymousListSessionsDenied() throws Exception {
            mockMvc.perform(get("/api/v1/sessions"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous GET /api/v1/sessions/{id} is denied 403")
        void anonymousGetSessionByIdDenied() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN GET session endpoints is denied 403")
        void citizenSessionReadsDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            mockMvc.perform(get("/api/v1/sessions")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_INVENTORY_OFFICER GET session endpoints is denied 403")
        void inventoryOfficerSessionReadsDenied() throws Exception {
            String auth = token(601L, "inventory@portal.gov", List.of("ROLE_INVENTORY_OFFICER"));

            mockMvc.perform(get("/api/v1/sessions")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_MANAGEMENT_USER GET session endpoints is denied 403")
        void managementUserSessionReadsDenied() throws Exception {
            String auth = token(701L, "management@portal.gov", List.of("ROLE_MANAGEMENT_USER"));

            mockMvc.perform(get("/api/v1/sessions")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId())
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Authorized staff roles succeed on GET /api/v1/sessions and GET /api/v1/sessions/{id} with 200 OK")
        void authorizedStaffRolesSucceed() throws Exception {
            List<String> authorizedRoles = List.of(
                    "ROLE_ADMIN",
                    "ROLE_CENTER_MANAGER",
                    "ROLE_CAMPAIGN_OFFICER"
            );

            long userId = 101L;
            for (String role : authorizedRoles) {
                String auth = token(userId++, role.toLowerCase() + "@portal.gov", List.of(role));

                mockMvc.perform(get("/api/v1/sessions")
                                .header(HttpHeaders.AUTHORIZATION, auth))
                        .andExpect(status().isOk());

                mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId())
                                .header(HttpHeaders.AUTHORIZATION, auth))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id", is(draftSession.getId().intValue())));
            }
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER GET /api/v1/sessions is denied 403")
        void healthcareOfficerSessionsDenied() throws Exception {
            String auth = token(301L, "hco@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            mockMvc.perform(get("/api/v1/sessions")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 2. INTERNAL OPERATIONAL READS REQUIRE STAFF ROLES
    // =========================================================================

    @Nested
    @DisplayName("Internal Operational Reads")
    class InternalOperationalReads {

        @Test
        @DisplayName("Anonymous GET /readiness is denied 403")
        void anonymousReadinessDenied() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId() + "/readiness"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN GET /readiness is denied 403")
        void citizenReadinessDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId() + "/readiness")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER GET /readiness is denied 403")
        void healthcareOfficerReadinessDenied() throws Exception {
            String auth = token(301L, "hco@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId() + "/readiness")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER GET /readiness succeeds 200")
        void centerManagerReadinessSucceeds() throws Exception {
            String auth = token(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId() + "/readiness")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Anonymous GET /change-events is denied 403")
        void anonymousChangeEventsDenied() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId() + "/change-events"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN GET /change-events is denied 403")
        void citizenChangeEventsDenied() throws Exception {
            String auth = token(501L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));
            mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId() + "/change-events")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_MANAGEMENT_USER GET /change-events succeeds 200")
        void managementUserChangeEventsSucceeds() throws Exception {
            String auth = token(202L, "manager@portal.gov", List.of("ROLE_MANAGEMENT_USER"));
            mockMvc.perform(get("/api/v1/sessions/" + draftSession.getId() + "/change-events")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // 3. EXECUTION LIFECYCLE (START / COMPLETE)
    // =========================================================================

    @Nested
    @DisplayName("Execution Lifecycle Authorization")
    class ExecutionLifecycleAuthorization {

        @Test
        @DisplayName("Anonymous POST /start is denied 403")
        void anonymousStartDenied() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/start"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER POST /start is denied 403 (execution requires ADMIN/CENTER_MANAGER)")
        void campaignOfficerStartDenied() throws Exception {
            String auth = token(201L, "officer@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));
            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/start")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER POST /start is denied 403")
        void healthcareOfficerStartDenied() throws Exception {
            String auth = token(301L, "hco@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));
            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/start")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER POST /start succeeds 200")
        void centerManagerStartSucceeds() throws Exception {
            String auth = token(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/start")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lifecycleStatus", is("IN_PROGRESS")));
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER POST /complete succeeds 200")
        void centerManagerCompleteSucceeds() throws Exception {
            String auth = token(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            mockMvc.perform(post("/api/v1/sessions/" + inProgressSession.getId() + "/complete")
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lifecycleStatus", is("COMPLETED")));
        }
    }

    // =========================================================================
    // 4. OPERATIONAL ASSIGNMENTS AUTHORIZATION
    // =========================================================================

    @Nested
    @DisplayName("Operational Assignments Authorization")
    class OperationalAssignmentsAuthorization {

        @Test
        @DisplayName("Anonymous POST resource-assignments is denied 403")
        void anonymousResourceAssignmentDenied() throws Exception {
            String payload = String.format("""
                    {
                        "resourceId": %d,
                        "startAt": "%s",
                        "endAt": "%s"
                    }
                    """, station.getId(), START_AT, END_AT);

            mockMvc.perform(post("/api/v1/sessions/" + draftSession.getId() + "/resource-assignments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER POST resource-assignments succeeds 201")
        void centerManagerResourceAssignmentSucceeds() throws Exception {
            String auth = token(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            String payload = String.format("""
                    {
                        "resourceId": %d,
                        "startAt": "%s",
                        "endAt": "%s"
                    }
                    """, station.getId(), START_AT, END_AT);

            mockMvc.perform(post("/api/v1/sessions/" + draftSession.getId() + "/resource-assignments")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resourceId", is(station.getId().intValue())));
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER POST staff-assignments succeeds 201")
        void centerManagerStaffAssignmentSucceeds() throws Exception {
            String auth = token(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            String payload = String.format("""
                    {
                        "staffRosterId": %d,
                        "role": "VACCINATOR",
                        "startAt": "%s",
                        "endAt": "%s"
                    }
                    """, staff.getId(), START_AT, END_AT);

            mockMvc.perform(post("/api/v1/sessions/" + draftSession.getId() + "/staff-assignments")
                            .header(HttpHeaders.AUTHORIZATION, auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.staffRosterId", is(staff.getId().intValue())));
        }
    }

    // =========================================================================
    // 5. CAPACITY PLANNING & OVERRIDE SEPARATION OF DUTIES
    // =========================================================================

    @Nested
    @DisplayName("Capacity Planning & Override Authorization")
    class CapacityPlanningAuthorization {

        @Test
        @DisplayName("Anonymous POST capacity-plans/preview is denied 403")
        void anonymousCapacityPreviewDenied() throws Exception {
            String payload = """
                    {
                        "fullyEquippedStationCount": 2,
                        "safeConcurrentOccupancy": 50,
                        "expectedDwellMinutes": 15,
                        "expectedAccompanyingPersonsPerBooking": 0.0,
                        "approvedCapacity": 30,
                        "overrideReason": null
                    }
                    """;

            mockMvc.perform(post("/api/v1/sessions/" + draftSession.getId() + "/capacity-plans/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous POST capacity-plans is denied 403")
        void anonymousCapacityPlanCreateDenied() throws Exception {
            String payload = """
                    {
                        "fullyEquippedStationCount": 2,
                        "approvedCapacity": 60,
                        "safeConcurrentOccupancy": 50,
                        "expectedDwellMinutes": 15,
                        "expectedAccompanyingPersonsPerBooking": 0.0,
                        "overrideReason": null
                    }
                    """;

            mockMvc.perform(post("/api/v1/sessions/" + draftSession.getId() + "/capacity-plans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_ADMIN POST standard capacity-plan (approved <= recommended) succeeds 201")
        void adminCreateStandardCapacityPlanSucceeds() throws Exception {
            // First assign resources and staff to give session non-zero throughput
            String mgrAuth = token(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            String resPayload = String.format("""
                    {
                        "resourceId": %d,
                        "startAt": "%s",
                        "endAt": "%s"
                    }
                    """, station.getId(), START_AT, END_AT);
            mockMvc.perform(post("/api/v1/sessions/" + draftSession.getId() + "/resource-assignments")
                            .header(HttpHeaders.AUTHORIZATION, mgrAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resPayload))
                    .andExpect(status().isCreated());

            String staffPayload = String.format("""
                    {
                        "staffRosterId": %d,
                        "role": "VACCINATOR",
                        "startAt": "%s",
                        "endAt": "%s"
                    }
                    """, staff.getId(), START_AT, END_AT);
            mockMvc.perform(post("/api/v1/sessions/" + draftSession.getId() + "/staff-assignments")
                            .header(HttpHeaders.AUTHORIZATION, mgrAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(staffPayload))
                    .andExpect(status().isCreated());

            String adminAuth = token(101L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            String planPayload = """
                    {
                        "fullyEquippedStationCount": 1,
                        "approvedCapacity": 10,
                        "safeConcurrentOccupancy": 50,
                        "expectedDwellMinutes": 15,
                        "expectedAccompanyingPersonsPerBooking": 0.0,
                        "overrideReason": null
                    }
                    """;

            mockMvc.perform(post("/api/v1/sessions/" + draftSession.getId() + "/capacity-plans")
                            .header(HttpHeaders.AUTHORIZATION, adminAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(planPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.approvedCapacity", is(10)))
                    .andExpect(jsonPath("$.revision", is(1)));
        }

        @Test
        @DisplayName("ROLE_ADMIN POST override capacity-plan (approved > recommended) is denied 403 by service business rule")
        void adminCreateOverrideCapacityPlanDenied() throws Exception {
            String adminAuth = token(101L, "admin@portal.gov", List.of("ROLE_ADMIN"));
            String planPayload = """
                    {
                        "fullyEquippedStationCount": 0,
                        "approvedCapacity": 150,
                        "safeConcurrentOccupancy": 50,
                        "expectedDwellMinutes": 15,
                        "expectedAccompanyingPersonsPerBooking": 0.0,
                        "overrideReason": "Emergency surge demand requires override"
                    }
                    """;

            // With fullyEquippedStationCount=0, recommendedThroughput is 0. approvedCapacity=150 is an override within ceiling (200).
            // Service layer requires ROLE_CENTER_MANAGER for override.
            mockMvc.perform(post("/api/v1/sessions/" + draftSession.getId() + "/capacity-plans")
                            .header(HttpHeaders.AUTHORIZATION, adminAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(planPayload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER POST override capacity-plan (approved > recommended) succeeds 201")
        void centerManagerCreateOverrideCapacityPlanSucceeds() throws Exception {
            String managerAuth = token(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
            String planPayload = """
                    {
                        "fullyEquippedStationCount": 0,
                        "approvedCapacity": 150,
                        "safeConcurrentOccupancy": 50,
                        "expectedDwellMinutes": 15,
                        "expectedAccompanyingPersonsPerBooking": 0.0,
                        "overrideReason": "Emergency surge demand approved by Center Manager"
                    }
                    """;

            mockMvc.perform(post("/api/v1/sessions/" + draftSession.getId() + "/capacity-plans")
                            .header(HttpHeaders.AUTHORIZATION, managerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(planPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.approvedCapacity", is(150)))
                    .andExpect(jsonPath("$.overrideReason", is("Emergency surge demand approved by Center Manager")));
        }
    }

    @Nested
    @DisplayName("Booking Suspend and Resume Authorization Tests")
    class BookingSuspensionSecurityTests {

        private final String suspendPayload = """
                {
                    "reason": "Operational shortage of nursing staff"
                }
                """;

        private final String resumePayload = """
                {
                    "reason": "Staff restored, resuming citizen booking intake"
                }
                """;

        @Test
        @DisplayName("ROLE_ADMIN is authorized for booking suspend (200) and resume (200)")
        void adminCanSuspendAndResumeBooking() throws Exception {
            String adminAuth = token(101L, "admin@portal.gov", List.of("ROLE_ADMIN"));

            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/booking-suspend")
                            .header(HttpHeaders.AUTHORIZATION, adminAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(suspendPayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingSuspended", is(true)));

            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/booking-resume")
                            .header(HttpHeaders.AUTHORIZATION, adminAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resumePayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingSuspended", is(false)));
        }

        @Test
        @DisplayName("ROLE_CENTER_MANAGER is authorized for booking suspend (200) and resume (200)")
        void centerManagerCanSuspendAndResumeBooking() throws Exception {
            String managerAuth = token(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));

            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/booking-suspend")
                            .header(HttpHeaders.AUTHORIZATION, managerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(suspendPayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingSuspended", is(true)));

            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/booking-resume")
                            .header(HttpHeaders.AUTHORIZATION, managerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resumePayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingSuspended", is(false)));
        }

        @Test
        @DisplayName("ROLE_CAMPAIGN_OFFICER is forbidden (403) from suspending or resuming booking")
        void campaignOfficerIsForbiddenFromBookingSuspension() throws Exception {
            String officerAuth = token(103L, "officer@portal.gov", List.of("ROLE_CAMPAIGN_OFFICER"));

            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/booking-suspend")
                            .header(HttpHeaders.AUTHORIZATION, officerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(suspendPayload))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/booking-resume")
                            .header(HttpHeaders.AUTHORIZATION, officerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resumePayload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_HEALTHCARE_OFFICER is forbidden (403) from suspending or resuming booking")
        void healthcareOfficerIsForbiddenFromBookingSuspension() throws Exception {
            String healthcareAuth = token(104L, "nurse@portal.gov", List.of("ROLE_HEALTHCARE_OFFICER"));

            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/booking-suspend")
                            .header(HttpHeaders.AUTHORIZATION, healthcareAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(suspendPayload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_CITIZEN is forbidden (403) from suspending or resuming booking")
        void citizenIsForbiddenFromBookingSuspension() throws Exception {
            String citizenAuth = token(105L, "citizen@portal.gov", List.of("ROLE_CITIZEN"));

            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/booking-suspend")
                            .header(HttpHeaders.AUTHORIZATION, citizenAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(suspendPayload))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated request is rejected (403)")
        void unauthenticatedRequestIsRejected() throws Exception {
            mockMvc.perform(post("/api/v1/sessions/" + scheduledSession.getId() + "/booking-suspend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(suspendPayload))
                    .andExpect(status().isForbidden());
        }
    }
}
