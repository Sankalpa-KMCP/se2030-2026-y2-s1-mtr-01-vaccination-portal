package com.se2030.vaccinationportal.screening.correction.web;

import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.screening.submission.service.CitizenScreeningAccessService;
import com.se2030.vaccinationportal.screening.correction.service.ScreeningCorrectionFormService;
import com.se2030.vaccinationportal.screening.validation.RoleAuthorizationInterceptor;
import com.se2030.vaccinationportal.screening.validation.ScreeningExceptionHandler;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScreeningCorrectionFormControllerTests {

    private static final String PATH = "/api/v1/screening/submissions/{submissionId}/correction-form";

    @Mock private ScreeningSubmissionRepository submissions;
    @Mock private QuestionnaireItemRepository items;
    @Mock private AnswerOptionRepository options;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        CitizenScreeningAccessService access = new CitizenScreeningAccessService(submissions);
        ScreeningCorrectionFormService service = new ScreeningCorrectionFormService(
                submissions, items, options, access
        );
        mvc = MockMvcBuilders.standaloneSetup(new ScreeningCorrectionFormController(service))
                .setControllerAdvice(new ScreeningExceptionHandler())
                .addInterceptors(new RoleAuthorizationInterceptor())
                .build();
        authenticate(6L, "ROLE_CITIZEN");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void citizenRetrievesOwnCorrectionFormWithOnlyCitizenSafeFields() throws Exception {
        when(submissions.findById(7L)).thenReturn(Optional.of(submission(6L,
                ScreeningSubmissionStatus.CORRECTIONS_REQUIRED)));
        when(items.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(70L)).thenReturn(List.of());

        mvc.perform(get(PATH, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(8)))
                .andExpect(jsonPath("$.submissionId").value(7L))
                .andExpect(jsonPath("$.campaignId").value(3L))
                .andExpect(jsonPath("$.questionnaireVersionId").value(70L))
                .andExpect(jsonPath("$.questionnaireVersionNumber").value(4))
                .andExpect(jsonPath("$.status").value("CORRECTIONS_REQUIRED"))
                .andExpect(jsonPath("$.correctionInstructions").value("Correct the answer"))
                .andExpect(jsonPath("$.correctionRequestedAt").value("2026-09-05T02:00:00Z"))
                .andExpect(jsonPath("$.questions").isEmpty())
                .andExpect(jsonPath("$.consentNoticeVersionId").doesNotExist())
                .andExpect(jsonPath("$.assessment").doesNotExist())
                .andExpect(jsonPath("$.clinicalNotes").doesNotExist())
                .andExpect(jsonPath("$.reviewerId").doesNotExist())
                .andExpect(jsonPath("$.triggeredRuleEvidence").doesNotExist());
    }

    @Test
    void citizenCannotRetrieveAnotherCitizensCorrectionForm() throws Exception {
        when(submissions.findById(7L)).thenReturn(Optional.of(submission(99L,
                ScreeningSubmissionStatus.CORRECTIONS_REQUIRED)));

        mvc.perform(get(PATH, 7L)).andExpect(status().isForbidden());

        verifyNoInteractions(items, options);
    }

    @Test
    void missingSubmissionReturns404() throws Exception {
        when(submissions.findById(404L)).thenReturn(Optional.empty());

        mvc.perform(get(PATH, 404L)).andExpect(status().isNotFound());

        verifyNoInteractions(items, options);
    }

    @ParameterizedTest
    @EnumSource(value = ScreeningSubmissionStatus.class, names = {
            "DRAFT", "PENDING_REVIEW", "REVIEWED"
    })
    void wrongSubmissionStatusReturns409(ScreeningSubmissionStatus submissionStatus) throws Exception {
        when(submissions.findById(7L)).thenReturn(Optional.of(submission(6L, submissionStatus)));

        mvc.perform(get(PATH, 7L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Correction form is only available for submissions in CORRECTIONS_REQUIRED status"));

        verifyNoInteractions(items, options);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR"})
    void administrativeRolesCanRetrieveCorrectionForm(String role) throws Exception {
        authenticate(100L, role);
        when(submissions.findById(7L)).thenReturn(Optional.of(submission(99L,
                ScreeningSubmissionStatus.CORRECTIONS_REQUIRED)));
        when(items.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(70L)).thenReturn(List.of());

        mvc.perform(get(PATH, 7L)).andExpect(status().isOk());
    }

    @Test
    void healthcareOfficerIsNotAuthorizedForCitizenCorrectionForm() throws Exception {
        authenticate(100L, "ROLE_HEALTHCARE_OFFICER");

        mvc.perform(get(PATH, 7L)).andExpect(status().isForbidden());

        verifyNoInteractions(submissions, items, options);
    }

    @Test
    void anonymousCallerReturns401() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get(PATH, 7L)).andExpect(status().isUnauthorized());

        verifyNoInteractions(submissions, items, options);
    }

    private ScreeningSubmission submission(Long citizenId, ScreeningSubmissionStatus status) {
        QuestionnaireVersion version = new QuestionnaireVersion(null, 4, QuestionnaireVersionStatus.RETIRED);
        version.setId(70L);
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(7L);
        submission.setCitizenId(citizenId);
        submission.setCampaignId(3L);
        submission.setQuestionnaireVersion(version);
        submission.setStatus(status);
        submission.setCorrectionInstructions("Correct the answer");
        submission.setCorrectionRequestedAt(Instant.parse("2026-09-05T02:00:00Z"));
        return submission;
    }

    private void authenticate(Long userId, String role) {
        UserPrincipal principal = UserPrincipal.fromClaims(userId, "synthetic@portal.test", List.of(role));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        ));
    }
}
