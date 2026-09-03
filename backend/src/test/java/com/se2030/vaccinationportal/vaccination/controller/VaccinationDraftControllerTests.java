package com.se2030.vaccinationportal.vaccination.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.exception.AppointmentNotCheckedInException;
import com.se2030.vaccinationportal.vaccination.exception.DuplicateVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationAppointmentNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationDraftNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordAlreadyExistsException;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.service.VaccinationDraftService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class VaccinationDraftControllerTests {

    @Mock
    private VaccinationDraftService draftService;

    private MockMvc mockMvc;
    private JsonMapper objectMapper;
    private VaccinationDraft sampleDraft;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        objectMapper = new JsonMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new VaccinationDraftController(draftService))
                .setControllerAdvice(new VaccinationExceptionHandler())
                .setValidator(validator)
                .build();

        sampleDraft = new VaccinationDraft(101L);
        sampleDraft.setVaccinatorId(201L);
        sampleDraft.setVaccineBatchId(301L);
        sampleDraft.setDoseNumber(1);
        sampleDraft.setAdministrationTime(Instant.parse("2026-08-26T09:00:00Z"));
        ReflectionTestUtils.setField(sampleDraft, "id", 1L);
        ReflectionTestUtils.setField(sampleDraft, "createdAt", Instant.parse("2026-08-26T08:00:00Z"));
        ReflectionTestUtils.setField(sampleDraft, "updatedAt", Instant.parse("2026-08-26T08:00:00Z"));
    }

    @Test
    void createDraft_validRequest_returns201() throws Exception {
        CreateVaccinationDraftRequest request = new CreateVaccinationDraftRequest(
                101L, 201L, 301L, 1,
                Instant.parse("2026-08-26T09:00:00Z"),
                LocalDate.of(2030, 9, 20),
                "Notes",
                false
        );
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class))).thenReturn(sampleDraft);

        mockMvc.perform(post("/api/v1/vaccination/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/vaccination/drafts/1"))
                .andExpect(jsonPath("$.appointmentId", is(101)))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    void createDraft_invalidRequest_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/vaccination/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":0,\"doseNumber\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void createDraft_duplicateAppointment_returns409() throws Exception {
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class)))
                .thenThrow(new DuplicateVaccinationDraftException(
                        "An active vaccination draft already exists for appointment: 101"
                ));

        mockMvc.perform(post("/api/v1/vaccination/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":101}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void createDraft_unknownAppointment_returns404() throws Exception {
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class)))
                .thenThrow(new VaccinationAppointmentNotFoundException("Appointment not found: 101"));

        mockMvc.perform(post("/api/v1/vaccination/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":101}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void createDraft_appointmentNotCheckedIn_returns409() throws Exception {
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class)))
                .thenThrow(new AppointmentNotCheckedInException("Appointment is not checked in: 101"));

        mockMvc.perform(post("/api/v1/vaccination/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":101}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("not checked in")));
    }

    @Test
    void createDraft_existingVaccinationRecord_returns409() throws Exception {
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class)))
                .thenThrow(new VaccinationRecordAlreadyExistsException(
                        "A finalized vaccination record already exists for appointment: 101"
                ));

        mockMvc.perform(post("/api/v1/vaccination/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":101}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void getAllDrafts_returnsArray() throws Exception {
        when(draftService.getAllDrafts()).thenReturn(List.of(sampleDraft));

        mockMvc.perform(get("/api/v1/vaccination/drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));
    }

    @Test
    void getDraftById_existingDraft_returns200() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);

        mockMvc.perform(get("/api/v1/vaccination/drafts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId", is(101)));
    }

    @Test
    void getDraftById_unknownDraft_returns404() throws Exception {
        when(draftService.getDraftById(999L))
                .thenThrow(new VaccinationDraftNotFoundException("Vaccination draft not found with ID: 999"));

        mockMvc.perform(get("/api/v1/vaccination/drafts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void updateDraft_permittedFields_returns200() throws Exception {
        sampleDraft.setDoseNumber(2);
        sampleDraft.setAuthorizedNotes("Updated notes");
        when(draftService.updateDraft(any(Long.class), any(UpdateVaccinationDraftRequest.class)))
                .thenReturn(sampleDraft);

        mockMvc.perform(patch("/api/v1/vaccination/drafts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doseNumber\":2,\"authorizedNotes\":\"Updated notes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doseNumber", is(2)))
                .andExpect(jsonPath("$.authorizedNotes", is("Updated notes")));
    }

    @Test
    void updateDraft_invalidField_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/vaccination/drafts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doseNumber\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void deleteDraft_existingDraft_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/vaccination/drafts/1"))
                .andExpect(status().isNoContent());

        verify(draftService).deleteDraft(1L);
    }

    @Test
    void deleteDraft_unknownDraft_returns404() throws Exception {
        doThrow(new VaccinationDraftNotFoundException("Vaccination draft not found with ID: 999"))
                .when(draftService).deleteDraft(999L);

        mockMvc.perform(delete("/api/v1/vaccination/drafts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }
}
