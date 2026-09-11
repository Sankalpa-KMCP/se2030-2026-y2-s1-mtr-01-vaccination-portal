package com.se2030.vaccinationportal.screening;

import com.se2030.vaccinationportal.screening.service.EligibilityReviewService;
import com.se2030.vaccinationportal.screening.service.ScreeningConfigurationService;
import com.se2030.vaccinationportal.screening.service.CitizenScreeningAccessService;
import com.se2030.vaccinationportal.screening.service.ScreeningHistoryService;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
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
import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScreeningSecurityAuthorizationIntegrationTests {

    private static final String LEGACY_ROLE_HEADER = "X-User-Role";
    private static final String ALTERNATE_LEGACY_ROLE_HEADER = "X-Role";
    private static final String DECISION_BODY = """
            {
              "status": "INELIGIBLE",
              "reason": "Security attribution test"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private ScreeningConfigurationService configurationService;

    @MockitoBean
    private EligibilityReviewService reviewService;

    @MockitoBean
    private CitizenScreeningAccessService accessService;

    @MockitoBean
    private ScreeningHistoryService historyService;

    @Test
    void authenticatedCitizenCanReachHistoryDetails() throws Exception {
        mockMvc.perform(get("/api/v1/screening/submissions/{submissionId}/history-details", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_CITIZEN")))
                .andExpect(status().isOk());
        org.mockito.Mockito.verify(historyService).getHistoryDetails(1L);
    }

    @Test
    void anonymousCallerCannotReadHistoryDetails() throws Exception {
        mockMvc.perform(get("/api/v1/screening/submissions/{submissionId}/history-details", 1L))
                .andExpect(status().isUnauthorized());
        org.mockito.Mockito.verifyNoInteractions(historyService);
    }

    @Test
    void unrelatedRoleCannotSpoofCitizenForHistoryDetails() throws Exception {
        mockMvc.perform(get("/api/v1/screening/submissions/{submissionId}/history-details", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_CAMPAIGN_OFFICER"))
                        .header(LEGACY_ROLE_HEADER, "CITIZEN"))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(historyService);
    }

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
    void authenticatedHealthcareOfficerDecisionUsesJwtPrincipalUserId() throws Exception {
        long reviewerId = 42L;
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(1L);
        EligibilityAssessment assessment = new EligibilityAssessment(
                submission,
                2L,
                3L,
                reviewerId,
                EligibilityAssessmentStatus.INELIGIBLE,
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                "Security attribution test",
                Instant.parse("2026-09-07T00:00:00Z"),
                null
        );
        assessment.setId(4L);
        assessment.setCreatedAt(Instant.parse("2026-09-07T00:00:00Z"));
        assessment.setUpdatedAt(Instant.parse("2026-09-07T00:00:00Z"));
        when(reviewService.recordDecision(
                1L,
                reviewerId,
                EligibilityAssessmentStatus.INELIGIBLE,
                "Security attribution test",
                null,
                null
        )).thenReturn(assessment);

        mockMvc.perform(post("/api/v1/screening/assessments/submissions/{submissionId}/decision", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token(reviewerId, "ROLE_HEALTHCARE_OFFICER"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(DECISION_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewerId").value(reviewerId));

        verify(reviewService).recordDecision(
                1L,
                reviewerId,
                EligibilityAssessmentStatus.INELIGIBLE,
                "Security attribution test",
                null,
                null
        );
    }

    @Test
    void anonymousCallerCannotRecordDecision() throws Exception {
        mockMvc.perform(post("/api/v1/screening/assessments/submissions/{submissionId}/decision", 1L)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(DECISION_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reviewService);
    }

    @Test
    void citizenCannotRecordDecision() throws Exception {
        mockMvc.perform(post("/api/v1/screening/assessments/submissions/{submissionId}/decision", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_CITIZEN"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(DECISION_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reviewService);
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
        return token(100L, role);
    }

    private String token(Long userId, String role) {
        return "Bearer " + jwtTokenService.generateToken(
                userId,
                "security-test@portal.gov",
                List.of(role)
        );
    }
}
