package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.dto.AddAnswerOptionRequest;
import com.se2030.vaccinationportal.screening.dto.CreateQuestionRequest;
import com.se2030.vaccinationportal.screening.dto.CreateQuestionnaireRequest;
import com.se2030.vaccinationportal.screening.dto.UpdateQuestionRequest;
import com.se2030.vaccinationportal.screening.dto.response.AnswerOptionResponse;
import com.se2030.vaccinationportal.screening.dto.response.QuestionnaireResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningQuestionResponse;
import com.se2030.vaccinationportal.screening.mapper.QuestionnaireMapper;
import com.se2030.vaccinationportal.screening.mapper.ScreeningQuestionMapper;
import com.se2030.vaccinationportal.screening.model.AnswerOption;
import com.se2030.vaccinationportal.screening.model.Questionnaire;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.service.ScreeningConfigurationService;
import com.se2030.vaccinationportal.screening.validation.RequireRoles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Admin Screening Configuration:
 * - Questions catalog CRUD
 * - Answer options management
 * - Root Questionnaire definition management
 */
@RestController
@RequestMapping("/api/v1/screening")
public class ScreeningConfigurationController {

    private final ScreeningConfigurationService configurationService;
    private final ScreeningQuestionMapper questionMapper;
    private final QuestionnaireMapper questionnaireMapper;

    public ScreeningConfigurationController(
            ScreeningConfigurationService configurationService,
            ScreeningQuestionMapper questionMapper,
            QuestionnaireMapper questionnaireMapper
    ) {
        this.configurationService = configurationService;
        this.questionMapper = questionMapper;
        this.questionnaireMapper = questionnaireMapper;
    }

    // ==========================================
    // Question Endpoints
    // ==========================================

    @GetMapping("/questions")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER"})
    public ResponseEntity<List<ScreeningQuestionResponse>> listQuestions(
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly
    ) {
        List<ScreeningQuestion> questions = activeOnly
                ? configurationService.getActiveQuestions()
                : configurationService.getAllQuestions();
        return ResponseEntity.ok(questionMapper.toQuestionResponses(questions));
    }

    @GetMapping("/questions/{id}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER"})
    public ResponseEntity<ScreeningQuestionResponse> getQuestionById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(questionMapper.toResponse(configurationService.getQuestionById(id)));
    }

    @PostMapping("/questions")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningQuestionResponse> createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        ScreeningQuestion created = configurationService.createQuestion(
                request.code(),
                request.text(),
                request.description(),
                request.questionType(),
                request.defaultRequired()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(questionMapper.toResponse(created));
    }

    @PutMapping("/questions/{id}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningQuestionResponse> updateQuestion(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateQuestionRequest request
    ) {
        ScreeningQuestion updated = configurationService.updateQuestion(
                id,
                request.text(),
                request.description(),
                request.defaultRequired(),
                request.active()
        );
        return ResponseEntity.ok(questionMapper.toResponse(updated));
    }

    @DeleteMapping("/questions/{id}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<Void> deleteQuestion(@PathVariable("id") Long id) {
        configurationService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Answer Option Endpoints
    // ==========================================

    @GetMapping("/questions/{id}/options")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER", "CITIZEN"})
    public ResponseEntity<List<AnswerOptionResponse>> getOptionsForQuestion(@PathVariable("id") Long id) {
        return ResponseEntity.ok(questionMapper.toOptionResponses(configurationService.getOptionsForQuestion(id)));
    }

    @PostMapping("/questions/{id}/options")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<AnswerOptionResponse> addAnswerOption(
            @PathVariable("id") Long id,
            @Valid @RequestBody AddAnswerOptionRequest request
    ) {
        AnswerOption option = configurationService.addAnswerOption(
                id,
                request.code(),
                request.label(),
                request.typedValue(),
                request.displayOrder()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(questionMapper.toResponse(option));
    }

    @DeleteMapping("/options/{optionId}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<Void> removeAnswerOption(@PathVariable("optionId") Long optionId) {
        configurationService.removeAnswerOption(optionId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Questionnaire Container Endpoints
    // ==========================================

    @GetMapping("/questionnaires")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER"})
    public ResponseEntity<List<QuestionnaireResponse>> listQuestionnaires() {
        return ResponseEntity.ok(questionnaireMapper.toResponses(configurationService.getAllQuestionnaires()));
    }

    @GetMapping("/questionnaires/{id}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER"})
    public ResponseEntity<QuestionnaireResponse> getQuestionnaireById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(questionnaireMapper.toResponse(configurationService.getQuestionnaireById(id)));
    }

    @PostMapping("/questionnaires")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<QuestionnaireResponse> createQuestionnaire(@Valid @RequestBody CreateQuestionnaireRequest request) {
        Questionnaire questionnaire = configurationService.createQuestionnaire(
                request.code(),
                request.name(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(questionnaireMapper.toResponse(questionnaire));
    }
}
