package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.mapper.QuestionnaireVersionMapper;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.Questionnaire;
import com.se2030.vaccinationportal.screening.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.service.QuestionnaireVersionService;
import com.se2030.vaccinationportal.screening.validation.RoleAuthorizationInterceptor;
import com.se2030.vaccinationportal.screening.validation.ScreeningExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionnaireControllerTests {

    @Mock
    private QuestionnaireVersionService versionService;

    private MockMvc mockMvc;

    private QuestionnaireVersion version;
    private ScreeningQuestion question;

    @BeforeEach
    void setUp() {
        QuestionnaireController controller = new QuestionnaireController(
                versionService,
                new QuestionnaireVersionMapper()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ScreeningExceptionHandler())
                .addInterceptors(new RoleAuthorizationInterceptor())
                .build();
        authenticateAs("HEALTHCARE_OFFICER");

        Questionnaire questionnaire = new Questionnaire("PRE_VAX", "Pre-vaccination", null);
        questionnaire.setId(10L);
        version = new QuestionnaireVersion(questionnaire, 1, QuestionnaireVersionStatus.DRAFT);
        version.setId(20L);
        question = new ScreeningQuestion("FEVER", "Do you have fever?", QuestionType.BOOLEAN, true, true);
        question.setId(30L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addQuestionItemAcceptsDisplayOrderOne() throws Exception {
        QuestionnaireItem item = new QuestionnaireItem(version, question, 1, null);
        item.setId(100L);
        when(versionService.addQuestionItem(20L, 30L, 1, null)).thenReturn(item);

        mockMvc.perform(post("/api/v1/screening/questionnaire-versions/{versionId}/items", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": 30,
                                  "displayOrder": 1,
                                  "requiredOverride": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.displayOrder").value(1));

        verify(versionService).addQuestionItem(20L, 30L, 1, null);
    }

    @Test
    void addQuestionItemAcceptsDisplayOrderGreaterThanOne() throws Exception {
        QuestionnaireItem item = new QuestionnaireItem(version, question, 3, true);
        item.setId(101L);
        when(versionService.addQuestionItem(20L, 30L, 3, true)).thenReturn(item);

        mockMvc.perform(post("/api/v1/screening/questionnaire-versions/{versionId}/items", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": 30,
                                  "displayOrder": 3,
                                  "requiredOverride": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.displayOrder").value(3));

        verify(versionService).addQuestionItem(20L, 30L, 3, true);
    }

    @Test
    void addQuestionItemRejectsZeroDisplayOrder() throws Exception {
        mockMvc.perform(post("/api/v1/screening/questionnaire-versions/{versionId}/items", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": 30,
                                  "displayOrder": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.displayOrder[0]").value("Display order must be at least 1"));

        verify(versionService, never()).addQuestionItem(anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void addQuestionItemRejectsNegativeDisplayOrder() throws Exception {
        mockMvc.perform(post("/api/v1/screening/questionnaire-versions/{versionId}/items", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": 30,
                                  "displayOrder": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.displayOrder[0]").value("Display order must be at least 1"));

        verify(versionService, never()).addQuestionItem(anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void addQuestionItemRejectsNullOrMissingDisplayOrder() throws Exception {
        mockMvc.perform(post("/api/v1/screening/questionnaire-versions/{versionId}/items", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": 30
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.displayOrder[0]").value("Display order is required"));

        verify(versionService, never()).addQuestionItem(anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void addQuestionItemRejectsNonIntegerDisplayOrder() throws Exception {
        mockMvc.perform(post("/api/v1/screening/questionnaire-versions/{versionId}/items", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": 30,
                                  "displayOrder": 1.5
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(versionService, never()).addQuestionItem(anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void getPublishedVersionsResolvesDedicatedEndpoint() throws Exception {
        when(versionService.getPublishedVersions()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/screening/questionnaire-versions/published"))
                .andExpect(status().isOk());

        verify(versionService).getPublishedVersions();
        verify(versionService, never()).getVersionById(anyLong());
    }

    @Test
    void getVersionByIdWithNumericIdResolvesCorrectly() throws Exception {
        when(versionService.getVersionById(20L)).thenReturn(version);

        mockMvc.perform(get("/api/v1/screening/questionnaire-versions/{versionId}", 20L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20L));

        verify(versionService).getVersionById(20L);
    }

    @Test
    void removeQuestionItemCallsServiceAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/screening/questionnaire-versions/{versionId}/items/{itemId}", 20L, 100L))
                .andExpect(status().isNoContent());

        verify(versionService).removeQuestionItem(20L, 100L);
    }

    @Test
    void replaceQuestionItemCallsServiceAndReturnsUpdatedItem() throws Exception {
        QuestionnaireItem item = new QuestionnaireItem(version, question, 1, false);
        item.setId(100L);
        when(versionService.replaceQuestionItem(20L, 100L, 30L, false)).thenReturn(item);

        mockMvc.perform(put("/api/v1/screening/questionnaire-versions/{versionId}/items/{itemId}", 20L, 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": 30,
                                  "requiredOverride": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.questionId").value(30L))
                .andExpect(jsonPath("$.requiredOverride").value(false));

        verify(versionService).replaceQuestionItem(20L, 100L, 30L, false);
    }

    @Test
    void reorderQuestionItemsCallsServiceAndReturnsUpdatedList() throws Exception {
        QuestionnaireItem item1 = new QuestionnaireItem(version, question, 1, null);
        item1.setId(101L);
        when(versionService.reorderQuestionItems(20L, List.of(101L))).thenReturn(List.of(item1));

        mockMvc.perform(put("/api/v1/screening/questionnaire-versions/{versionId}/items/reorder", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemIds": [101]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101L))
                .andExpect(jsonPath("$[0].displayOrder").value(1));

        verify(versionService).reorderQuestionItems(20L, List.of(101L));
    }

    @Test
    void getVersionByIdWithInvalidTextReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/screening/questionnaire-versions/{versionId}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'versionId'"));

        verify(versionService, never()).getVersionById(anyLong());
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
