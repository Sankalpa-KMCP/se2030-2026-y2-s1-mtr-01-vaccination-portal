package com.se2030.vaccinationportal.campaign.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.service.TargetGroupService;
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
class TargetGroupControllerTests {

    private MockMvc mockMvc;

    @Mock
    private TargetGroupService targetGroupService;

    private TargetGroup sampleGroup1;
    private TargetGroup sampleGroup2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TargetGroupController(targetGroupService))
                .setControllerAdvice(new GlobalCampaignExceptionHandler())
                .build();

        sampleGroup1 = new TargetGroup("TG-ELDERLY-65", "Senior citizens aged 65 and above", true);
        ReflectionTestUtils.setField(sampleGroup1, "id", 201L);

        sampleGroup2 = new TargetGroup("TG-HCW", "Frontline healthcare workers", true);
        ReflectionTestUtils.setField(sampleGroup2, "id", 202L);
    }

    @Test
    @DisplayName("GET /api/v1/target-groups returns list of active target groups")
    void getActiveTargetGroups_returnsList() throws Exception {
        when(targetGroupService.getActiveTargetGroups()).thenReturn(List.of(sampleGroup1, sampleGroup2));

        mockMvc.perform(get("/api/v1/target-groups").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(201)))
                .andExpect(jsonPath("$[0].code", is("TG-ELDERLY-65")))
                .andExpect(jsonPath("$[0].description", is("Senior citizens aged 65 and above")))
                .andExpect(jsonPath("$[0].active", is(true)))
                .andExpect(jsonPath("$[1].id", is(202)))
                .andExpect(jsonPath("$[1].code", is("TG-HCW")))
                .andExpect(jsonPath("$[1].description", is("Frontline healthcare workers")))
                .andExpect(jsonPath("$[1].active", is(true)));

        verify(targetGroupService).getActiveTargetGroups();
    }

    @Test
    @DisplayName("GET /api/v1/target-groups returns empty array when no active target groups exist")
    void getActiveTargetGroups_returnsEmptyList() throws Exception {
        when(targetGroupService.getActiveTargetGroups()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/target-groups").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(targetGroupService).getActiveTargetGroups();
    }
}
