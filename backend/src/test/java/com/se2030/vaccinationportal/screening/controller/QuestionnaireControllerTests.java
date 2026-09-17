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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
