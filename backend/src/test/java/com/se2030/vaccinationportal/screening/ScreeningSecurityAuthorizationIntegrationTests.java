package com.se2030.vaccinationportal.screening;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.screening.validation.RoleAuthorizationInterceptor;
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
 * Spring Security integration tests for {@code /api/v1/screening/**} endpoints (MF-04).
 * Verifies that spoofed X-User-Role headers are rejected and authoritative JWT SecurityContext is enforced.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ScreeningSecurityAuthorizationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    @Nested
    @DisplayName("Allowed Authenticated Roles (ADMIN, HEALTHCARE_OFFICER)")
    class AllowedRoles {

        @Test
        @DisplayName("ADMIN token is authorized for screening queue")
        void adminAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/screening/assessments/queue")
                            .header(HttpHeaders.AUTHORIZATION, token(1L, "admin@test.gov", List.of("ROLE_ADMIN")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("HEALTHCARE_OFFICER token is authorized for screening queue")
        void healthcareOfficerAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/screening/assessments/queue")
                            .header(HttpHeaders.AUTHORIZATION, token(2L, "hco@test.gov", List.of("ROLE_HEALTHCARE_OFFICER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Denied Authenticated Roles (INVENTORY_OFFICER, CITIZEN)")
    class DeniedRoles {

        @Test
        @DisplayName("INVENTORY_OFFICER token is denied with 403 Forbidden for screening queue")
        void inventoryOfficerDenied() throws Exception {
            mockMvc.perform(get("/api/v1/screening/assessments/queue")
                            .header(HttpHeaders.AUTHORIZATION, token(3L, "inv@test.gov", List.of("ROLE_INVENTORY_OFFICER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CITIZEN token is denied with 403 Forbidden for screening queue")
        void citizenDenied() throws Exception {
            mockMvc.perform(get("/api/v1/screening/assessments/queue")
                            .header(HttpHeaders.AUTHORIZATION, token(4L, "cit@test.gov", List.of("ROLE_CITIZEN")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Header Spoofing Attack Rejection")
    class HeaderSpoofingRejection {

        @Test
        @DisplayName("Anonymous caller with forged X-User-Role: ADMIN header is rejected with 401 Unauthorized")
        void anonymousForgedHeaderRejected() throws Exception {
            mockMvc.perform(get("/api/v1/screening/assessments/queue")
                            .header(RoleAuthorizationInterceptor.ROLE_HEADER, "ADMIN")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Authenticated CITIZEN token attempting privilege escalation via forged X-User-Role: ADMIN header is rejected with 403 Forbidden")
        void citizenPrivilegeEscalationRejected() throws Exception {
            mockMvc.perform(get("/api/v1/screening/assessments/queue")
                            .header(HttpHeaders.AUTHORIZATION, token(5L, "cit@test.gov", List.of("ROLE_CITIZEN")))
                            .header(RoleAuthorizationInterceptor.ROLE_HEADER, "ADMIN")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }
}
