package com.se2030.vaccinationportal.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StaffRequestReviewControllerTests {

    private static final String RAW_PASSWORD = "ReviewPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserAccount administrator;
    private UserAccount citizen;
    private UserAccount nonAdminStaff;
    private UserAccount pending;
    private Role requestedRole;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.saveAll(List.of(
                new Role("ROLE_ADMIN", "System Administrator"),
                new Role("ROLE_CENTER_MANAGER", "Vaccination Center Manager"),
                new Role("ROLE_HEALTHCARE_OFFICER", "Healthcare Officer"),
                new Role("ROLE_CAMPAIGN_OFFICER", "Campaign Officer"),
                new Role("ROLE_INVENTORY_OFFICER", "Inventory Officer"),
                new Role("ROLE_CITIZEN", "Citizen"),
                new Role("ROLE_MANAGEMENT_USER", "Management User")
        ));
        requestedRole = roleRepository.findByCode("ROLE_HEALTHCARE_OFFICER").orElseThrow();
        administrator = saveActive("admin@example.com", "ROLE_ADMIN");
        citizen = saveActive("citizen@example.com", "ROLE_CITIZEN");
        nonAdminStaff = saveActive("staff@example.com", "ROLE_CENTER_MANAGER");
        pending = savePending("pending-review@example.com");
    }

    @Test
    @DisplayName("administrator can list and inspect pending staff requests without password data")
    void listAndDetail_asAdministrator_returnsSafePendingData() throws Exception {
        mockMvc.perform(get("/api/v1/admin/staff-requests").header("Authorization", bearer(administrator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(pending.getId().intValue())))
                .andExpect(jsonPath("$[0].accountStatus", is("PENDING_APPROVAL")))
                .andExpect(jsonPath("$[0].requestedRoleCode", is("ROLE_HEALTHCARE_OFFICER")))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/staff-requests/{id}", pending.getId())
                        .header("Authorization", bearer(administrator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("pending-review@example.com")))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("anonymous caller receives 401 for staff request review")
    void list_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/staff-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("citizen and non-admin staff receive 403 for staff request review")
    void list_nonAdministrators_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/staff-requests").header("Authorization", bearer(citizen)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/staff-requests").header("Authorization", bearer(nonAdminStaff)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrator approval ignores client reviewer identity and records authenticated principal")
    void approve_asAdministrator_usesAuthenticatedReviewer() throws Exception {
        String payload = """
                {"roleCode":"ROLE_CENTER_MANAGER","reviewerId":999999}
                """;

        mockMvc.perform(post("/api/v1/admin/staff-requests/{id}/approve", pending.getId())
                        .header("Authorization", bearer(administrator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.roles[0]", is("ROLE_CENTER_MANAGER")))
                .andExpect(jsonPath("$.reviewedByUserId", is(administrator.getId().intValue())));
    }

    @Test
    @DisplayName("non-admin callers cannot approve or reject staff requests")
    void review_nonAdministrator_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/staff-requests/{id}/approve", pending.getId())
                        .header("Authorization", bearer(nonAdminStaff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCode\":\"ROLE_CENTER_MANAGER\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/staff-requests/{id}/reject", pending.getId())
                        .header("Authorization", bearer(citizen))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Not eligible\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("invalid approval role returns stable 400 error")
    void approve_adminRole_returnsStable400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/staff-requests/{id}/approve", pending.getId())
                        .header("Authorization", bearer(administrator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCode\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_APPROVAL_ROLE")));
    }

    @Test
    @DisplayName("double review returns stable 409 error")
    void approve_alreadyReviewed_returnsStable409() throws Exception {
        String approval = objectMapper.writeValueAsString(java.util.Map.of("roleCode", "ROLE_CENTER_MANAGER"));
        mockMvc.perform(post("/api/v1/admin/staff-requests/{id}/approve", pending.getId())
                        .header("Authorization", bearer(administrator))
                        .contentType(MediaType.APPLICATION_JSON).content(approval))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/staff-requests/{id}/approve", pending.getId())
                        .header("Authorization", bearer(administrator))
                        .contentType(MediaType.APPLICATION_JSON).content(approval))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("STAFF_REQUEST_NOT_PENDING")));
    }

    @Test
    @DisplayName("blank rejection reason returns validation error and valid rejection persists trimmed reason")
    void reject_validatesAndPersistsReason() throws Exception {
        mockMvc.perform(post("/api/v1/admin/staff-requests/{id}/reject", pending.getId())
                        .header("Authorization", bearer(administrator))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        mockMvc.perform(post("/api/v1/admin/staff-requests/{id}/reject", pending.getId())
                        .header("Authorization", bearer(administrator))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"  Documents invalid  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus", is("REJECTED")))
                .andExpect(jsonPath("$.rejectionReason", is("Documents invalid")));
    }

    @Test
    @DisplayName("unknown staff request returns stable 404 error")
    void detail_unknownRequest_returnsStable404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/staff-requests/{id}", 999999)
                        .header("Authorization", bearer(administrator)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("STAFF_REQUEST_NOT_FOUND")));
    }

    private UserAccount saveActive(String email, String roleCode) {
        UserAccount account = new UserAccount(email, passwordEncoder.encode(RAW_PASSWORD), AccountStatus.ACTIVE);
        account.addRole(roleRepository.findByCode(roleCode).orElseThrow());
        return userAccountRepository.saveAndFlush(account);
    }

    private UserAccount savePending(String email) {
        UserAccount account = new UserAccount(email, passwordEncoder.encode(RAW_PASSWORD), AccountStatus.PENDING_APPROVAL);
        account.setFullName("Pending Staff");
        account.setPhoneNumber("+94 77 100 2000");
        account.setRequestedRole(requestedRole);
        return userAccountRepository.saveAndFlush(account);
    }

    private String bearer(UserAccount account) {
        List<String> roles = account.getRoles().stream().map(Role::getCode).toList();
        return "Bearer " + jwtTokenService.generateToken(account.getId(), account.getEmail(), roles);
    }
}
