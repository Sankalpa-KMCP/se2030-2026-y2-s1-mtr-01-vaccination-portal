package com.se2030.vaccinationportal.campaign.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.service.VaccineTypeService;
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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VaccineTypeController(vaccineTypeService))
                .setControllerAdvice(new GlobalCampaignExceptionHandler())
                .build();

        sampleVaccine1 = new VaccineType("VAX-MRNA-01", "COVID-19 mRNA Vaccine", "dose", true);
        ReflectionTestUtils.setField(sampleVaccine1, "id", 101L);

        sampleVaccine2 = new VaccineType("VAX-INACT-02", "Inactivated Viral Vaccine", "dose", true);
        ReflectionTestUtils.setField(sampleVaccine2, "id", 102L);
    }

    @Test
    @DisplayName("GET /api/v1/vaccine-types returns list of active vaccine types")
    void getActiveVaccineTypes_returnsList() throws Exception {
        when(vaccineTypeService.getActiveVaccineTypes()).thenReturn(List.of(sampleVaccine1, sampleVaccine2));

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

        verify(vaccineTypeService).getActiveVaccineTypes();
    }

    @Test
    @DisplayName("GET /api/v1/vaccine-types returns empty array when no active vaccine types exist")
    void getActiveVaccineTypes_returnsEmptyList() throws Exception {
        when(vaccineTypeService.getActiveVaccineTypes()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/vaccine-types").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(vaccineTypeService).getActiveVaccineTypes();
    }
}
