package com.se2030.vaccinationportal.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.shared.auth.dto.StaffApprovalRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRegistrationRequest;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SuperAdminPrivilegeEscalationIntegrationTests {

    private static final String PASSWORD = "EscalationPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserAccount administrator;
    private UserAccount pendingUser;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.saveAll(List.of(
                new Role("ROLE_ADMIN", "System Administrator"),
                new Role("ROLE_SUPER_ADMIN", "Super Administrator"),
                new Role("ROLE_HEALTHCARE_OFFICER", "Healthcare Officer"),
                new Role("ROLE_CENTER_MANAGER", "Center Manager"),
                new Role("ROLE_CAMPAIGN_OFFICER", "Campaign Officer"),
                new Role("ROLE_INVENTORY_OFFICER", "Inventory Officer"),
                new Role("ROLE_CITIZEN", "Citizen"),
                new Role("ROLE_MANAGEMENT_USER", "Management User")
        ));

        administrator = new UserAccount("admin@portal.gov", passwordEncoder.encode(PASSWORD), AccountStatus.ACTIVE);
        administrator.setFullName("Standard Administrator");
        administrator.setPhoneNumber("0771112233");
        administrator.addRole(roleRepository.findByCode("ROLE_ADMIN").orElseThrow());
        userAccountRepository.saveAndFlush(administrator);

        Role healthcareRole = roleRepository.findByCode("ROLE_HEALTHCARE_OFFICER").orElseThrow();
        pendingUser = new UserAccount("applicant@portal.gov", passwordEncoder.encode(PASSWORD), AccountStatus.PENDING_APPROVAL);
        pendingUser.setFullName("Pending Applicant");
        pendingUser.setPhoneNumber("0772223344");
        pendingUser.setRequestedRole(healthcareRole);
        userAccountRepository.saveAndFlush(pendingUser);
    }

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    @Test
    @DisplayName("Public staff registration strictly rejects request for ROLE_SUPER_ADMIN")
    void publicStaffRegistration_cannotRequestSuperAdmin() throws Exception {
        StaffRegistrationRequest request = new StaffRegistrationRequest(
                "Super Admin Applicant",
                "hacker@portal.gov",
                "0773334455",
                PASSWORD,
                "ROLE_SUPER_ADMIN"
        );

        mockMvc.perform(post("/api/v1/auth/register/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUESTED_ROLE"));

        assertEquals(2, userAccountRepository.count(), "No new account should have been created");
    }

    @Test
    @DisplayName("Ordinary Admin cannot approve a staff request into ROLE_SUPER_ADMIN")
    void ordinaryAdmin_cannotApproveIntoSuperAdmin() throws Exception {
        StaffApprovalRequest request = new StaffApprovalRequest("ROLE_SUPER_ADMIN");
        String adminToken = token(administrator.getId(), administrator.getEmail(), List.of("ROLE_ADMIN"));

        mockMvc.perform(post("/api/v1/admin/staff-requests/" + pendingUser.getId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_APPROVAL_ROLE"));

        UserAccount refreshed = userAccountRepository.findById(pendingUser.getId()).orElseThrow();
        assertEquals(AccountStatus.PENDING_APPROVAL, refreshed.getAccountStatus(), "Account must remain pending");
    }

    @Test
    @DisplayName("Ordinary Admin cannot approve a staff request into SUPER_ADMIN without prefix")
    void ordinaryAdmin_cannotApproveIntoSuperAdminWithoutPrefix() throws Exception {
        StaffApprovalRequest request = new StaffApprovalRequest("SUPER_ADMIN");
        String adminToken = token(administrator.getId(), administrator.getEmail(), List.of("ROLE_ADMIN"));

        mockMvc.perform(post("/api/v1/admin/staff-requests/" + pendingUser.getId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_APPROVAL_ROLE"));
    }
}
