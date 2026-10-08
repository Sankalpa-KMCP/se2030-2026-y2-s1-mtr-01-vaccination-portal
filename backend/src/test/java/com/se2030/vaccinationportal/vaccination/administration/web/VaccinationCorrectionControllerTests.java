package com.se2030.vaccinationportal.vaccination.administration.web;

import com.se2030.vaccinationportal.vaccination.web.VaccinationExceptionHandler;


import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.VaccinationCorrectionLifecycleResponse;
import com.se2030.vaccinationportal.vaccination.administration.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.administration.mapper.VaccinationCorrectionMapper;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionField;
import com.se2030.vaccinationportal.vaccination.administration.service.VaccinationCorrectionLifecycleService;
import com.se2030.vaccinationportal.vaccination.administration.service.VaccinationCorrectionService;
import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
class VaccinationCorrectionControllerTests {

    private static final Instant CORRECTED_AT = Instant.parse("2026-09-08T10:00:00Z");

    @Mock
    private VaccinationCorrectionService correctionService;

    @Mock
    private VaccinationCorrectionLifecycleService lifecycleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new VaccinationCorrectionController(correctionService, lifecycleService)
                )
                .setControllerAdvice(new VaccinationExceptionHandler())
                .build();
    }

    @Test
    void postCreatesPendingCorrectionAndReturnsSafeResponse() throws Exception {
        VaccinationCorrection correction = correction(301L, VaccinationCorrectionField.DOSE_NUMBER, "1", "2");
        when(correctionService.createCorrection(eq(101L), any(CreateVaccinationCorrectionRequest.class), any()))
                .thenReturn(correction);

        mockMvc.perform(post("/api/v1/vaccination/records/101/corrections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fieldName": "doseNumber",
                                  "newValue": "2",
                                  "reason": "Dose number correction",
                                  "correctedBy": 91
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/vaccination/records/101/corrections/301"
                ))
                .andExpect(jsonPath("$.id", is(301)))
                .andExpect(jsonPath("$.vaccinationRecordId", is(101)))
                .andExpect(jsonPath("$.fieldName", is("doseNumber")))
                .andExpect(jsonPath("$.oldValue", is("1")))
                .andExpect(jsonPath("$.newValue", is("2")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.correctedBy", is(91)))
                .andExpect(jsonPath("$.correctedAt", is(CORRECTED_AT.toString())));
    }

    @Test
    void approveCorrectionReturnsApprovedStatus() throws Exception {
        VaccinationCorrection correction = correction(301L, VaccinationCorrectionField.DOSE_NUMBER, "1", "2");
        correction.approve(501L, CORRECTED_AT, "Approved by reviewer");
        when(correctionService.approveCorrection(eq(101L), eq(301L), any(), any()))
                .thenReturn(correction);

        mockMvc.perform(post("/api/v1/vaccination/records/101/corrections/301/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Approved by reviewer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(301)))
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    @Test
    void rejectCorrectionReturnsRejectedStatus() throws Exception {
        VaccinationCorrection correction = correction(301L, VaccinationCorrectionField.DOSE_NUMBER, "1", "2");
        correction.reject(501L, CORRECTED_AT, "Rejected due to invalid dose");
        when(correctionService.rejectCorrection(eq(101L), eq(301L), any(), any()))
                .thenReturn(correction);

        mockMvc.perform(post("/api/v1/vaccination/records/101/corrections/301/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Rejected due to invalid dose\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(301)))
                .andExpect(jsonPath("$.status", is("REJECTED")));
    }

    @Test
    void applyApprovedCorrectionReturnsLifecycleResponse() throws Exception {
        VaccinationCorrection correction = correction(301L, VaccinationCorrectionField.DOSE_NUMBER, "1", "2");
        correction.approve(501L, CORRECTED_AT, "Approved");
        correction.apply();
        when(lifecycleService.applyApprovedCorrection(eq(101L), eq(301L), any()))
                .thenReturn(new VaccinationCorrectionLifecycleResponse(
                        VaccinationCorrectionMapper.toResponse(correction),
                        true,
                        false,
                        null
                ));

        mockMvc.perform(post("/api/v1/vaccination/records/101/corrections/301/apply"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correction.id", is(301)))
                .andExpect(jsonPath("$.correction.status", is("APPLIED")))
                .andExpect(jsonPath("$.certificateImpacted", is(true)))
                .andExpect(jsonPath("$.certificateReissued", is(false)));
    }


    @Test
    void getReturnsCorrectionHistory() throws Exception {
        VaccinationCorrection first = correction(301L, VaccinationCorrectionField.DOSE_NUMBER, "1", "2");
        VaccinationCorrection second = correction(
                302L,
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "Original",
                "Corrected"
        );
        when(correctionService.getCorrectionHistory(101L)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/vaccination/records/101/corrections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(301)))
                .andExpect(jsonPath("$[1].id", is(302)));
    }

    @Test
    void blankReasonReturnsStructured400() throws Exception {
        mockMvc.perform(post("/api/v1/vaccination/records/101/corrections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fieldName": "doseNumber",
                                  "newValue": "2",
                                  "reason": " ",
                                  "correctedBy": 91
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("reason")));
    }

    @Test
    void unsupportedFieldReturnsStructured400() throws Exception {
        when(correctionService.createCorrection(eq(101L), any(CreateVaccinationCorrectionRequest.class), any()))
                .thenThrow(new UnsupportedVaccinationCorrectionFieldException(
                        "Unsupported vaccination correction field: citizenId"
                ));

        mockMvc.perform(post("/api/v1/vaccination/records/101/corrections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fieldName": "citizenId",
                                  "newValue": "999",
                                  "reason": "Incorrect citizen",
                                  "correctedBy": 91
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Unsupported")));
    }

    @Test
    void unknownVaccinationRecordReturnsStructured404() throws Exception {
        when(correctionService.getCorrectionHistory(999L))
                .thenThrow(new VaccinationRecordNotFoundException(
                        "Vaccination record not found with ID: 999"
                ));

        mockMvc.perform(get("/api/v1/vaccination/records/999/corrections"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("999")));
    }

    @Test
    void deleteEndpointIsNotExposed() throws Exception {
        mockMvc.perform(delete("/api/v1/vaccination/records/101/corrections/301"))
                .andExpect(status().isNotFound());
    }

    private VaccinationCorrection correction(
            Long id,
            VaccinationCorrectionField field,
            String oldValue,
            String newValue
    ) {
        VaccinationCorrection correction = new VaccinationCorrection(
                101L,
                field,
                oldValue,
                newValue,
                "Correction reason",
                91L,
                CORRECTED_AT
        );
        ReflectionTestUtils.setField(correction, "id", id);
        return correction;
    }
}
