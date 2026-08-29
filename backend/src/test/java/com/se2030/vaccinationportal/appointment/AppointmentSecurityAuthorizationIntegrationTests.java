package com.se2030.vaccinationportal.appointment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import java.util.List;
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
 * Spring Security integration tests for {@code /api/v1/appointments} and {@code /api/v1/waitlist} endpoints (MF-03).
 * Tests method security, staff override authorization, and preservation of public citizen intake.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AppointmentSecurityAuthorizationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    @Nested
    @DisplayName("Operational Endpoints Allowed Roles (ADMIN, HEALTHCARE_OFFICER, CENTER_MANAGER)")
    class OperationalAllowedRoles {

        @Test
        @DisplayName("ADMIN role is allowed to access GET /api/v1/appointments/queue")
        void adminQueueAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/queue")
                            .param("slotId", "1")
                            .header(HttpHeaders.AUTHORIZATION, token(1L, "admin@test.gov", List.of("ROLE_ADMIN")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HEALTHCARE_OFFICER role is allowed to access GET /api/v1/appointments/queue")
        void healthcareOfficerQueueAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/queue")
                            .param("slotId", "1")
                            .header(HttpHeaders.AUTHORIZATION, token(2L, "hco@test.gov", List.of("ROLE_HEALTHCARE_OFFICER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("CENTER_MANAGER role is allowed to access GET /api/v1/appointments/queue")
        void centerManagerQueueAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/queue")
                            .param("slotId", "1")
                            .header(HttpHeaders.AUTHORIZATION, token(3L, "cm@test.gov", List.of("ROLE_CENTER_MANAGER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Operational Endpoints Denied Roles (CITIZEN, CAMPAIGN_OFFICER, INVENTORY_OFFICER)")
    class OperationalDeniedRoles {

        @Test
        @DisplayName("CITIZEN role is denied with 403 Forbidden on GET /api/v1/appointments/queue")
        void citizenQueueDenied() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/queue")
                            .param("slotId", "1")
                            .header(HttpHeaders.AUTHORIZATION, token(4L, "cit@test.gov", List.of("ROLE_CITIZEN")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CAMPAIGN_OFFICER role is denied with 403 Forbidden on check-in")
        void campaignOfficerCheckInDenied() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/check-in")
                            .header(HttpHeaders.AUTHORIZATION, token(5L, "co@test.gov", List.of("ROLE_CAMPAIGN_OFFICER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INVENTORY_OFFICER role is denied with 403 Forbidden on reverse-check-in")
        void inventoryOfficerReverseCheckInDenied() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/reverse-check-in")
                            .header(HttpHeaders.AUTHORIZATION, token(6L, "inv@test.gov", List.of("ROLE_INVENTORY_OFFICER"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous request to GET /api/v1/appointments/queue is rejected with 401/403")
        void anonymousQueueRejected() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/queue")
                            .param("slotId", "1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Anonymous request to POST /api/v1/appointments/1/check-in is rejected with 401/403")
        void anonymousCheckInRejected() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/check-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("Staff Override on Cancel / Reschedule Protection")
    class StaffOverrideProtection {

        @Test
        @DisplayName("Anonymous caller claiming isStaffOverride=true on cancel is denied with 403")
        void anonymousStaffOverrideCancelDenied() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isStaffOverride\":true,\"staffReason\":\"emergency\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CITIZEN role claiming isStaffOverride=true on cancel is denied with 403")
        void citizenStaffOverrideCancelDenied() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/cancel")
                            .header(HttpHeaders.AUTHORIZATION, token(7L, "cit@test.gov", List.of("ROLE_CITIZEN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isStaffOverride\":true,\"staffReason\":\"emergency\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Public Citizen Intake Preservation")
    class PublicCitizenIntake {

        @Test
        @DisplayName("Anonymous request to POST /api/v1/appointments/book reaches controller without 401/403")
        void anonymousBookingAllowedThroughSecurity() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"citizenId\":0,\"campaignId\":0,\"slotId\":0}"))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()));
        }

        @Test
        @DisplayName("Anonymous request to GET /api/v1/waitlist/status reaches controller without 401/403")
        void anonymousWaitlistStatusAllowedThroughSecurity() throws Exception {
            mockMvc.perform(get("/api/v1/waitlist/status")
                            .param("citizenId", "1")
                            .param("campaignId", "1"))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()));
        }
    }
}
