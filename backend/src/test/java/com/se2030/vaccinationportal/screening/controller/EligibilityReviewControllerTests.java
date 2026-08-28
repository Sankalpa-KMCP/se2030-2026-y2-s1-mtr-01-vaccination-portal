package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.mapper.EligibilityAssessmentMapper;
import com.se2030.vaccinationportal.screening.mapper.ScreeningSubmissionMapper;
import com.se2030.vaccinationportal.screening.dto.response.EligibilityAssessmentResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningReviewAnswerOptionResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningReviewAnswerResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningReviewDetailsResponse;
import com.se2030.vaccinationportal.screening.dto.response.TriggeredRuleEvidenceResponse;
import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.service.EligibilityReviewService;
import com.se2030.vaccinationportal.screening.validation.RoleAuthorizationInterceptor;
import com.se2030.vaccinationportal.screening.validation.ScreeningExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EligibilityReviewControllerTests {

    private static final Long CITIZEN_ID = 10L;
    private static final Long CAMPAIGN_ID = 20L;
    private static final Long SUBMISSION_ID = 30L;
    private static final Long REVIEWER_ID = 40L;
    private static final Instant REVIEWED_AT = Instant.parse("2026-08-24T12:00:00Z");
    private static final Instant VALID_UNTIL = Instant.parse("2026-08-25T12:00:00Z");

    @Mock
    private EligibilityReviewService reviewService;

    @Mock
    private ScreeningSubmissionMapper submissionMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EligibilityReviewController controller = new EligibilityReviewController(
                reviewService,
                new EligibilityAssessmentMapper(),
                submissionMapper
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ScreeningExceptionHandler())
                .addInterceptors(new RoleAuthorizationInterceptor())
                .build();
    }

    @Test
    void recordDecisionReturnsCreatedMappedAssessmentResponse() throws Exception {
        EligibilityAssessment assessment = assessment(
                50L,
                SUBMISSION_ID,
                EligibilityAssessmentStatus.ELIGIBLE,
                "Citizen is clinically eligible",
                REVIEWED_AT,
                VALID_UNTIL
        );
        when(reviewService.recordDecision(
                SUBMISSION_ID,
                REVIEWER_ID,
                EligibilityAssessmentStatus.ELIGIBLE,
                "Citizen is clinically eligible",
                "No clinical concerns",
                VALID_UNTIL
        )).thenReturn(assessment);

        mockMvc.perform(post(
                        "/api/v1/screening/assessments/submissions/{submissionId}/decision",
                        SUBMISSION_ID)
                        .header(RoleAuthorizationInterceptor.ROLE_HEADER, "HEALTHCARE_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewerId": 40,
                                  "status": "ELIGIBLE",
                                  "reason": "Citizen is clinically eligible",
                                  "clinicalNotes": "No clinical concerns",
                                  "validUntil": "2026-08-25T12:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.submissionId").value(SUBMISSION_ID))
                .andExpect(jsonPath("$.citizenId").value(CITIZEN_ID))
                .andExpect(jsonPath("$.campaignId").value(CAMPAIGN_ID))
                .andExpect(jsonPath("$.reviewerId").value(REVIEWER_ID))
                .andExpect(jsonPath("$.status").value("ELIGIBLE"))
                .andExpect(jsonPath("$.preliminaryResult").value("PRELIMINARY_PASS"))
                .andExpect(jsonPath("$.reason").value("Citizen is clinically eligible"))
                .andExpect(jsonPath("$.clinicalNotes").value("No clinical concerns"))
                .andExpect(jsonPath("$.reviewedAt").value("2026-08-24T12:00:00Z"))
                .andExpect(jsonPath("$.validUntil").value("2026-08-25T12:00:00Z"))
                .andExpect(jsonPath("$.submission").doesNotExist());

        verify(reviewService).recordDecision(
                SUBMISSION_ID,
                REVIEWER_ID,
                EligibilityAssessmentStatus.ELIGIBLE,
                "Citizen is clinically eligible",
                "No clinical concerns",
                VALID_UNTIL
        );
    }

    @Test
    void getAssessmentBySubmissionReturnsNoContentWhenAbsent() throws Exception {
        when(reviewService.getAssessmentBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(
                        "/api/v1/screening/assessments/submissions/{submissionId}",
                        SUBMISSION_ID)
                        .header(RoleAuthorizationInterceptor.ROLE_HEADER, "CITIZEN"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(reviewService).getAssessmentBySubmissionId(SUBMISSION_ID);
    }

    @Test
    void getCitizenAssessmentHistoryReturnsMappedResponsesInServiceOrder() throws Exception {
        EligibilityAssessment newest = assessment(
                52L,
                32L,
                EligibilityAssessmentStatus.INELIGIBLE,
                "Latest assessment",
                REVIEWED_AT,
                null
        );
        EligibilityAssessment older = assessment(
                51L,
                31L,
                EligibilityAssessmentStatus.ELIGIBLE,
                "Older assessment",
                REVIEWED_AT.minusSeconds(3_600),
                VALID_UNTIL
        );
        when(reviewService.getAssessmentsForCitizen(CITIZEN_ID)).thenReturn(List.of(newest, older));

        mockMvc.perform(get("/api/v1/screening/citizens/{citizenId}/assessments", CITIZEN_ID)
                        .header(RoleAuthorizationInterceptor.ROLE_HEADER, "CITIZEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(52L))
                .andExpect(jsonPath("$[0].submissionId").value(32L))
                .andExpect(jsonPath("$[0].status").value("INELIGIBLE"))
                .andExpect(jsonPath("$[0].reason").value("Latest assessment"))
                .andExpect(jsonPath("$[1].id").value(51L))
                .andExpect(jsonPath("$[1].submissionId").value(31L))
                .andExpect(jsonPath("$[1].status").value("ELIGIBLE"))
                .andExpect(jsonPath("$[1].reason").value("Older assessment"))
                .andExpect(jsonPath("$[0].submission").doesNotExist())
                .andExpect(jsonPath("$[1].submission").doesNotExist());

        verify(reviewService).getAssessmentsForCitizen(CITIZEN_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    void getReviewDetailsAllowsHealthcareAndAdministrativeRoles(String role) throws Exception {
        when(reviewService.getReviewDetails(SUBMISSION_ID)).thenReturn(reviewDetailsResponse());

        mockMvc.perform(get(
                        "/api/v1/screening/submissions/{submissionId}/review-details",
                        SUBMISSION_ID)
                        .header(RoleAuthorizationInterceptor.ROLE_HEADER, role))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value(SUBMISSION_ID))
                .andExpect(jsonPath("$.preliminaryResult").value("PRELIMINARY_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.correctionInstructions").value("Clarify the allergy response"))
                .andExpect(jsonPath("$.answers[0].questionCode").value("ALLERGY"))
                .andExpect(jsonPath("$.answers[0].typedAnswer").value("yes"))
                .andExpect(jsonPath("$.answers[0].selectedOptions[0].label").value("Yes"))
                .andExpect(jsonPath("$.triggeredRuleEvidence[0].ruleCode").value("ALLERGY_REVIEW"))
                .andExpect(jsonPath("$.triggeredRuleEvidence[0].message").value("Allergy requires review"))
                .andExpect(jsonPath("$.assessment.status").value("INELIGIBLE"));

        verify(reviewService).getReviewDetails(SUBMISSION_ID);
    }

    @Test
    void getReviewDetailsForbidsCitizen() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/screening/submissions/{submissionId}/review-details",
                        SUBMISSION_ID)
                        .header(RoleAuthorizationInterceptor.ROLE_HEADER, "CITIZEN"))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).getReviewDetails(SUBMISSION_ID);
    }

    @Test
    void getReviewDetailsReturnsNotFoundForMissingSubmission() throws Exception {
        when(reviewService.getReviewDetails(SUBMISSION_ID))
                .thenThrow(new ResourceNotFoundException("Screening submission not found with ID: " + SUBMISSION_ID));

        mockMvc.perform(get(
                        "/api/v1/screening/submissions/{submissionId}/review-details",
                        SUBMISSION_ID)
                        .header(RoleAuthorizationInterceptor.ROLE_HEADER, "HEALTHCARE_OFFICER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Screening submission not found with ID: 30"));

        verify(reviewService).getReviewDetails(SUBMISSION_ID);
    }

    private ScreeningReviewDetailsResponse reviewDetailsResponse() {
        ScreeningReviewAnswerResponse answer = new ScreeningReviewAnswerResponse(
                70L,
                80L,
                90L,
                "ALLERGY",
                "Do you have an allergy?",
                QuestionType.SINGLE_CHOICE,
                1,
                true,
                "yes",
                REVIEWED_AT.minusSeconds(600),
                List.of(new ScreeningReviewAnswerOptionResponse(100L, "YES", "Yes", "yes"))
        );
        TriggeredRuleEvidenceResponse evidence = new TriggeredRuleEvidenceResponse(
                110L,
                120L,
                "ALLERGY_REVIEW",
                "Allergy requires review",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                10,
                1,
                REVIEWED_AT.minusSeconds(500)
        );
        EligibilityAssessmentResponse assessment = new EligibilityAssessmentResponse(
                50L,
                SUBMISSION_ID,
                CITIZEN_ID,
                CAMPAIGN_ID,
                REVIEWER_ID,
                EligibilityAssessmentStatus.INELIGIBLE,
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                "Clinical review completed",
                "Allergy confirmed",
                REVIEWED_AT,
                null,
                null,
                null,
                null,
                null,
                null,
                REVIEWED_AT,
                REVIEWED_AT
        );
        return new ScreeningReviewDetailsResponse(
                SUBMISSION_ID,
                CITIZEN_ID,
                CAMPAIGN_ID,
                60L,
                61L,
                com.se2030.vaccinationportal.screening.model.ScreeningSubmissionStatus.CORRECTIONS_REQUIRED,
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                REVIEWED_AT.minusSeconds(3_600),
                REVIEWED_AT.minusSeconds(1_800),
                "Clarify the allergy response",
                REVIEWED_AT.minusSeconds(900),
                List.of(answer),
                List.of(evidence),
                assessment
        );
    }

    private EligibilityAssessment assessment(
            Long assessmentId,
            Long submissionId,
            EligibilityAssessmentStatus status,
            String reason,
            Instant reviewedAt,
            Instant validUntil
    ) {
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(submissionId);

        EligibilityAssessment assessment = new EligibilityAssessment(
                submission,
                CITIZEN_ID,
                CAMPAIGN_ID,
                REVIEWER_ID,
                status,
                PreliminaryScreeningResult.PRELIMINARY_PASS,
                reason,
                reviewedAt,
                validUntil
        );
        assessment.setId(assessmentId);
        assessment.setClinicalNotes("No clinical concerns");
        assessment.setCreatedAt(reviewedAt);
        assessment.setUpdatedAt(reviewedAt);
        return assessment;
    }
}
