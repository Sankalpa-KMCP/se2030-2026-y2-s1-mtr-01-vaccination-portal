package com.se2030.vaccinationportal.vaccination;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.service.VaccinationDraftService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VaccinationDraftSecurityAuthorizationIntegrationTests {
    private static final String BASE = "/api/v1/vaccination/drafts";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;
    @MockitoBean private VaccinationDraftService draftService;

    private String token(Long userId, String role) {
        return "Bearer " + jwtTokenService.generateToken(userId, "staff" + userId + "@test.gov", List.of(role));
    }

    private String token(String role) {
        return token(201L, role);
    }

    private List<MockHttpServletRequestBuilder> endpoints() {
        return List.of(
                MockMvcRequestBuilders.post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"appointmentId\":101}"),
                MockMvcRequestBuilders.get(BASE),
                MockMvcRequestBuilders.get(BASE + "/1"),
                MockMvcRequestBuilders.patch(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"doseNumber\":2}"),
                MockMvcRequestBuilders.delete(BASE + "/1?reason=Removal"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_CITIZEN", "ROLE_HEALTHCARE_OFFICER", "ROLE_ADMIN", "ROLE_CENTER_MANAGER",
            "ROLE_CAMPAIGN_OFFICER", "ROLE_INVENTORY_OFFICER", "ROLE_MANAGEMENT_USER"})
    void otherRolesCannotAccessAnyCrudEndpoint(String role) throws Exception {
        for (var endpoint : endpoints()) {
            mockMvc.perform(endpoint.header(HttpHeaders.AUTHORIZATION, token(role)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void anonymousCannotAccessAnyCrudEndpoint() throws Exception {
        for (var endpoint : endpoints()) {
            mockMvc.perform(endpoint).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void vaccinatorCanAccessAllCrudEndpointsAndCreateUsesPrincipalId() throws Exception {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccinatorId(201L);
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class), eq(201L))).thenReturn(draft);
        when(draftService.getAllDraftsByVaccinatorId(201L)).thenReturn(List.of(draft));
        when(draftService.getDraftById(1L)).thenReturn(draft);
        when(draftService.updateDraft(eq(1L), any(UpdateVaccinationDraftRequest.class))).thenReturn(draft);
        int[] statuses = {201, 200, 200, 200, 204};
        int index = 0;
        for (var endpoint : endpoints()) {
            mockMvc.perform(endpoint.header(HttpHeaders.AUTHORIZATION, token("ROLE_VACCINATOR")))
                    .andExpect(status().is(statuses[index++]));
        }
        verify(draftService).createDraft(any(CreateVaccinationDraftRequest.class), eq(201L));
        verify(draftService).deleteDraft(1L, "Removal", 201L);
    }

    @Test
    void otherVaccinatorCannotReadUpdateOrDeleteDraft() throws Exception {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccinatorId(201L);
        when(draftService.getDraftById(1L)).thenReturn(draft);

        String otherVaccinator = token(202L, "ROLE_VACCINATOR");

        mockMvc.perform(MockMvcRequestBuilders.get(BASE + "/1").header(HttpHeaders.AUTHORIZATION, otherVaccinator))
                .andExpect(status().isForbidden());

        mockMvc.perform(MockMvcRequestBuilders.patch(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doseNumber\":2}")
                        .header(HttpHeaders.AUTHORIZATION, otherVaccinator))
                .andExpect(status().isForbidden());

        mockMvc.perform(MockMvcRequestBuilders.delete(BASE + "/1?reason=Mistake")
                        .header(HttpHeaders.AUTHORIZATION, otherVaccinator))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminCanAccessOtherVaccinatorsDraft() throws Exception {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccinatorId(201L);
        when(draftService.getDraftById(1L)).thenReturn(draft);
        when(draftService.getAllDrafts()).thenReturn(List.of(draft));
        when(draftService.updateDraft(eq(1L), any(UpdateVaccinationDraftRequest.class))).thenReturn(draft);

        String superAdmin = token(999L, "ROLE_SUPER_ADMIN");

        mockMvc.perform(MockMvcRequestBuilders.get(BASE).header(HttpHeaders.AUTHORIZATION, superAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get(BASE + "/1").header(HttpHeaders.AUTHORIZATION, superAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.patch(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doseNumber\":2}")
                        .header(HttpHeaders.AUTHORIZATION, superAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.delete(BASE + "/1?reason=SuperAdmin+cleanup")
                        .header(HttpHeaders.AUTHORIZATION, superAdmin))
                .andExpect(status().isNoContent());

        verify(draftService).deleteDraft(1L, "SuperAdmin cleanup", 999L);
    }
}
