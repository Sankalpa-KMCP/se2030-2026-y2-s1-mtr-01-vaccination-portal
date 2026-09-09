package com.se2030.vaccinationportal.vaccination.controller;

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

import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionField;
import com.se2030.vaccinationportal.vaccination.service.VaccinationCorrectionService;
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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VaccinationCorrectionController(correctionService))
                .setControllerAdvice(new VaccinationExceptionHandler())
                .build();
    }

    @Test
    void postCreatesAppendOnlyCorrectionAndReturnsSafeResponse() throws Exception {
        VaccinationCorrection correction = correction(301L, VaccinationCorrectionField.DOSE_NUMBER, "1", "2");
        when(correctionService.createCorrection(eq(101L), any(CreateVaccinationCorrectionRequest.class)))
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
                .andExpect(jsonPath("$.status", is("APPLIED")))
                .andExpect(jsonPath("$.correctedBy", is(91)))
                .andExpect(jsonPath("$.correctedAt", is(CORRECTED_AT.toString())));
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
        when(correctionService.createCorrection(eq(101L), any(CreateVaccinationCorrectionRequest.class)))
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
