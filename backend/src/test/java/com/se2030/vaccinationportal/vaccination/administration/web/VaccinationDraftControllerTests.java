package com.se2030.vaccinationportal.vaccination.administration.web;

import com.se2030.vaccinationportal.vaccination.web.VaccinationExceptionHandler;


import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.administration.exception.AppointmentNotCheckedInException;
import com.se2030.vaccinationportal.vaccination.administration.exception.DuplicateVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationAppointmentNotFoundException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationDraftNotFoundException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationRecordAlreadyExistsException;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.administration.service.VaccinationDraftService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;
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

import com.se2030.vaccinationportal.vaccination.administration.dto.FinalizeVaccinationResponse;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraftStatus;
import com.se2030.vaccinationportal.vaccination.administration.service.VaccinationFinalizationService;

@ExtendWith(MockitoExtension.class)
class VaccinationDraftControllerTests {

    @Mock
    private VaccinationDraftService draftService;

    @Mock
    private VaccinationFinalizationService finalizationService;

    private MockMvc mockMvc;
    private JsonMapper objectMapper;
    private VaccinationDraft sampleDraft;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        objectMapper = new JsonMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new VaccinationDraftController(draftService, finalizationService))
                .setControllerAdvice(new VaccinationExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                            NativeWebRequest request, WebDataBinderFactory factory) {
                        return UserPrincipal.fromClaims(201L, "vaccinator@test.gov", List.of("ROLE_VACCINATOR"));
                    }
                })
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
                101L, 301L, 1,
                Instant.parse("2026-08-26T09:00:00Z"),
                LocalDate.of(2030, 9, 20),
                "Notes"
        );
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class), eq(201L))).thenReturn(sampleDraft);

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
    void createDraft_futureAdministrationTime_returns400() throws Exception {
        String futureInstant = Instant.now().plusSeconds(86400).toString();
        mockMvc.perform(post("/api/v1/vaccination/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":101,\"administrationTime\":\"" + futureInstant + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("administrationTime")))
                .andExpect(jsonPath("$.fieldErrors[0].message", containsString("cannot be in the future")));
    }

    @Test
    void createDraft_duplicateAppointment_returns409() throws Exception {
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class), eq(201L)))
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
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class), eq(201L)))
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
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class), eq(201L)))
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
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class), eq(201L)))
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
    void createDraft_unavailableBatchExpired_returns409() throws Exception {
        when(draftService.createDraft(any(CreateVaccinationDraftRequest.class), eq(201L)))
                .thenThrow(new com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException(
                        com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.EXPIRED_BATCH
                ));

        mockMvc.perform(post("/api/v1/vaccination/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":101,\"vaccineBatchId\":101}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("EXPIRED_BATCH")));
    }

    @Test
    void updateDraft_unavailableBatchInsufficientStock_returns409() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);
        when(draftService.updateDraft(eq(1L), any(UpdateVaccinationDraftRequest.class)))
                .thenThrow(new com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException(
                        com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.INSUFFICIENT_STOCK
                ));

        mockMvc.perform(patch("/api/v1/vaccination/drafts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vaccineBatchId\":103}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("INSUFFICIENT_STOCK")));
    }

    @Test
    void getAllDrafts_returnsArray() throws Exception {
        when(draftService.getAllDraftsByVaccinatorId(201L)).thenReturn(List.of(sampleDraft));

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
    void getDraftById_otherVaccinator_returns403() throws Exception {
        sampleDraft.setVaccinatorId(999L);
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);

        mockMvc.perform(get("/api/v1/vaccination/drafts/1"))
                .andExpect(status().isForbidden());
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
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);
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
    void updateDraft_otherVaccinator_returns403() throws Exception {
        sampleDraft.setVaccinatorId(999L);
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);

        mockMvc.perform(patch("/api/v1/vaccination/drafts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doseNumber\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateDraft_invalidField_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/vaccination/drafts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doseNumber\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Validation failed")));
        org.mockito.Mockito.verifyNoInteractions(draftService);
    }

    @Test
    void updateDraft_notEditableDraft_returns400() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);
        when(draftService.updateDraft(any(Long.class), any(UpdateVaccinationDraftRequest.class)))
                .thenThrow(new com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationDraftException(
                        "Vaccination draft is not editable: 1"
                ));

        mockMvc.perform(patch("/api/v1/vaccination/drafts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doseNumber\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("not editable")));
    }

    @Test
    void updateDraft_unknownDraft_returns404() throws Exception {
        when(draftService.getDraftById(999L))
                .thenThrow(new VaccinationDraftNotFoundException("Vaccination draft not found with ID: 999"));

        mockMvc.perform(patch("/api/v1/vaccination/drafts/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doseNumber\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "{}", "null", "{", "{\"appointmentId\":null}", "{\"appointmentId\":\"abc\"}",
            "{\"appointmentId\":101,\"vaccineBatchId\":0}",
            "{\"appointmentId\":101,\"administrationTime\":\"2099-01-01T00:00:00Z\"}",
            "{\"appointmentId\":101,\"nextDoseDate\":\"2000-01-01\"}"})
    void createDraftMalformedOrInvalidPayloadReturns400(String payload) throws Exception {
        mockMvc.perform(post("/api/v1/vaccination/drafts").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(draftService);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"vaccinatorId", "physicalAdministrationConfirmed", "status", "id"})
    void publicRequestsRejectServerControlledFields(String field) throws Exception {
        String value = field.equals("physicalAdministrationConfirmed") ? "true" : "999";
        mockMvc.perform(post("/api/v1/vaccination/drafts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"appointmentId\":101,\"" + field + "\":" + value + "}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/vaccination/drafts/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"doseNumber\":2,\"" + field + "\":" + value + "}"))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(draftService);
    }

    @Test
    void oversizedNotesReturns400() throws Exception {
        mockMvc.perform(patch("/api/v1/vaccination/drafts/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"authorizedNotes\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(draftService);
    }
    @Test
    void deleteDraft_existingDraft_returns204() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);

        mockMvc.perform(delete("/api/v1/vaccination/drafts/1?reason=Patient+left"))
                .andExpect(status().isNoContent());

        verify(draftService).deleteDraft(1L, "Patient left", 201L);
    }

    @Test
    void deleteDraft_withBody_returns204() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);

        mockMvc.perform(delete("/api/v1/vaccination/drafts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Duplicate entry entered in error\"}"))
                .andExpect(status().isNoContent());

        verify(draftService).deleteDraft(1L, "Duplicate entry entered in error", 201L);
    }

    @Test
    void deleteDraft_missingReason_returns400() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);

        mockMvc.perform(delete("/api/v1/vaccination/drafts/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("reason is required")));
    }

    @Test
    void deleteDraft_otherVaccinator_returns403() throws Exception {
        sampleDraft.setVaccinatorId(999L);
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);

        mockMvc.perform(delete("/api/v1/vaccination/drafts/1?reason=Cancelled"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteDraft_unknownDraft_returns404() throws Exception {
        doThrow(new VaccinationDraftNotFoundException("Vaccination draft not found with ID: 999"))
                .when(draftService).getDraftById(999L);

        mockMvc.perform(delete("/api/v1/vaccination/drafts/999?reason=Cancelled"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void confirmAdministration_validDraft_returns200() throws Exception {
        sampleDraft.setStatus(VaccinationDraftStatus.READY_FOR_FINALIZATION);
        sampleDraft.setPhysicalAdministrationConfirmed(true);
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);
        when(draftService.confirmPhysicalAdministration(1L)).thenReturn(sampleDraft);

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/confirm-administration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.physicalAdministrationConfirmed", is(true)))
                .andExpect(jsonPath("$.status", is("READY_FOR_FINALIZATION")));

        verify(draftService).confirmPhysicalAdministration(1L);
    }

    @Test
    void confirmAdministration_otherVaccinator_returns403() throws Exception {
        sampleDraft.setVaccinatorId(999L);
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/confirm-administration"))
                .andExpect(status().isForbidden());
    }

    @Test
    void finalizeDraft_validDraft_returns200WithCertificate() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);

        FinalizeVaccinationResponse response = new FinalizeVaccinationResponse(
                1L, 777L, 101L, 501L, 301L, 1, sampleDraft.getAdministrationTime(),
                "CERT-2026-XYZ", "tok_verify_123", null
        );
        when(finalizationService.finalizeDraft(1L, 201L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/finalize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftId", is(1)))
                .andExpect(jsonPath("$.vaccinationRecordId", is(777)))
                .andExpect(jsonPath("$.certificateReference", is("CERT-2026-XYZ")))
                .andExpect(jsonPath("$.verificationToken", is("tok_verify_123")));

        verify(finalizationService).finalizeDraft(1L, 201L);
    }

    @Test
    void finalizeDraft_otherVaccinator_returns403() throws Exception {
        sampleDraft.setVaccinatorId(999L);
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/finalize"))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmAdministration_nonexistentBatch_returns404WithVaccinationEnvelope() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);
        when(draftService.confirmPhysicalAdministration(1L)).thenThrow(
                new com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException(
                        com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.BATCH_NOT_FOUND
                )
        );

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/confirm-administration"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("BATCH_NOT_FOUND")))
                .andExpect(jsonPath("$.path", is("/api/v1/vaccination/drafts/1/confirm-administration")));
    }

    @Test
    void confirmAdministration_unavailableBatchExpired_returns409WithVaccinationEnvelope() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);
        when(draftService.confirmPhysicalAdministration(1L)).thenThrow(
                new com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException(
                        com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.EXPIRED_BATCH
                )
        );

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/confirm-administration"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("EXPIRED_BATCH")))
                .andExpect(jsonPath("$.path", is("/api/v1/vaccination/drafts/1/confirm-administration")));
    }

    @Test
    void confirmAdministration_unavailableBatchInsufficientStock_returns409WithVaccinationEnvelope() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);
        when(draftService.confirmPhysicalAdministration(1L)).thenThrow(
                new com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException(
                        com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.INSUFFICIENT_STOCK
                )
        );

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/confirm-administration"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("INSUFFICIENT_STOCK")));
    }

    @Test
    void confirmAdministration_vaccineTypeMismatch_returns409WithVaccinationEnvelope() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);
        when(draftService.confirmPhysicalAdministration(1L)).thenThrow(
                new com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationBatchVaccineTypeMismatchException(10L, 20L)
        );

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/confirm-administration"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("Selected vaccine batch type 20 does not match the campaign vaccine type 10")));
    }

    @Test
    void finalizeDraft_nonexistentBatch_returns404WithVaccinationEnvelope() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);
        when(finalizationService.finalizeDraft(1L, 201L)).thenThrow(
                new com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException(
                        com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.BATCH_NOT_FOUND
                )
        );

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/finalize"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("BATCH_NOT_FOUND")));
    }

    @Test
    void finalizeDraft_unavailableBatch_returns409WithVaccinationEnvelope() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);
        when(finalizationService.finalizeDraft(1L, 201L)).thenThrow(
                new com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException(
                        com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.INACTIVE_BATCH
                )
        );

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/finalize"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("INACTIVE_BATCH")));
    }

    @Test
    void confirmAndFinalizeDraft_delegatesToFinalizationService() throws Exception {
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);
        FinalizeVaccinationResponse expected = new FinalizeVaccinationResponse(
                1L, 100L, 101L, 501L, 301L, 1, sampleDraft.getAdministrationTime(),
                "CERT-123", "token-xyz", null
        );
        when(finalizationService.confirmAndFinalizeDraft(1L, 201L)).thenReturn(expected);

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/confirm-and-finalize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftId", is(1)))
                .andExpect(jsonPath("$.certificateReference", is("CERT-123")));

        verify(finalizationService).confirmAndFinalizeDraft(1L, 201L);
    }

    @Test
    void confirmAndFinalizeDraft_otherVaccinator_returns403() throws Exception {
        sampleDraft.setVaccinatorId(999L);
        when(draftService.getDraftById(1L)).thenReturn(sampleDraft);

        mockMvc.perform(post("/api/v1/vaccination/drafts/1/confirm-and-finalize"))
                .andExpect(status().isForbidden());
    }
}
