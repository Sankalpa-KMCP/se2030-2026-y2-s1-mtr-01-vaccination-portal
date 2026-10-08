package com.se2030.vaccinationportal.screening;

import com.se2030.vaccinationportal.screening.questionnaire.web.QuestionnaireController;
import com.se2030.vaccinationportal.screening.questionnaire.web.ScreeningConfigurationController;
import com.se2030.vaccinationportal.screening.eligibility.service.EligibilityReviewService;
import com.se2030.vaccinationportal.screening.questionnaire.service.QuestionnaireVersionService;
import com.se2030.vaccinationportal.screening.questionnaire.service.ScreeningConfigurationService;
import com.se2030.vaccinationportal.screening.submission.service.CitizenScreeningAccessService;
import com.se2030.vaccinationportal.screening.query.service.ScreeningHistoryService;
import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.validation.RequireRoles;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.util.List;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
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
    private QuestionnaireVersionService questionnaireVersionService;

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
    void authenticatedHealthcareOfficerCanPerformScreeningConfigurationMutations() throws Exception {
        mockMvc.perform(delete("/api/v1/screening/questions/{id}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_HEALTHCARE_OFFICER")))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/screening/rules/{ruleId}", 2L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_HEALTHCARE_OFFICER")))
                .andExpect(status().isNoContent());

        verify(configurationService).deleteQuestion(1L);
        verify(questionnaireVersionService).deleteEligibilityRule(2L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR"})
    void administrativeRolesCannotPerformScreeningConfigurationMutations(String role) throws Exception {
        mockMvc.perform(delete("/api/v1/screening/questions/{id}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token(role)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/screening/rules/{ruleId}", 2L)
                        .header(HttpHeaders.AUTHORIZATION, token(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(configurationService, questionnaireVersionService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_HEALTHCARE_OFFICER", "ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR"})
    void existingQuestionCatalogueReadRolesRemainAuthorized(String role) throws Exception {
        when(configurationService.getAllQuestions()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/screening/questions")
                        .header(HttpHeaders.AUTHORIZATION, token(role)))
                .andExpect(status().isOk());
    }

    @Test
    void allScreeningAuthoringMutationsAreHealthcareOfficerOnly() {
        assertHealthcareOfficerOnly(
                ScreeningConfigurationController.class,
                "createQuestion",
                "updateQuestion",
                "deleteQuestion",
                "addAnswerOption",
                "updateAnswerOption",
                "removeAnswerOption",
                "createQuestionnaire"
        );
        assertHealthcareOfficerOnly(
                QuestionnaireController.class,
                "createDraftVersion",
                "addQuestionItem",
                "addEligibilityRule",
                "updateEligibilityRule",
                "deleteEligibilityRule",
                "publishVersion",
                "retireVersion",
                "deleteDraftVersion"
        );
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
    void legacyHeadersCannotOverrideAuthenticatedHealthcareOfficerAuthority() throws Exception {
        mockMvc.perform(delete("/api/v1/screening/questions/{id}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_HEALTHCARE_OFFICER"))
                        .header(LEGACY_ROLE_HEADER, "CITIZEN")
                        .header(ALTERNATE_LEGACY_ROLE_HEADER, "CITIZEN"))
                .andExpect(status().isNoContent());
    }

    @Test
    void campaignOfficerCanAccessPublishedQuestionnaireVersions() throws Exception {
        when(questionnaireVersionService.getPublishedVersions()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/screening/questionnaire-versions/published")
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_CAMPAIGN_OFFICER")))
                .andExpect(status().isOk());

        verify(questionnaireVersionService).getPublishedVersions();
        verify(questionnaireVersionService, never()).getVersionById(anyLong());
    }

    @Test
    void campaignOfficerCanAccessQuestionnaireVersionById() throws Exception {
        Questionnaire q = new Questionnaire("TEST-Q", "Test Questionnaire", "Desc");
        q.setId(10L);
        QuestionnaireVersion v = new QuestionnaireVersion(q, 1, QuestionnaireVersionStatus.PUBLISHED);
        v.setId(123L);
        when(questionnaireVersionService.getVersionById(123L)).thenReturn(v);

        mockMvc.perform(get("/api/v1/screening/questionnaire-versions/{versionId}", 123L)
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_CAMPAIGN_OFFICER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123L));

        verify(questionnaireVersionService).getVersionById(123L);
    }

    @Test
    void invalidTextualVersionIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/screening/questionnaire-versions/not-a-number")
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_CAMPAIGN_OFFICER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'versionId'"));

        verify(questionnaireVersionService, never()).getVersionById(anyLong());
    }

    @Test
    void citizenCannotAccessPublishedQuestionnaireVersions() throws Exception {
        mockMvc.perform(get("/api/v1/screening/questionnaire-versions/published")
                        .header(HttpHeaders.AUTHORIZATION, token("ROLE_CITIZEN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(questionnaireVersionService);
    }

    private void assertHealthcareOfficerOnly(Class<?> controllerType, String... methodNames) {
        for (String methodName : methodNames) {
            Method method = Arrays.stream(controllerType.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow();
            RequireRoles annotation = method.getAnnotation(RequireRoles.class);
            assertThat(annotation)
                    .as("@RequireRoles on %s.%s", controllerType.getSimpleName(), methodName)
                    .isNotNull();
            assertThat(annotation.value())
                    .as("authoring roles on %s.%s", controllerType.getSimpleName(), methodName)
                    .containsExactly("HEALTHCARE_OFFICER");
        }
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
