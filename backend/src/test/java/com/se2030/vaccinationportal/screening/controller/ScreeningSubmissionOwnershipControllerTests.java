package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.mapper.ScreeningSubmissionMapper;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.screening.service.CitizenScreeningAccessService;
import com.se2030.vaccinationportal.screening.service.ScreeningSubmissionService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScreeningSubmissionOwnershipControllerTests {

    private static final Long CITIZEN_ID = 100L;
    private static final Long OTHER_CITIZEN_ID = 200L;
    private static final Long SUBMISSION_ID = 300L;

    @Mock
    private ScreeningSubmissionService submissionService;
    @Mock
    private ScreeningSubmissionMapper submissionMapper;
    @Mock
    private ScreeningSubmissionRepository submissionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CitizenScreeningAccessService accessService =
                new CitizenScreeningAccessService(submissionRepository);
        ScreeningSubmissionController controller = new ScreeningSubmissionController(
                submissionService,
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
    void citizenSubmitsForOwnId() throws Exception {
        when(submissionService.submitScreening(
                CITIZEN_ID, 2L, 20L, 30L, Map.of(40L, "true")
        )).thenReturn(submission(SUBMISSION_ID, CITIZEN_ID));

        mockMvc.perform(post("/api/v1/screening/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(CITIZEN_ID)))
                .andExpect(status().isCreated());

        verify(submissionService).submitScreening(
                CITIZEN_ID, 2L, 20L, 30L, Map.of(40L, "true")
        );
    }

    @Test
    void citizenCannotSubmitForAnotherCitizen() throws Exception {
        mockMvc.perform(post("/api/v1/screening/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(OTHER_CITIZEN_ID)))
                .andExpect(status().isForbidden());

        verify(submissionService, never()).submitScreening(any(), any(), any(), any(), anyMap());
    }

    @Test
    void citizenRetrievesOwnSubmission() throws Exception {
        when(submissionService.getSubmissionById(SUBMISSION_ID))
                .thenReturn(submission(SUBMISSION_ID, CITIZEN_ID));

        mockMvc.perform(get("/api/v1/screening/submissions/{id}", SUBMISSION_ID))
                .andExpect(status().isOk());
    }

    @Test
    void citizenCannotRetrieveAnotherCitizensSubmission() throws Exception {
        when(submissionService.getSubmissionById(SUBMISSION_ID))
                .thenReturn(submission(SUBMISSION_ID, OTHER_CITIZEN_ID));

        mockMvc.perform(get("/api/v1/screening/submissions/{id}", SUBMISSION_ID))
                .andExpect(status().isForbidden());

        verify(submissionMapper, never()).toResponse(any());
    }

    @Test
    void citizenListsOwnSubmissionHistory() throws Exception {
        when(submissionService.getSubmissionsForCitizen(CITIZEN_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/screening/citizens/{citizenId}/submissions", CITIZEN_ID))
                .andExpect(status().isOk());

        verify(submissionService).getSubmissionsForCitizen(CITIZEN_ID);
    }

    @Test
    void citizenCannotListAnotherCitizensSubmissionHistory() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/screening/citizens/{citizenId}/submissions",
                        OTHER_CITIZEN_ID
                ))
                .andExpect(status().isForbidden());

        verify(submissionService, never()).getSubmissionsForCitizen(any());
    }

    @Test
    void citizenRetrievesOwnActiveSubmission() throws Exception {
        when(submissionService.getActiveSubmission(CITIZEN_ID, 2L)).thenReturn(Optional.empty());

        mockMvc.perform(get(
                        "/api/v1/screening/citizens/{citizenId}/campaigns/{campaignId}/active-submission",
                        CITIZEN_ID,
                        2L
                ))
                .andExpect(status().isNoContent());

        verify(submissionService).getActiveSubmission(CITIZEN_ID, 2L);
    }

    @Test
    void citizenCannotRetrieveAnotherCitizensActiveSubmission() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/screening/citizens/{citizenId}/campaigns/{campaignId}/active-submission",
                        OTHER_CITIZEN_ID,
                        2L
                ))
                .andExpect(status().isForbidden());

        verify(submissionService, never()).getActiveSubmission(any(), any());
    }

    @Test
    void citizenResubmitsOwnCorrectionSubmission() throws Exception {
        ScreeningSubmission submission = submission(SUBMISSION_ID, CITIZEN_ID);
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));
        when(submissionService.resubmitCorrections(SUBMISSION_ID, Map.of(40L, "false")))
                .thenReturn(submission);

        mockMvc.perform(post("/api/v1/screening/submissions/{submissionId}/resubmit", SUBMISSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":{\"40\":\"false\"}}"))
                .andExpect(status().isOk());

        verify(submissionService).resubmitCorrections(SUBMISSION_ID, Map.of(40L, "false"));
    }

    @Test
    void citizenCannotResubmitAnotherCitizensSubmission() throws Exception {
        when(submissionRepository.findById(SUBMISSION_ID))
                .thenReturn(Optional.of(submission(SUBMISSION_ID, OTHER_CITIZEN_ID)));

        mockMvc.perform(post("/api/v1/screening/submissions/{submissionId}/resubmit", SUBMISSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":{\"40\":\"false\"}}"))
                .andExpect(status().isForbidden());

        verify(submissionService, never()).resubmitCorrections(any(), anyMap());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR"})
    void administrativeRolesRetainSubmitForCitizenBehavior(String role) throws Exception {
        authenticate(900L, role);
        when(submissionService.submitScreening(
                OTHER_CITIZEN_ID, 2L, 20L, 30L, Map.of(40L, "true")
        )).thenReturn(submission(SUBMISSION_ID, OTHER_CITIZEN_ID));

        mockMvc.perform(post("/api/v1/screening/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(OTHER_CITIZEN_ID)))
                .andExpect(status().isCreated());
    }

    @Test
    void healthcareOfficerIsNotGrantedSubmissionCreation() throws Exception {
        authenticate(900L, "ROLE_HEALTHCARE_OFFICER");

        mockMvc.perform(post("/api/v1/screening/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(OTHER_CITIZEN_ID)))
                .andExpect(status().isForbidden());

        verify(submissionService, never()).submitScreening(any(), any(), any(), any(), anyMap());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_HEALTHCARE_OFFICER", "ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR"})
    void privilegedReadRolesRetainCrossCitizenSubmissionAccess(String role) throws Exception {
        authenticate(900L, role);
        when(submissionService.getSubmissionById(SUBMISSION_ID))
                .thenReturn(submission(SUBMISSION_ID, OTHER_CITIZEN_ID));

        mockMvc.perform(get("/api/v1/screening/submissions/{id}", SUBMISSION_ID))
                .andExpect(status().isOk());
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

    private ScreeningSubmission submission(Long submissionId, Long citizenId) {
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(submissionId);
        submission.setCitizenId(citizenId);
        return submission;
    }

    private String submissionJson(Long citizenId) {
        return """
                {
                  "citizenId": %d,
                  "campaignId": 2,
                  "questionnaireVersionId": 20,
                  "consentNoticeVersionId": 30,
                  "answers": {"40": "true"}
                }
                """.formatted(citizenId);
    }
}
