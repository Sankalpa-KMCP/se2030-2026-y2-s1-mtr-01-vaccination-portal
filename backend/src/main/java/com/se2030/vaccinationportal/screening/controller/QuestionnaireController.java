package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.dto.AddEligibilityRuleRequest;
import com.se2030.vaccinationportal.screening.dto.AddQuestionItemRequest;
import com.se2030.vaccinationportal.screening.model.EligibilityRule;
import com.se2030.vaccinationportal.screening.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.service.QuestionnaireVersionService;
import com.se2030.vaccinationportal.screening.validation.RequireRoles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Questionnaire Versioning & Rule Management:
 * - Version lifecycle management (DRAFT -> PUBLISHED -> RETIRED)
 * - Questionnaire item ordering
 * - Rule snapshot definitions
 */
@RestController
@RequestMapping("/api/v1/screening")
public class QuestionnaireController {

    private final QuestionnaireVersionService versionService;

    public QuestionnaireController(QuestionnaireVersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping("/questionnaires/{questionnaireId}/versions")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER"})
    public ResponseEntity<List<QuestionnaireVersion>> getVersionsForQuestionnaire(
            @PathVariable("questionnaireId") Long questionnaireId
    ) {
        return ResponseEntity.ok(versionService.getVersionsForQuestionnaire(questionnaireId));
    }

    @GetMapping("/questionnaire-versions/{versionId}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER", "CITIZEN"})
    public ResponseEntity<QuestionnaireVersion> getVersionById(@PathVariable("versionId") Long versionId) {
        return ResponseEntity.ok(versionService.getVersionById(versionId));
    }

    @PostMapping("/questionnaires/{questionnaireId}/versions")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<QuestionnaireVersion> createDraftVersion(
            @PathVariable("questionnaireId") Long questionnaireId
    ) {
        QuestionnaireVersion draft = versionService.createDraftVersion(questionnaireId);
        return ResponseEntity.status(HttpStatus.CREATED).body(draft);
    }

    @PostMapping("/questionnaire-versions/{versionId}/items")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<QuestionnaireItem> addQuestionItem(
            @PathVariable("versionId") Long versionId,
            @Valid @RequestBody AddQuestionItemRequest request
    ) {
        QuestionnaireItem item = versionService.addQuestionItem(
                versionId,
                request.questionId(),
                request.displayOrder(),
                request.requiredOverride()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PostMapping("/questionnaire-versions/{versionId}/rules")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<EligibilityRule> addEligibilityRule(
            @PathVariable("versionId") Long versionId,
            @Valid @RequestBody AddEligibilityRuleRequest request
    ) {
        EligibilityRule rule = versionService.addEligibilityRule(
                versionId,
                request.ruleCode(),
                request.ruleName(),
                request.conditionExpression(),
                request.preliminaryResult(),
                request.priority(),
                request.message()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @PostMapping("/questionnaire-versions/{versionId}/publish")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<QuestionnaireVersion> publishVersion(@PathVariable("versionId") Long versionId) {
        QuestionnaireVersion published = versionService.publishVersion(versionId);
        return ResponseEntity.ok(published);
    }

    @PostMapping("/questionnaire-versions/{versionId}/retire")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<QuestionnaireVersion> retireVersion(@PathVariable("versionId") Long versionId) {
        QuestionnaireVersion retired = versionService.retireVersion(versionId);
        return ResponseEntity.ok(retired);
    }

    @DeleteMapping("/questionnaire-versions/{versionId}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<Void> deleteDraftVersion(@PathVariable("versionId") Long versionId) {
        versionService.deleteDraftVersion(versionId);
        return ResponseEntity.noContent().build();
    }
}
