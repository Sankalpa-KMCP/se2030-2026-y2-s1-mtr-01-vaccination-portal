package com.se2030.vaccinationportal.campaign.reference.web;

import com.se2030.vaccinationportal.campaign.web.GlobalCampaignExceptionHandler;


import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.service.TargetGroupService;
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
    private TargetGroup sampleGroupInactive;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TargetGroupController(targetGroupService))
                .setControllerAdvice(new GlobalCampaignExceptionHandler())
                .build();

        sampleGroup1 = new TargetGroup("TG-ELDERLY-65", "Senior citizens aged 65 and above", true);
        ReflectionTestUtils.setField(sampleGroup1, "id", 201L);

        sampleGroup2 = new TargetGroup("TG-HCW", "Frontline healthcare workers", true);
        ReflectionTestUtils.setField(sampleGroup2, "id", 202L);

        sampleGroupInactive = new TargetGroup("TG-LEGACY", "Legacy cohort", false);
        ReflectionTestUtils.setField(sampleGroupInactive, "id", 203L);
    }

    @Test
    @DisplayName("GET /api/v1/target-groups returns list of active target groups by default")
    void getActiveTargetGroups_returnsList() throws Exception {
        when(targetGroupService.getTargetGroups(false)).thenReturn(List.of(sampleGroup1, sampleGroup2));

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

        verify(targetGroupService).getTargetGroups(false);
    }

    @Test
    @DisplayName("GET /api/v1/target-groups?includeInactive=true returns all target groups")
    void getAllTargetGroups_withIncludeInactive() throws Exception {
        when(targetGroupService.getTargetGroups(true))
                .thenReturn(List.of(sampleGroup1, sampleGroup2, sampleGroupInactive));

        mockMvc.perform(get("/api/v1/target-groups?includeInactive=true").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].id", is(203)))
                .andExpect(jsonPath("$[2].code", is("TG-LEGACY")))
                .andExpect(jsonPath("$[2].active", is(false)));

        verify(targetGroupService).getTargetGroups(true);
    }

    @Test
    @DisplayName("GET /api/v1/target-groups returns empty array when no active target groups exist")
    void getActiveTargetGroups_returnsEmptyList() throws Exception {
        when(targetGroupService.getTargetGroups(false)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/target-groups").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(targetGroupService).getTargetGroups(false);
    }

    @Test
    @DisplayName("POST /api/v1/target-groups creates new target group")
    void createTargetGroup_success() throws Exception {
        TargetGroup created = new TargetGroup("TG-CHILD-05", "Children aged 5 to 11", true);
        ReflectionTestUtils.setField(created, "id", 204L);

        when(targetGroupService.createTargetGroup("TG-CHILD-05", "Children aged 5 to 11"))
                .thenReturn(created);

        String json = """
                {
                    "code": "TG-CHILD-05",
                    "description": "Children aged 5 to 11"
                }
                """;

        mockMvc.perform(post("/api/v1/target-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(204)))
                .andExpect(jsonPath("$.code", is("TG-CHILD-05")))
                .andExpect(jsonPath("$.description", is("Children aged 5 to 11")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/target-groups fails on blank code/description")
    void createTargetGroup_validationFailure() throws Exception {
        String json = """
                {
                    "code": "",
                    "description": ""
                }
                """;

        mockMvc.perform(post("/api/v1/target-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/target-groups returns 400 when duplicate code exists")
    void createTargetGroup_duplicateCode() throws Exception {
        when(targetGroupService.createTargetGroup(eq("TG-HCW"), any()))
                .thenThrow(new IllegalArgumentException("Target group already exists with code: TG-HCW"));

        String json = """
                {
                    "code": "TG-HCW",
                    "description": "Duplicate HCW"
                }
                """;

        mockMvc.perform(post("/api/v1/target-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Target group already exists with code: TG-HCW")));
    }

    @Test
    @DisplayName("PUT /api/v1/target-groups/{id} updates existing target group")
    void updateTargetGroup_success() throws Exception {
        TargetGroup updated = new TargetGroup("TG-ELDERLY-65", "Updated Senior description", true);
        ReflectionTestUtils.setField(updated, "id", 201L);

        when(targetGroupService.updateTargetGroup(201L, "Updated Senior description"))
                .thenReturn(updated);

        String json = """
                {
                    "description": "Updated Senior description"
                }
                """;

        mockMvc.perform(put("/api/v1/target-groups/201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(201)))
                .andExpect(jsonPath("$.description", is("Updated Senior description")));
    }

    @Test
    @DisplayName("PUT /api/v1/target-groups/{id} returns 404 when ID not found")
    void updateTargetGroup_notFound() throws Exception {
        when(targetGroupService.updateTargetGroup(999L, "Nonexistent description"))
                .thenThrow(new ResourceNotFoundException("TargetGroup not found with ID: 999"));

        String json = """
                {
                    "description": "Nonexistent description"
                }
                """;

        mockMvc.perform(put("/api/v1/target-groups/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/target-groups/{id}/activate sets active=true")
    void activateTargetGroup_success() throws Exception {
        TargetGroup activated = new TargetGroup("TG-LEGACY", "Legacy cohort", true);
        ReflectionTestUtils.setField(activated, "id", 203L);

        when(targetGroupService.activateTargetGroup(203L)).thenReturn(activated);

        mockMvc.perform(post("/api/v1/target-groups/203/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(203)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/target-groups/{id}/deactivate sets active=false")
    void deactivateTargetGroup_success() throws Exception {
        TargetGroup deactivated = new TargetGroup("TG-ELDERLY-65", "Senior citizens", false);
        ReflectionTestUtils.setField(deactivated, "id", 201L);

        when(targetGroupService.deactivateTargetGroup(201L)).thenReturn(deactivated);

        mockMvc.perform(post("/api/v1/target-groups/201/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(201)))
                .andExpect(jsonPath("$.active", is(false)));
    }
}
