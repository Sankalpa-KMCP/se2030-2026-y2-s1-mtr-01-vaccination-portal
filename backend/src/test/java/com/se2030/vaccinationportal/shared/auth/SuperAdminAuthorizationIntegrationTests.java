package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.core.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.core.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.core.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.session.planning.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;
import com.se2030.vaccinationportal.shared.auth.dto.StaffApprovalRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRejectionRequest;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SuperAdminAuthorizationIntegrationTests {

    private static final String PASSWORD = "SuperAdminPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;

    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private OperationalStaffRosterRepository staffRosterRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private SessionResourceAssignmentRepository resourceAssignmentRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private SessionCapacityPlanRepository planRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserAccount superAdmin;
    private UserAccount pendingStaff;
    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private VaccinationSession session;
    private VaccineType vaccine;
    private TargetGroup targetGroup;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.saveAll(List.of(
                new Role("ROLE_SUPER_ADMIN", "Super Administrator"),
                new Role("ROLE_ADMIN", "System Administrator"),
                new Role("ROLE_CITIZEN", "Citizen"),
                new Role("ROLE_CAMPAIGN_OFFICER", "Campaign Officer"),
                new Role("ROLE_CENTER_MANAGER", "Center Manager"),
                new Role("ROLE_HEALTHCARE_OFFICER", "Healthcare Officer"),
                new Role("ROLE_INVENTORY_OFFICER", "Inventory Officer"),
                new Role("ROLE_MANAGEMENT_USER", "Management User")
        ));

        superAdmin = new UserAccount("superadmin@portal.gov", passwordEncoder.encode(PASSWORD), AccountStatus.ACTIVE);
        superAdmin.setFullName("Super Administrator");
        superAdmin.setPhoneNumber("0779998877");
        superAdmin.addRole(roleRepository.findByCode("ROLE_SUPER_ADMIN").orElseThrow());
        userAccountRepository.saveAndFlush(superAdmin);

        Role hcRole = roleRepository.findByCode("ROLE_HEALTHCARE_OFFICER").orElseThrow();
        pendingStaff = new UserAccount("applicant@portal.gov", passwordEncoder.encode(PASSWORD), AccountStatus.PENDING_APPROVAL);
        pendingStaff.setFullName("Pending Healthcare Officer");
        pendingStaff.setPhoneNumber("0771234567");
        pendingStaff.setRequestedRole(hcRole);
        userAccountRepository.saveAndFlush(pendingStaff);

        vaccine = vaccineTypeRepository.save(new VaccineType("VAX-SA-01", "SuperAdmin Vaccine"));
        targetGroup = targetGroupRepository.save(new TargetGroup("TG-SA-01", "SuperAdmin Target Group"));

        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-SA-01", "SuperAdmin Campaign", "SuperAdmin Campaign Desc",
                vaccine, targetGroup,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-12-31T00:00:00Z"),
                CampaignStatus.ACTIVE
        ));

        center = centerRepository.save(new VaccinationCenter(
                "CTR-SA-01", "SuperAdmin Center", "100 Capital Road", "center@health.gov",
                CenterStatus.ACTIVE, 60, 100, "UTC"
        ));

        Instant sessionStart = Instant.parse("2026-11-01T08:00:00Z");
        Instant sessionEnd = Instant.parse("2026-11-01T12:00:00Z"); // 240 minutes

        session = sessionRepository.save(new VaccinationSession(
                campaign, center, sessionStart, sessionEnd, Instant.parse("2026-11-01T07:00:00Z"), "UTC"
        ));

        doseProgrammeRepository.save(new CampaignDoseProgramme(
                campaign, 1, DoseCategory.PRIMARY, "1", 21, false, 10,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31)
        ));

        // 2 active vaccinators, 2 active stations -> recommended throughput = 2 * (240 / 10) = 48
        OperationalStaffRoster r1 = staffRosterRepository.save(new OperationalStaffRoster(center, 301L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE));
        OperationalStaffRoster r2 = staffRosterRepository.save(new OperationalStaffRoster(center, 302L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE));
        staffAssignmentRepository.save(new StaffAssignment(session, r1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        staffAssignmentRepository.save(new StaffAssignment(session, r2, StaffRole.VACCINATOR, sessionStart, sessionEnd));

        OperationalResource s1 = resourceRepository.save(new OperationalResource(center, "ST-SA-1", ResourceType.VACCINATION_STATION, "Station 1", OperationalResourceStatus.ACTIVE));
        OperationalResource s2 = resourceRepository.save(new OperationalResource(center, "ST-SA-2", ResourceType.VACCINATION_STATION, "Station 2", OperationalResourceStatus.ACTIVE));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, s1, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, s2, sessionStart, sessionEnd));
    }

    private String superAdminToken() {
        return "Bearer " + jwtTokenService.generateToken(superAdmin.getId(), superAdmin.getEmail(), List.of("ROLE_SUPER_ADMIN"));
    }

    // =========================================================================
    // 1. STAFF REQUEST ADMINISTRATION
    // =========================================================================

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can list pending staff requests")
    void superAdmin_canListPendingStaffRequests() throws Exception {
        mockMvc.perform(get("/api/v1/admin/staff-requests")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("applicant@portal.gov"));
    }

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can approve a pending staff request")
    void superAdmin_canApproveStaffRequest() throws Exception {
        StaffApprovalRequest request = new StaffApprovalRequest("ROLE_HEALTHCARE_OFFICER");

        mockMvc.perform(post("/api/v1/admin/staff-requests/" + pendingStaff.getId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_HEALTHCARE_OFFICER"));

        UserAccount approved = userAccountRepository.findById(pendingStaff.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, approved.getAccountStatus());
        assertEquals(superAdmin.getId(), approved.getReviewedBy().getId());
    }

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can reject a pending staff request")
    void superAdmin_canRejectStaffRequest() throws Exception {
        StaffRejectionRequest request = new StaffRejectionRequest("Verification documentation incomplete.");

        mockMvc.perform(post("/api/v1/admin/staff-requests/" + pendingStaff.getId() + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("REJECTED"));

        UserAccount rejected = userAccountRepository.findById(pendingStaff.getId()).orElseThrow();
        assertEquals(AccountStatus.REJECTED, rejected.getAccountStatus());
    }

    // =========================================================================
    // 2. CAMPAIGN MANAGEMENT
    // =========================================================================

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can list campaigns")
    void superAdmin_canListCampaigns() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CMP-SA-01"));
    }

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can archive a completed campaign")
    void superAdmin_canArchiveCampaign() throws Exception {
        CampaignTestFixtures.forceStatus(campaign, CampaignStatus.COMPLETED);
        campaignRepository.saveAndFlush(campaign);

        String payload = "{\"reason\":\"Super Admin executive archival\"}";

        mockMvc.perform(post("/api/v1/campaigns/" + campaign.getId() + "/archive")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    // =========================================================================
    // 3. CENTER OPERATIONS
    // =========================================================================

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can list vaccination centers")
    void superAdmin_canListCenters() throws Exception {
        mockMvc.perform(get("/api/v1/centers")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CTR-SA-01"));
    }

    // =========================================================================
    // 4. SESSION MANAGEMENT & CAPACITY OVERRIDE
    // =========================================================================

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can suspend and resume booking intake for a session")
    void superAdmin_canSuspendAndResumeSessionBooking() throws Exception {
        session.schedule();
        session = sessionRepository.save(session);

        String suspendPayload = "{\"reason\":\"Adverse weather condition\"}";

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/booking-suspend")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(suspendPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingSuspended").value(true));

        String resumePayload = "{\"reason\":\"Weather condition cleared\"}";

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/booking-resume")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resumePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingSuspended").value(false));
    }

    @Test
    @DisplayName("ROLE_SUPER_ADMIN is explicitly authorized to approve session capacity override above recommended throughput")
    void superAdmin_canApproveSessionCapacityOverride() throws Exception {
        // Recommended throughput is 48. We approve 60 (> 48).
        String payload = """
                {
                    "fullyEquippedStationCount": 2,
                    "approvedCapacity": 60,
                    "overrideReason": "Super Admin executive surge override approval"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/capacity-plans")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.revision", is(1)))
                .andExpect(jsonPath("$.approvedCapacity", is(60)))
                .andExpect(jsonPath("$.overrideReason", is("Super Admin executive surge override approval")))
                .andExpect(jsonPath("$.approvedBy", is(String.valueOf(superAdmin.getId()))));

        SessionCapacityPlan plan = planRepository.findTopBySessionIdOrderByRevisionDesc(session.getId()).orElseThrow();
        assertEquals(String.valueOf(superAdmin.getId()), plan.getApprovedBy());
        assertEquals(60, plan.getApprovedCapacity());
    }

    // =========================================================================
    // 5. OPERATIONAL ANALYTICS
    // =========================================================================

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can access operational analytics")
    void superAdmin_canAccessOperationalAnalytics() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/operations")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken()))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 6. CLINICAL SCREENING OPERATIONS
    // =========================================================================

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can access screening review queue")
    void superAdmin_canAccessScreeningReviews() throws Exception {
        mockMvc.perform(get("/api/v1/screening/assessments/queue")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can access screening questions catalogue")
    void superAdmin_canAccessScreeningQuestions() throws Exception {
        mockMvc.perform(get("/api/v1/screening/questions")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can access questionnaires catalogue")
    void superAdmin_canAccessQuestionnaires() throws Exception {
        mockMvc.perform(get("/api/v1/screening/questionnaires")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken()))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 7. VACCINATION OPERATIONAL ENDPOINTS
    // =========================================================================

    @Test
    @DisplayName("ROLE_SUPER_ADMIN can access vaccination draft operational endpoints")
    void superAdmin_canAccessVaccinationDrafts() throws Exception {
        mockMvc.perform(get("/api/v1/vaccination/drafts")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
