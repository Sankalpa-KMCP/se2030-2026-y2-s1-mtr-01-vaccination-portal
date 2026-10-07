package com.se2030.vaccinationportal.screening.submission.web;

import com.se2030.vaccinationportal.screening.submission.mapper.ScreeningSubmissionMapper;
import com.se2030.vaccinationportal.screening.submission.service.ScreeningSubmissionService;
import com.se2030.vaccinationportal.screening.submission.service.CitizenScreeningAccessService;
import com.se2030.vaccinationportal.screening.validation.ScreeningExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScreeningSubmissionDuplicateAnswerTests {

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
                .build();
    }

    @Test
    void duplicateQuestionAnswersReturnBadRequestBeforeServiceInvocation() throws Exception {
        String json = """
                {
                  "citizenId": 1,
                  "campaignId": 2,
                  "questionnaireVersionId": 3,
                  "consentNoticeVersionId": 4,
                  "answers": {"7": "true", "7": "false"}
                }
                """;

        mockMvc.perform(post("/api/v1/screening/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Duplicate answer submitted for question 7")));

        verify(submissionService, never()).submitScreening(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}
