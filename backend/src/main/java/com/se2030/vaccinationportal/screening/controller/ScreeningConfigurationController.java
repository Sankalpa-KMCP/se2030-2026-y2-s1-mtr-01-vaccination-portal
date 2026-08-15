package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.dto.AddAnswerOptionRequest;
import com.se2030.vaccinationportal.screening.dto.CreateQuestionRequest;
import com.se2030.vaccinationportal.screening.dto.CreateQuestionnaireRequest;
import com.se2030.vaccinationportal.screening.dto.UpdateQuestionRequest;
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

    public ScreeningConfigurationController(ScreeningConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    // ==========================================
    // Question Endpoints
    // ==========================================

    @GetMapping("/questions")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER"})
    public ResponseEntity<List<ScreeningQuestion>> listQuestions(
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly
    ) {
        List<ScreeningQuestion> questions = activeOnly
                ? configurationService.getActiveQuestions()
                : configurationService.getAllQuestions();
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/questions/{id}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER"})
    public ResponseEntity<ScreeningQuestion> getQuestionById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(configurationService.getQuestionById(id));
    }

    @PostMapping("/questions")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningQuestion> createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        ScreeningQuestion created = configurationService.createQuestion(
                request.code(),
                request.text(),
                request.description(),
                request.questionType(),
                request.defaultRequired()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/questions/{id}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningQuestion> updateQuestion(
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
        return ResponseEntity.ok(updated);
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
    public ResponseEntity<List<AnswerOption>> getOptionsForQuestion(@PathVariable("id") Long id) {
        return ResponseEntity.ok(configurationService.getOptionsForQuestion(id));
    }

    @PostMapping("/questions/{id}/options")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<AnswerOption> addAnswerOption(
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
        return ResponseEntity.status(HttpStatus.CREATED).body(option);
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
    public ResponseEntity<List<Questionnaire>> listQuestionnaires() {
        return ResponseEntity.ok(configurationService.getAllQuestionnaires());
    }

    @GetMapping("/questionnaires/{id}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER"})
    public ResponseEntity<Questionnaire> getQuestionnaireById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(configurationService.getQuestionnaireById(id));
    }

    @PostMapping("/questionnaires")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<Questionnaire> createQuestionnaire(@Valid @RequestBody CreateQuestionnaireRequest request) {
        Questionnaire questionnaire = configurationService.createQuestionnaire(
                request.code(),
                request.name(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(questionnaire);
    }
}
