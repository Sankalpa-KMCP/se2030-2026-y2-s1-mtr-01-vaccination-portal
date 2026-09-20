package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.dto.AddEligibilityRuleRequest;
import com.se2030.vaccinationportal.screening.dto.AddQuestionItemRequest;
import com.se2030.vaccinationportal.screening.dto.ReorderQuestionItemsRequest;
import com.se2030.vaccinationportal.screening.dto.UpdateEligibilityRuleRequest;
import com.se2030.vaccinationportal.screening.dto.UpdateQuestionItemRequest;
import com.se2030.vaccinationportal.screening.dto.response.EligibilityRuleResponse;
import com.se2030.vaccinationportal.screening.dto.response.PublishedQuestionnaireVersionResponse;
import com.se2030.vaccinationportal.screening.dto.response.QuestionnaireItemResponse;
import com.se2030.vaccinationportal.screening.dto.response.QuestionnaireVersionResponse;
import com.se2030.vaccinationportal.screening.mapper.QuestionnaireVersionMapper;
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
    private final QuestionnaireVersionMapper versionMapper;

    public QuestionnaireController(
            QuestionnaireVersionService versionService,
            QuestionnaireVersionMapper versionMapper
    ) {
        this.versionService = versionService;
        this.versionMapper = versionMapper;
    }

    @GetMapping("/questionnaires/{questionnaireId}/versions")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER"})
    public ResponseEntity<List<QuestionnaireVersionResponse>> getVersionsForQuestionnaire(
            @PathVariable("questionnaireId") Long questionnaireId
    ) {
        return ResponseEntity.ok(versionMapper.toResponses(versionService.getVersionsForQuestionnaire(questionnaireId)));
    }

    @GetMapping("/questionnaire-versions/published")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER", "CAMPAIGN_OFFICER"})
    public ResponseEntity<List<PublishedQuestionnaireVersionResponse>> getPublishedVersions() {
        List<PublishedQuestionnaireVersionResponse> responses = versionService.getPublishedVersions().stream()
                .map(v -> new PublishedQuestionnaireVersionResponse(
                        v.getId(),
                        v.getQuestionnaire().getId(),
                        v.getQuestionnaire().getCode(),
                        v.getQuestionnaire().getName(),
                        v.getVersionNumber(),
                        v.getStatus(),
                        v.getPublishedAt()
                ))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/questionnaire-versions/{versionId}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER", "CITIZEN", "CAMPAIGN_OFFICER"})
    public ResponseEntity<QuestionnaireVersionResponse> getVersionById(@PathVariable("versionId") Long versionId) {
        return ResponseEntity.ok(versionMapper.toResponse(versionService.getVersionById(versionId)));
    }

    @PostMapping("/questionnaires/{questionnaireId}/versions")
    @RequireRoles({"HEALTHCARE_OFFICER"})
    public ResponseEntity<QuestionnaireVersionResponse> createDraftVersion(
            @PathVariable("questionnaireId") Long questionnaireId
    ) {
        QuestionnaireVersion draft = versionService.createDraftVersion(questionnaireId);
        return ResponseEntity.status(HttpStatus.CREATED).body(versionMapper.toResponse(draft));
    }

    @PostMapping("/questionnaire-versions/{versionId}/items")
    @RequireRoles({"HEALTHCARE_OFFICER"})
    public ResponseEntity<QuestionnaireItemResponse> addQuestionItem(
            @PathVariable("versionId") Long versionId,
            @Valid @RequestBody AddQuestionItemRequest request
    ) {
        QuestionnaireItem item = versionService.addQuestionItem(
                versionId,
                request.questionId(),
                request.displayOrder(),
                request.requiredOverride()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(versionMapper.toResponse(item));
    }

    @DeleteMapping("/questionnaire-versions/{versionId}/items/{itemId}")
    @RequireRoles({"HEALTHCARE_OFFICER"})
    public ResponseEntity<Void> removeQuestionItem(
            @PathVariable("versionId") Long versionId,
            @PathVariable("itemId") Long itemId
    ) {
        versionService.removeQuestionItem(versionId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/questionnaire-versions/{versionId}/items/{itemId}")
    @RequireRoles({"HEALTHCARE_OFFICER"})
    public ResponseEntity<QuestionnaireItemResponse> replaceQuestionItem(
            @PathVariable("versionId") Long versionId,
            @PathVariable("itemId") Long itemId,
            @Valid @RequestBody UpdateQuestionItemRequest request
    ) {
        QuestionnaireItem item = versionService.replaceQuestionItem(
                versionId,
                itemId,
                request.questionId(),
                request.requiredOverride()
        );
        return ResponseEntity.ok(versionMapper.toResponse(item));
    }

    @PutMapping("/questionnaire-versions/{versionId}/items/reorder")
    @RequireRoles({"HEALTHCARE_OFFICER"})
    public ResponseEntity<List<QuestionnaireItemResponse>> reorderQuestionItems(
            @PathVariable("versionId") Long versionId,
            @Valid @RequestBody ReorderQuestionItemsRequest request
    ) {
        List<QuestionnaireItem> items = versionService.reorderQuestionItems(versionId, request.itemIds());
        return ResponseEntity.ok(versionMapper.toItemResponses(items));
    }

    @PostMapping("/questionnaire-versions/{versionId}/rules")
    @RequireRoles({"HEALTHCARE_OFFICER"})
    public ResponseEntity<EligibilityRuleResponse> addEligibilityRule(
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
        return ResponseEntity.status(HttpStatus.CREATED).body(versionMapper.toResponse(rule));
    }

    @GetMapping("/questionnaire-versions/{versionId}/rules")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER", "CITIZEN"})
    public ResponseEntity<List<EligibilityRuleResponse>> getEligibilityRulesForVersion(
            @PathVariable("versionId") Long versionId
    ) {
        return ResponseEntity.ok(versionMapper.toRuleResponses(versionService.getEligibilityRulesForVersion(versionId)));
    }

    @GetMapping("/rules/{ruleId}")
    @RequireRoles({"ADMIN", "SYSTEM_ADMINISTRATOR", "HEALTHCARE_OFFICER", "CITIZEN"})
    public ResponseEntity<EligibilityRuleResponse> getEligibilityRuleById(@PathVariable("ruleId") Long ruleId) {
        return ResponseEntity.ok(versionMapper.toResponse(versionService.getEligibilityRuleById(ruleId)));
    }

    @PutMapping("/rules/{ruleId}")
    @RequireRoles({"HEALTHCARE_OFFICER"})
    public ResponseEntity<EligibilityRuleResponse> updateEligibilityRule(
            @PathVariable("ruleId") Long ruleId,
            @Valid @RequestBody UpdateEligibilityRuleRequest request
    ) {
        EligibilityRule rule = versionService.updateEligibilityRule(
                ruleId,
                request.ruleCode(),
                request.ruleName(),
                request.conditionExpression(),
                request.preliminaryResult(),
                request.priority(),
                request.message()
        );
        return ResponseEntity.ok(versionMapper.toResponse(rule));
    }

    @DeleteMapping("/rules/{ruleId}")
    @RequireRoles({"HEALTHCARE_OFFICER"})
    public ResponseEntity<Void> deleteEligibilityRule(@PathVariable("ruleId") Long ruleId) {
        versionService.deleteEligibilityRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/questionnaire-versions/{versionId}/publish")
    @RequireRoles({"HEALTHCARE_OFFICER"})
    public ResponseEntity<QuestionnaireVersionResponse> publishVersion(@PathVariable("versionId") Long versionId) {
        QuestionnaireVersion published = versionService.publishVersion(versionId);
        return ResponseEntity.ok(versionMapper.toResponse(published));
    }

    @PostMapping("/questionnaire-versions/{versionId}/retire")
    @RequireRoles({"HEALTHCARE_OFFICER"})
    public ResponseEntity<QuestionnaireVersionResponse> retireVersion(@PathVariable("versionId") Long versionId) {
        QuestionnaireVersion retired = versionService.retireVersion(versionId);
        return ResponseEntity.ok(versionMapper.toResponse(retired));
    }

    @DeleteMapping("/questionnaire-versions/{versionId}")
    @RequireRoles({"HEALTHCARE_OFFICER"})
    public ResponseEntity<Void> deleteDraftVersion(@PathVariable("versionId") Long versionId) {
        versionService.deleteDraftVersion(versionId);
        return ResponseEntity.noContent().build();
    }
}
