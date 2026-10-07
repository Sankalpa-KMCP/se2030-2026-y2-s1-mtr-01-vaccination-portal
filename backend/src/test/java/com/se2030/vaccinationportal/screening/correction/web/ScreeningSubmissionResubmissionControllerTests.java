package com.se2030.vaccinationportal.screening.correction.web;

import com.se2030.vaccinationportal.screening.submission.web.ScreeningSubmissionController;


import com.se2030.vaccinationportal.screening.submission.dto.response.ScreeningSubmissionResponse;
import com.se2030.vaccinationportal.screening.submission.mapper.ScreeningSubmissionMapper;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;
import com.se2030.vaccinationportal.screening.submission.service.ScreeningSubmissionService;
import com.se2030.vaccinationportal.screening.submission.service.CitizenScreeningAccessService;
import com.se2030.vaccinationportal.screening.validation.RoleAuthorizationInterceptor;
import com.se2030.vaccinationportal.screening.validation.ScreeningExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScreeningSubmissionResubmissionControllerTests {

    private static final long SUBMISSION_ID = 50L;
    private static final String RESUBMIT_PATH =
            "/api/v1/screening/submissions/{submissionId}/resubmit";

    @Mock
    private ScreeningSubmissionService submissionService;

    @Mock
    private ScreeningSubmissionMapper submissionMapper;

    @Mock
    private CitizenScreeningAccessService accessService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ScreeningSubmissionController controller =
                new ScreeningSubmissionController(submissionService, submissionMapper, accessService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ScreeningExceptionHandler())
                .addInterceptors(new RoleAuthorizationInterceptor())
                .build();
        authenticateAs("CITIZEN");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resubmitCorrectionsReturnsUpdatedSubmission() throws Exception {
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(SUBMISSION_ID);
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);

        ScreeningSubmissionResponse response = new ScreeningSubmissionResponse(
                SUBMISSION_ID,
                1L,
                2L,
                20L,
                30L,
                ScreeningSubmissionStatus.PENDING_REVIEW,
                PreliminaryScreeningResult.PRELIMINARY_PASS,
                null,
                null,
                List.of(),
                null,
                null,
                "Please correct the answer",
                null
        );

        lenient().when(submissionService.resubmitCorrections(
                        eq(SUBMISSION_ID),
                        eq(Map.of(101L, "false"))))
                .thenReturn(submission);
        lenient().when(submissionMapper.toResponse(submission)).thenReturn(response);

        mockMvc.perform(post(RESUBMIT_PATH, SUBMISSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": {
                                    "101": "false"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SUBMISSION_ID))
                .andExpect(jsonPath("$.citizenId").value(1L))
                .andExpect(jsonPath("$.campaignId").value(2L))
                .andExpect(jsonPath("$.questionnaireVersionId").value(20L))
                .andExpect(jsonPath("$.consentNoticeVersionId").value(30L))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        verify(submissionService).resubmitCorrections(
                SUBMISSION_ID,
                Map.of(101L, "false"));
        verify(submissionMapper).toResponse(submission);
    }

    @Test
    void resubmitCorrectionsRejectsDuplicateQuestionKeys() throws Exception {
        mockMvc.perform(post(RESUBMIT_PATH, SUBMISSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": {
                                    "7": "true",
                                    "7": "false"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(
                        "Duplicate answer submitted for question 7")));

        verify(submissionService, never()).resubmitCorrections(any(), anyMap());
    }

    @Test
    void resubmitCorrectionsRejectsNumericEquivalentDuplicateQuestionKeys() throws Exception {
        mockMvc.perform(post(RESUBMIT_PATH, SUBMISSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": {
                                    "7": "true",
                                    "007": "false"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(
                        "Duplicate answer submitted for question 7")));

        verify(submissionService, never()).resubmitCorrections(any(), anyMap());
    }

    @Test
    void resubmitCorrectionsRejectsMissingAnswers() throws Exception {
        mockMvc.perform(post(RESUBMIT_PATH, SUBMISSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(submissionService, never()).resubmitCorrections(any(), anyMap());
    }

    @Test
    void resubmitCorrectionsPropagatesExistingLifecycleErrorHandling() throws Exception {
        lenient().when(submissionService.resubmitCorrections(
                        eq(SUBMISSION_ID),
                        eq(Map.of(101L, "false"))))
                .thenThrow(new IllegalStateException(
                        "Corrections can only be resubmitted for submissions in CORRECTIONS_REQUIRED status"));

        mockMvc.perform(post(RESUBMIT_PATH, SUBMISSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": {
                                    "101": "false"
                                  }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("CORRECTIONS_REQUIRED")));
    }

    @Test
    void resubmitCorrectionsRejectsRoleOutsideCitizenFacingBoundary() throws Exception {
        authenticateAs("HEALTHCARE_OFFICER");
        mockMvc.perform(post(RESUBMIT_PATH, SUBMISSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": {
                                    "101": "false"
                                  }
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(submissionService, never()).resubmitCorrections(any(), anyMap());
    }

    private void authenticateAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test-user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                )
        );
    }
}
