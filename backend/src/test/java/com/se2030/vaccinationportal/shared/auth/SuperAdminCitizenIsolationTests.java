package com.se2030.vaccinationportal.shared.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.appointment.waitlist.dto.WaitlistJoinRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SuperAdminCitizenIsolationTests {

    private static final String PASSWORD = "SuperAdminPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserAccount superAdmin;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.flush();

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
    }

    private String superAdminToken() {
        return "Bearer " + jwtTokenService.generateToken(superAdmin.getId(), superAdmin.getEmail(), List.of("ROLE_SUPER_ADMIN"));
    }

    @Test
    @DisplayName("GET /api/v1/profile/me is strictly denied 403 for ROLE_SUPER_ADMIN")
    void superAdmin_cannotAccessCitizenProfileMe() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/profile/me is strictly denied 403 for ROLE_SUPER_ADMIN")
    void superAdmin_cannotUpdateCitizenProfileMe() throws Exception {
        String json = """
                {
                    "fullName": "Super Admin Name",
                    "phoneNumber": "0779998877",
                    "dateOfBirth": "1985-05-20",
                    "gender": "MALE",
                    "residentialAddress": "123 Capital Ave"
                }
                """;

        mockMvc.perform(put("/api/v1/profile/me")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/citizen/campaigns is strictly denied 403 for ROLE_SUPER_ADMIN")
    void superAdmin_cannotAccessCitizenCampaignDiscovery() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/campaigns")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/waitlist/join is strictly denied 403 for ROLE_SUPER_ADMIN")
    void superAdmin_cannotJoinCitizenWaitlist() throws Exception {
        WaitlistJoinRequest request = new WaitlistJoinRequest(1L, 1L, superAdmin.getId());

        mockMvc.perform(post("/api/v1/waitlist/join")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/appointments/mine is strictly denied 403 for ROLE_SUPER_ADMIN")
    void superAdmin_cannotAccessCitizenAppointmentsMine() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/mine")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/appointments/book is strictly denied 403 for ROLE_SUPER_ADMIN")
    void superAdmin_cannotBookCitizenAppointment() throws Exception {
        String json = String.format("""
                {
                    "citizenId": %d,
                    "campaignId": 1,
                    "slotId": 1
                }
                """, superAdmin.getId());

        mockMvc.perform(post("/api/v1/appointments/book")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }
}
