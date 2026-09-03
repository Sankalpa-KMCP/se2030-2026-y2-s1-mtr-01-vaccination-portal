package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.dto.response.ScreeningFormConsentNoticeResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningFormResponse;
import com.se2030.vaccinationportal.screening.service.ScreeningFormService;
import com.se2030.vaccinationportal.screening.validation.RoleAuthorizationInterceptor;
import com.se2030.vaccinationportal.screening.validation.ScreeningExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CitizenScreeningFormControllerTests {

    @Mock
    private ScreeningFormService screeningFormService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CitizenScreeningFormController(screeningFormService))
                .setControllerAdvice(new ScreeningExceptionHandler())
                .addInterceptors(new RoleAuthorizationInterceptor())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void citizenRetrievesCampaignScreeningForm() throws Exception {
        authenticateAs("CITIZEN");
        ScreeningFormResponse response = new ScreeningFormResponse(
                2L,
                20L,
                3,
                new ScreeningFormConsentNoticeResponse(
                        30L,
                        "PRE_VACCINATION_SCREENING",
                        1,
                        "Synthetic title",
                        "Synthetic content",
                        "synthetic-hash",
                        Instant.parse("2026-09-03T00:00:00Z")
                ),
                List.of()
        );
        when(screeningFormService.getFormForCampaign(2L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/screening/campaigns/{campaignId}/form", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value(2L))
                .andExpect(jsonPath("$.questionnaireVersionId").value(20L))
                .andExpect(jsonPath("$.consentNotice.noticeCode")
                        .value("PRE_VACCINATION_SCREENING"));

        verify(screeningFormService).getFormForCampaign(2L);
    }

    @Test
    void roleOutsideCitizenAndAdministrativeBoundaryIsDenied() throws Exception {
        authenticateAs("HEALTHCARE_OFFICER");

        mockMvc.perform(get("/api/v1/screening/campaigns/{campaignId}/form", 2L))
                .andExpect(status().isForbidden());

        verify(screeningFormService, never()).getFormForCampaign(2L);
    }

    @Test
    void anonymousCallerIsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/screening/campaigns/{campaignId}/form", 2L))
                .andExpect(status().isUnauthorized());
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
