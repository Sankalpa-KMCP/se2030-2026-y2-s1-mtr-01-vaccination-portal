package com.se2030.vaccinationportal.screening.eligibility.web;

import com.se2030.vaccinationportal.screening.eligibility.mapper.EligibilityAssessmentMapper;
import com.se2030.vaccinationportal.screening.submission.mapper.ScreeningSubmissionMapper;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.screening.submission.service.CitizenScreeningAccessService;
import com.se2030.vaccinationportal.screening.eligibility.service.EligibilityReviewService;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EligibilityReviewOwnershipControllerTests {

    private static final Long CITIZEN_ID = 100L;
    private static final Long OTHER_CITIZEN_ID = 200L;
    private static final Long SUBMISSION_ID = 300L;

    @Mock
    private EligibilityReviewService reviewService;
    @Mock
    private ScreeningSubmissionMapper submissionMapper;
    @Mock
    private ScreeningSubmissionRepository submissionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CitizenScreeningAccessService accessService =
                new CitizenScreeningAccessService(submissionRepository);
        EligibilityReviewController controller = new EligibilityReviewController(
                reviewService,
                new EligibilityAssessmentMapper(),
                submissionMapper,
                accessService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ScreeningExceptionHandler())
                .addInterceptors(new RoleAuthorizationInterceptor())
                .build();
        authenticate(CITIZEN_ID, "ROLE_CITIZEN");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void citizenReadsAssessmentForOwnSubmission() throws Exception {
        when(submissionRepository.findById(SUBMISSION_ID))
                .thenReturn(Optional.of(submission(CITIZEN_ID)));
        when(reviewService.getAssessmentBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(
                        "/api/v1/screening/assessments/submissions/{submissionId}",
                        SUBMISSION_ID
                ))
                .andExpect(status().isNoContent());
    }

    @Test
    void citizenCannotReadAssessmentForAnotherCitizensSubmission() throws Exception {
        when(submissionRepository.findById(SUBMISSION_ID))
                .thenReturn(Optional.of(submission(OTHER_CITIZEN_ID)));

        mockMvc.perform(get(
                        "/api/v1/screening/assessments/submissions/{submissionId}",
                        SUBMISSION_ID
                ))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).getAssessmentBySubmissionId(any());
    }

    @Test
    void citizenReadsOwnAssessmentHistory() throws Exception {
        when(reviewService.getAssessmentsForCitizen(CITIZEN_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/screening/citizens/{citizenId}/assessments", CITIZEN_ID))
                .andExpect(status().isOk());
    }

    @Test
    void citizenCannotReadAnotherCitizensAssessmentHistory() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/screening/citizens/{citizenId}/assessments",
                        OTHER_CITIZEN_ID
                ))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).getAssessmentsForCitizen(any());
    }

    @Test
    void citizenChecksOwnCampaignEligibility() throws Exception {
        when(reviewService.isCitizenCurrentlyEligible(CITIZEN_ID, 2L)).thenReturn(false);

        mockMvc.perform(get(
                        "/api/v1/screening/citizens/{citizenId}/campaigns/{campaignId}/eligibility",
                        CITIZEN_ID,
                        2L
                ))
                .andExpect(status().isOk());
    }

    @Test
    void citizenCannotCheckAnotherCitizensCampaignEligibility() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/screening/citizens/{citizenId}/campaigns/{campaignId}/eligibility",
                        OTHER_CITIZEN_ID,
                        2L
                ))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).isCitizenCurrentlyEligible(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_HEALTHCARE_OFFICER", "ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR"})
    void privilegedRolesRetainCrossCitizenAssessmentRead(String role) throws Exception {
        authenticate(900L, role);
        when(reviewService.getAssessmentBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(
                        "/api/v1/screening/assessments/submissions/{submissionId}",
                        SUBMISSION_ID
                ))
                .andExpect(status().isNoContent());

        verify(submissionRepository, never()).findById(any());
    }

    private void authenticate(Long userId, String role) {
        UserPrincipal principal = UserPrincipal.fromClaims(
                userId,
                "synthetic-user@portal.test",
                List.of(role)
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }

    private ScreeningSubmission submission(Long citizenId) {
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(SUBMISSION_ID);
        submission.setCitizenId(citizenId);
        return submission;
    }
}
