package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.dto.ScreeningSubmissionRequest;
import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScreeningExceptionHandlerTests {

    private static final String BASE_PATH = "/test/screening-errors";
    private static final String INTERNAL_MESSAGE = "database password was exposed";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionTestController())
                .setControllerAdvice(new ScreeningExceptionHandler())
                .build();
    }

    @Test
    void invalidArgumentReturnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/invalid-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid boolean answer for question 7"));
    }

    @Test
    void resourceNotFoundReturnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Screening question 7 not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void campaignResourceNotFoundStillReturnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/campaign-resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Vaccination campaign 8 not found"));
    }

    @Test
    void lifecycleConflictReturnsConflict() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Questionnaire version is not in DRAFT status"));
    }

    @Test
    void invalidPathVariableReturnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/typed/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("Invalid value for parameter 'id'")));
    }

    @Test
    void validationFailureReturnsBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.citizenId").isArray())
                .andExpect(jsonPath("$.fieldErrors.answers").isArray());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed screening request payload"));
    }

    @Test
    void duplicateAnswerKeysRemainBadRequest() throws Exception {
        assertDuplicateAnswerKeysRejected("\"7\": \"true\", \"7\": \"false\"");
        assertDuplicateAnswerKeysRejected("\"7\": \"true\", \"07\": \"false\"");
    }

    @Test
    void unexpectedExceptionReturnsSanitizedInternalServerError() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected internal server error occurred"))
                .andExpect(jsonPath("$.message", not(containsString(INTERNAL_MESSAGE))))
                .andExpect(jsonPath("$.message", not(containsString("RuntimeException"))));
    }

    private void assertDuplicateAnswerKeysRejected(String answerEntries) throws Exception {
        String requestBody = """
                {
                  "citizenId": 1,
                  "campaignId": 2,
                  "questionnaireVersionId": 3,
                  "consentNoticeVersionId": 4,
                  "answers": {%s}
                }
                """.formatted(answerEntries);

        mockMvc.perform(post(BASE_PATH + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Duplicate answer submitted for question 7"));
    }

    @RestController
    @RequestMapping(BASE_PATH)
    private static class ExceptionTestController {

        @GetMapping("/invalid-argument")
        void invalidArgument() {
            throw new IllegalArgumentException("Invalid boolean answer for question 7");
        }

        @GetMapping("/resource-not-found")
        void resourceNotFound() {
            throw new ResourceNotFoundException("Screening question 7 not found");
        }

        @GetMapping("/campaign-resource-not-found")
        void campaignResourceNotFound() {
            throw new com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException(
                    "Vaccination campaign 8 not found");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new IllegalStateException("Questionnaire version is not in DRAFT status");
        }

        @GetMapping("/typed/{id}")
        void typedPathVariable(@PathVariable Long id) {
        }

        @PostMapping("/request")
        void request(@Valid @RequestBody ScreeningSubmissionRequest request) {
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new RuntimeException(INTERNAL_MESSAGE);
        }

    }
}
