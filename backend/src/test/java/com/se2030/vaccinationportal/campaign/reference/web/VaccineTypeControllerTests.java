package com.se2030.vaccinationportal.campaign.reference.web;

import com.se2030.vaccinationportal.campaign.web.GlobalCampaignExceptionHandler;


import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.service.VaccineTypeService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class VaccineTypeControllerTests {

    private MockMvc mockMvc;

    @Mock
    private VaccineTypeService vaccineTypeService;

    private VaccineType sampleVaccine1;
    private VaccineType sampleVaccine2;
    private VaccineType sampleVaccineInactive;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VaccineTypeController(vaccineTypeService))
                .setControllerAdvice(new GlobalCampaignExceptionHandler())
                .build();

        sampleVaccine1 = new VaccineType("VAX-MRNA-01", "COVID-19 mRNA Vaccine", "dose", true);
        ReflectionTestUtils.setField(sampleVaccine1, "id", 101L);

        sampleVaccine2 = new VaccineType("VAX-INACT-02", "Inactivated Viral Vaccine", "dose", true);
        ReflectionTestUtils.setField(sampleVaccine2, "id", 102L);

        sampleVaccineInactive = new VaccineType("VAX-OLD-03", "Legacy Vaccine", "dose", false);
        ReflectionTestUtils.setField(sampleVaccineInactive, "id", 103L);
    }

    @Test
    @DisplayName("GET /api/v1/vaccine-types returns list of active vaccine types by default")
    void getActiveVaccineTypes_returnsList() throws Exception {
        when(vaccineTypeService.getVaccineTypes(false)).thenReturn(List.of(sampleVaccine1, sampleVaccine2));

        mockMvc.perform(get("/api/v1/vaccine-types").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(101)))
                .andExpect(jsonPath("$[0].code", is("VAX-MRNA-01")))
                .andExpect(jsonPath("$[0].name", is("COVID-19 mRNA Vaccine")))
                .andExpect(jsonPath("$[0].doseUnit", is("dose")))
                .andExpect(jsonPath("$[0].active", is(true)))
                .andExpect(jsonPath("$[1].id", is(102)))
                .andExpect(jsonPath("$[1].code", is("VAX-INACT-02")))
                .andExpect(jsonPath("$[1].name", is("Inactivated Viral Vaccine")))
                .andExpect(jsonPath("$[1].doseUnit", is("dose")))
                .andExpect(jsonPath("$[1].active", is(true)));

        verify(vaccineTypeService).getVaccineTypes(false);
    }

    @Test
    @DisplayName("GET /api/v1/vaccine-types?includeInactive=true returns all vaccine types")
    void getAllVaccineTypes_withIncludeInactive() throws Exception {
        when(vaccineTypeService.getVaccineTypes(true))
                .thenReturn(List.of(sampleVaccine1, sampleVaccine2, sampleVaccineInactive));

        mockMvc.perform(get("/api/v1/vaccine-types?includeInactive=true").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].id", is(103)))
                .andExpect(jsonPath("$[2].code", is("VAX-OLD-03")))
                .andExpect(jsonPath("$[2].active", is(false)));

        verify(vaccineTypeService).getVaccineTypes(true);
    }

    @Test
    @DisplayName("GET /api/v1/vaccine-types returns empty array when no active vaccine types exist")
    void getActiveVaccineTypes_returnsEmptyList() throws Exception {
        when(vaccineTypeService.getVaccineTypes(false)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/vaccine-types").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(vaccineTypeService).getVaccineTypes(false);
    }

    @Test
    @DisplayName("POST /api/v1/vaccine-types creates new vaccine type")
    void createVaccineType_success() throws Exception {
        VaccineType created = new VaccineType("VAX-NEW-01", "New Recombinant Vaccine", "dose", true);
        ReflectionTestUtils.setField(created, "id", 104L);

        when(vaccineTypeService.createVaccineType("VAX-NEW-01", "New Recombinant Vaccine", "dose"))
                .thenReturn(created);

        String json = """
                {
                    "code": "VAX-NEW-01",
                    "name": "New Recombinant Vaccine",
                    "doseUnit": "dose"
                }
                """;

        mockMvc.perform(post("/api/v1/vaccine-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(104)))
                .andExpect(jsonPath("$.code", is("VAX-NEW-01")))
                .andExpect(jsonPath("$.name", is("New Recombinant Vaccine")))
                .andExpect(jsonPath("$.doseUnit", is("dose")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/vaccine-types fails on blank code/name")
    void createVaccineType_validationFailure() throws Exception {
        String json = """
                {
                    "code": "",
                    "name": ""
                }
                """;

        mockMvc.perform(post("/api/v1/vaccine-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/vaccine-types returns 400 when duplicate code exists")
    void createVaccineType_duplicateCode() throws Exception {
        when(vaccineTypeService.createVaccineType(eq("VAX-MRNA-01"), any(), any()))
                .thenThrow(new IllegalArgumentException("Vaccine type already exists with code: VAX-MRNA-01"));

        String json = """
                {
                    "code": "VAX-MRNA-01",
                    "name": "Duplicate MRNA",
                    "doseUnit": "dose"
                }
                """;

        mockMvc.perform(post("/api/v1/vaccine-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Vaccine type already exists with code: VAX-MRNA-01")));
    }

    @Test
    @DisplayName("PUT /api/v1/vaccine-types/{id} updates existing vaccine type")
    void updateVaccineType_success() throws Exception {
        VaccineType updated = new VaccineType("VAX-MRNA-01", "Updated mRNA Name", "vial", true);
        ReflectionTestUtils.setField(updated, "id", 101L);

        when(vaccineTypeService.updateVaccineType(101L, "Updated mRNA Name", "vial"))
                .thenReturn(updated);

        String json = """
                {
                    "name": "Updated mRNA Name",
                    "doseUnit": "vial"
                }
                """;

        mockMvc.perform(put("/api/v1/vaccine-types/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)))
                .andExpect(jsonPath("$.name", is("Updated mRNA Name")))
                .andExpect(jsonPath("$.doseUnit", is("vial")));
    }

    @Test
    @DisplayName("PUT /api/v1/vaccine-types/{id} returns 404 when ID not found")
    void updateVaccineType_notFound() throws Exception {
        when(vaccineTypeService.updateVaccineType(999L, "Some Name", "dose"))
                .thenThrow(new ResourceNotFoundException("VaccineType not found with ID: 999"));

        String json = """
                {
                    "name": "Some Name",
                    "doseUnit": "dose"
                }
                """;

        mockMvc.perform(put("/api/v1/vaccine-types/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/vaccine-types/{id}/activate sets active=true")
    void activateVaccineType_success() throws Exception {
        VaccineType activated = new VaccineType("VAX-OLD-03", "Legacy Vaccine", "dose", true);
        ReflectionTestUtils.setField(activated, "id", 103L);

        when(vaccineTypeService.activateVaccineType(103L)).thenReturn(activated);

        mockMvc.perform(post("/api/v1/vaccine-types/103/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(103)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/vaccine-types/{id}/deactivate sets active=false")
    void deactivateVaccineType_success() throws Exception {
        VaccineType deactivated = new VaccineType("VAX-MRNA-01", "COVID-19 mRNA Vaccine", "dose", false);
        ReflectionTestUtils.setField(deactivated, "id", 101L);

        when(vaccineTypeService.deactivateVaccineType(101L)).thenReturn(deactivated);

        mockMvc.perform(post("/api/v1/vaccine-types/101/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)))
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void deleteUnused_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/vaccine-types/101"))
                .andExpect(status().isNoContent());
        verify(vaccineTypeService).deleteVaccineType(101L);
    }

    @Test
    void deleteMissing_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Missing entry"))
                .when(vaccineTypeService).deleteVaccineType(999L);
        mockMvc.perform(delete("/api/v1/vaccine-types/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUsed_returnsConflict() throws Exception {
        doThrow(new IllegalStateException("Entry is used. Deactivate it instead."))
                .when(vaccineTypeService).deleteVaccineType(101L);
        mockMvc.perform(delete("/api/v1/vaccine-types/101"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Entry is used. Deactivate it instead.")));
    }
}
