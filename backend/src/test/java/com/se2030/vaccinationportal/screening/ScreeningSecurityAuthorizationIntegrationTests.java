package com.se2030.vaccinationportal.screening;

import com.se2030.vaccinationportal.screening.service.EligibilityReviewService;
import com.se2030.vaccinationportal.screening.service.ScreeningConfigurationService;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScreeningSecurityAuthorizationIntegrationTests {

    private static final String LEGACY_ROLE_HEADER = "X-User-Role";
    private static final String ALTERNATE_LEGACY_ROLE_HEADER = "X-Role";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private ScreeningConfigurationService configurationService;

    @MockitoBean
    private EligibilityReviewService reviewService;

    @Test
    void authenticatedAdminCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(delete("/api/v1/screening/questions/{id}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void authenticatedHealthcareOfficerCanAccessReviewQueue() throws Exception {
        mockMvc.perform(get("/api/v1/screening/assessments/queue")
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_HEALTHCARE_OFFICER")))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedCitizenCanAccessCitizenAssessmentRead() throws Exception {
        mockMvc.perform(get("/api/v1/screening/assessments/submissions/{submissionId}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_CITIZEN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void authenticatedCitizenCannotAccessReviewDetails() throws Exception {
        mockMvc.perform(get("/api/v1/screening/submissions/{submissionId}/review-details", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_CITIZEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCallerCannotAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/screening/assessments/queue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCallerCannotSpoofAdminWithLegacyHeader() throws Exception {
        mockMvc.perform(delete("/api/v1/screening/questions/{id}", 1L)
                        .header(LEGACY_ROLE_HEADER, "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void citizenCannotSpoofHealthcareOfficerWithLegacyHeader() throws Exception {
        mockMvc.perform(get("/api/v1/screening/submissions/{submissionId}/review-details", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_CITIZEN"))
                        .header(LEGACY_ROLE_HEADER, "HEALTHCARE_OFFICER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void canonicalRolePrefixMapsToRequireRolesValue() throws Exception {
        mockMvc.perform(get("/api/v1/screening/assessments/queue")
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_HEALTHCARE_OFFICER")))
                .andExpect(status().isOk());
    }

    @Test
    void legacyHeadersCannotOverrideAuthenticatedAuthorities() throws Exception {
        mockMvc.perform(delete("/api/v1/screening/questions/{id}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_ADMIN"))
                        .header(LEGACY_ROLE_HEADER, "CITIZEN")
                        .header(ALTERNATE_LEGACY_ROLE_HEADER, "CITIZEN"))
                .andExpect(status().isNoContent());
    }

    private String token(String role) {
        return "Bearer " + jwtTokenService.generateToken(
                100L,
                "security-test@portal.gov",
                List.of(role)
        );
    }
}
