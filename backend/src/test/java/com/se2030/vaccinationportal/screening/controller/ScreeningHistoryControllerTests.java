package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.mapper.ScreeningHistoryDetailsMapper;
import com.se2030.vaccinationportal.screening.model.*;
import com.se2030.vaccinationportal.screening.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.screening.service.CitizenScreeningAccessService;
import com.se2030.vaccinationportal.screening.service.ScreeningHistoryService;
import com.se2030.vaccinationportal.screening.validation.RoleAuthorizationInterceptor;
import com.se2030.vaccinationportal.screening.validation.ScreeningExceptionHandler;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.aMapWithSize;

@ExtendWith(MockitoExtension.class)
class ScreeningHistoryControllerTests {
    @Mock private ScreeningSubmissionRepository submissions;
    @Mock private QuestionnaireItemRepository items;
    @Mock private AnswerOptionRepository options;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ScreeningHistoryService service = new ScreeningHistoryService(submissions, items, options,
                new CitizenScreeningAccessService(submissions), new ScreeningHistoryDetailsMapper());
        mvc = MockMvcBuilders.standaloneSetup(new ScreeningHistoryController(service))
                .setControllerAdvice(new ScreeningExceptionHandler())
                .addInterceptors(new RoleAuthorizationInterceptor()).build();
        authenticate(6L, "ROLE_CITIZEN");
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void citizenReadsOwnHistoricalDetailsWithOnlyCitizenSafeFields() throws Exception {
        stubSubmission();
        mvc.perform(get("/api/v1/screening/submissions/7/history-details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(13)))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.citizenId").value(6))
                .andExpect(jsonPath("$.campaignId").value(1))
                .andExpect(jsonPath("$.questionnaireVersionId").value(70))
                .andExpect(jsonPath("$.questionnaireVersionNumber").value(3))
                .andExpect(jsonPath("$.status").value("CORRECTIONS_REQUIRED"))
                .andExpect(jsonPath("$.preliminaryResult").value("PRELIMINARY_PASS"))
                .andExpect(jsonPath("$.submittedAt").value("2026-09-01T10:00:00Z"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-01T10:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-09-03T10:00:00Z"))
                .andExpect(jsonPath("$.correctionInstructions").value("Clarify answer"))
                .andExpect(jsonPath("$.correctionRequestedAt").value("2026-09-03T10:00:00Z"))
                .andExpect(jsonPath("$.answers[0]", aMapWithSize(9)))
                .andExpect(jsonPath("$.answers[0].id").value(100))
                .andExpect(jsonPath("$.answers[0].questionId").value(10))
                .andExpect(jsonPath("$.answers[0].questionCode").value("FEVER"))
                .andExpect(jsonPath("$.answers[0].questionText").value("Do you currently have a fever?"))
                .andExpect(jsonPath("$.answers[0].questionType").value("BOOLEAN"))
                .andExpect(jsonPath("$.answers[0].displayOrder").value(2))
                .andExpect(jsonPath("$.answers[0].typedAnswer").value("false"))
                .andExpect(jsonPath("$.answers[0].displayAnswer").value("No"))
                .andExpect(jsonPath("$.answers[0].selectedOptions").isEmpty())
                .andExpect(jsonPath("$.assessment").doesNotExist())
                .andExpect(jsonPath("$.clinicalNotes").doesNotExist())
                .andExpect(jsonPath("$.reviewerId").doesNotExist())
                .andExpect(jsonPath("$.triggeredRuleEvidence").doesNotExist());
    }

    @Test
    void citizenCannotReadAnotherCitizensDetailsBeforeMetadataHydration() throws Exception {
        when(submissions.findById(7L)).thenReturn(Optional.of(submission()));
        authenticate(99L, "ROLE_CITIZEN");
        mvc.perform(get("/api/v1/screening/submissions/7/history-details"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(items, options);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_CITIZEN", "ROLE_HEALTHCARE_OFFICER", "ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR"})
    void nonexistentSubmissionReturns404(String role) throws Exception {
        authenticate(6L, role);
        when(submissions.findById(404L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/screening/submissions/404/history-details"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(items, options);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_HEALTHCARE_OFFICER", "ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR"})
    void privilegedReadRolesCanReadAcrossCitizens(String role) throws Exception {
        authenticate(99L, role);
        stubSubmission();
        mvc.perform(get("/api/v1/screening/submissions/7/history-details"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_CAMPAIGN_OFFICER", "ROLE_RECEPTION_STAFF", "ROLE_CENTER_MANAGER"})
    void unrelatedRolesCannotReadDetails(String role) throws Exception {
        authenticate(99L, role);
        mvc.perform(get("/api/v1/screening/submissions/7/history-details"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(submissions, items, options);
    }

    @Test
    void anonymousRequestReturns401() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/v1/screening/submissions/7/history-details"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(submissions, items, options);
    }

    @Test
    void malformedSubmissionIdReturns400() throws Exception {
        mvc.perform(get("/api/v1/screening/submissions/invalid/history-details"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(submissions, items, options);
    }

    private void stubSubmission() {
        ScreeningSubmission submission = submission();
        ScreeningQuestion question = new ScreeningQuestion("FEVER", "Do you currently have a fever?", QuestionType.BOOLEAN, true, true);
        question.setId(10L);
        ScreeningAnswer answer = new ScreeningAnswer(submission, question, "false");
        answer.setId(100L);
        submission.addAnswer(answer);
        EligibilityAssessment assessment = new EligibilityAssessment();
        assessment.setClinicalNotes("Internal information must not appear");
        submission.setAssessment(assessment);
        when(submissions.findById(7L)).thenReturn(Optional.of(submission));
        when(items.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(70L)).thenReturn(
                List.of(new QuestionnaireItem(submission.getQuestionnaireVersion(), question, 2, null)));
    }

    private ScreeningSubmission submission() {
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(7L);
        submission.setCitizenId(6L);
        submission.setCampaignId(1L);
        QuestionnaireVersion version = new QuestionnaireVersion(null, 3, QuestionnaireVersionStatus.RETIRED);
        version.setId(70L);
        submission.setQuestionnaireVersion(version);
        submission.setStatus(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED);
        submission.setPreliminaryResult(PreliminaryScreeningResult.PRELIMINARY_PASS);
        submission.setSubmittedAt(Instant.parse("2026-09-01T10:00:00Z"));
        submission.setCreatedAt(submission.getSubmittedAt());
        submission.setUpdatedAt(Instant.parse("2026-09-03T10:00:00Z"));
        submission.setCorrectionInstructions("Clarify answer");
        submission.setCorrectionRequestedAt(submission.getUpdatedAt());
        return submission;
    }

    private void authenticate(Long id, String role) {
        UserPrincipal principal = UserPrincipal.fromClaims(id, "synthetic@portal.test", List.of(role));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }
}
