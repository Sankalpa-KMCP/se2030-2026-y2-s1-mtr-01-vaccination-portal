package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.campaign.service.QuestionnaireVersionPublicationPort;
import com.se2030.vaccinationportal.screening.model.*;
import com.se2030.vaccinationportal.screening.repository.EligibilityRuleRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service managing QuestionnaireVersion lifecycle (DRAFT -> PUBLISHED -> RETIRED).
 * Implements QuestionnaireVersionPublicationPort for cross-module integration with MF-02.
 * Enforces immutability: published versions cannot be mutated or deleted.
 */
@Service
@Transactional
public class QuestionnaireVersionService implements QuestionnaireVersionPublicationPort {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireVersionRepository versionRepository;
    private final QuestionnaireItemRepository itemRepository;
    private final EligibilityRuleRepository ruleRepository;
    private final ScreeningQuestionRepository questionRepository;
    private final QuestionnaireMutabilityValidator mutabilityValidator;

    public QuestionnaireVersionService(
            QuestionnaireRepository questionnaireRepository,
            QuestionnaireVersionRepository versionRepository,
            QuestionnaireItemRepository itemRepository,
            EligibilityRuleRepository ruleRepository,
            ScreeningQuestionRepository questionRepository,
            QuestionnaireMutabilityValidator mutabilityValidator
    ) {
        this.questionnaireRepository = questionnaireRepository;
        this.versionRepository = versionRepository;
        this.itemRepository = itemRepository;
        this.ruleRepository = ruleRepository;
        this.questionRepository = questionRepository;
        this.mutabilityValidator = mutabilityValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPublished(Long questionnaireVersionId) {
        if (questionnaireVersionId == null) {
            return false;
        }
        return versionRepository.findByIdAndStatus(questionnaireVersionId, QuestionnaireVersionStatus.PUBLISHED).isPresent();
    }

    @Transactional(readOnly = true)
    public QuestionnaireVersion getVersionById(Long versionId) {
        if (versionId == null) {
            throw new IllegalArgumentException("Version ID cannot be null");
        }
        QuestionnaireVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Questionnaire version not found with ID: " + versionId));
        return initializeResponseRelations(version);
    }

    @Transactional(readOnly = true)
    public List<QuestionnaireVersion> getVersionsForQuestionnaire(Long questionnaireId) {
        List<QuestionnaireVersion> versions = versionRepository.findByQuestionnaireIdOrderByVersionNumberDesc(questionnaireId);
        versions.forEach(this::initializeResponseRelations);
        return versions;
    }

    public QuestionnaireVersion createDraftVersion(Long questionnaireId) {
        Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId)
                .orElseThrow(() -> new IllegalArgumentException("Questionnaire not found with ID: " + questionnaireId));

        int nextVersionNumber = versionRepository.findFirstByQuestionnaireIdOrderByVersionNumberDesc(questionnaireId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        QuestionnaireVersion draft = new QuestionnaireVersion(questionnaire, nextVersionNumber, QuestionnaireVersionStatus.DRAFT);
        return versionRepository.save(draft);
    }

    public QuestionnaireItem addQuestionItem(Long versionId, Long questionId, int displayOrder, Boolean requiredOverride) {
        QuestionnaireVersion version = getVersionById(versionId);
        mutabilityValidator.assertVersionIsDraft(version);

        ScreeningQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + questionId));

        if (!question.isActive()) {
            throw new IllegalStateException("Cannot add inactive question to a questionnaire version");
        }

        if (itemRepository.existsByQuestionnaireVersionIdAndQuestionId(versionId, questionId)) {
            throw new IllegalStateException("Question already added to this questionnaire version");
        }

        if (itemRepository.existsByQuestionnaireVersionIdAndDisplayOrder(versionId, displayOrder)) {
            throw new IllegalStateException("Display order " + displayOrder + " already in use in this version");
        }

        QuestionnaireItem item = new QuestionnaireItem(version, question, displayOrder, requiredOverride);
        return itemRepository.save(item);
    }

    public EligibilityRule addEligibilityRule(Long versionId, String ruleCode, String ruleName, String conditionExpression,
                                             PreliminaryScreeningResult preliminaryResult, int priority, String message) {
        QuestionnaireVersion version = getVersionById(versionId);
        mutabilityValidator.assertVersionIsDraft(version);

        if (ruleCode == null || ruleCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule code is required");
        }
        if (conditionExpression == null || conditionExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Condition expression is required");
        }
        if (ruleRepository.existsByQuestionnaireVersionIdAndRuleCode(versionId, ruleCode.trim())) {
            throw new IllegalStateException("Rule code '" + ruleCode.trim() + "' already exists in this version");
        }

        EligibilityRule rule = new EligibilityRule(
                ruleCode.trim(),
                version,
                ruleName != null ? ruleName.trim() : ruleCode.trim(),
                conditionExpression.trim(),
                preliminaryResult != null ? preliminaryResult : PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                priority,
                message != null ? message.trim() : "Rule triggered preliminary review"
        );
        return ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<EligibilityRule> getEligibilityRulesForVersion(Long versionId) {
        if (versionId == null) {
            throw new IllegalArgumentException("Version ID cannot be null");
        }
        if (!versionRepository.existsById(versionId)) {
            throw new IllegalArgumentException("Questionnaire version not found with ID: " + versionId);
        }
        return ruleRepository.findByQuestionnaireVersionIdOrderByPriorityAsc(versionId);
    }

    @Transactional(readOnly = true)
    public EligibilityRule getEligibilityRuleById(Long ruleId) {
        if (ruleId == null) {
            throw new IllegalArgumentException("Eligibility rule ID cannot be null");
        }
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Eligibility rule not found with ID: " + ruleId));
    }

    public EligibilityRule updateEligibilityRule(
            Long ruleId,
            String ruleCode,
            String ruleName,
            String conditionExpression,
            PreliminaryScreeningResult preliminaryResult,
            int priority,
            String message
    ) {
        EligibilityRule rule = getEligibilityRuleById(ruleId);
        mutabilityValidator.assertVersionIsDraft(rule.getQuestionnaireVersion());
        if (ruleCode == null || ruleCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule code is required");
        }
        if (ruleName == null || ruleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name is required");
        }
        if (conditionExpression == null || conditionExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Condition expression is required");
        }
        if (preliminaryResult == null) {
            throw new IllegalArgumentException("Preliminary result is required");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule message is required");
        }

        String normalizedCode = ruleCode.trim();
        Long versionId = rule.getQuestionnaireVersion().getId();
        if (!rule.getRuleCode().equals(normalizedCode)
                && ruleRepository.existsByQuestionnaireVersionIdAndRuleCode(versionId, normalizedCode)) {
            throw new IllegalStateException("Rule code '" + normalizedCode + "' already exists in this version");
        }

        rule.setRuleCode(normalizedCode);
        rule.setRuleName(ruleName.trim());
        rule.setConditionExpression(conditionExpression.trim());
        rule.setPreliminaryResult(preliminaryResult);
        rule.setPriority(priority);
        rule.setMessage(message.trim());
        return ruleRepository.save(rule);
    }

    public void deleteEligibilityRule(Long ruleId) {
        EligibilityRule rule = getEligibilityRuleById(ruleId);
        mutabilityValidator.assertVersionIsDraft(rule.getQuestionnaireVersion());
        ruleRepository.delete(rule);
    }

    public QuestionnaireVersion publishVersion(Long versionId) {
        QuestionnaireVersion version = getVersionById(versionId);

        if (version.getStatus() != QuestionnaireVersionStatus.DRAFT) {
            throw new IllegalStateException("Cannot publish version in " + version.getStatus() + " status");
        }

        List<QuestionnaireItem> items = itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(versionId);
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot publish questionnaire version: version must contain at least one question item");
        }

        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
        version.setPublishedAt(Instant.now());
        return versionRepository.save(version);
    }

    public QuestionnaireVersion retireVersion(Long versionId) {
        QuestionnaireVersion version = getVersionById(versionId);

        if (version.getStatus() != QuestionnaireVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED versions can be retired");
        }

        version.setStatus(QuestionnaireVersionStatus.RETIRED);
        version.setRetiredAt(Instant.now());
        return versionRepository.save(version);
    }

    public void deleteDraftVersion(Long versionId) {
        QuestionnaireVersion version = getVersionById(versionId);
        mutabilityValidator.assertVersionIsDraft(version);
        if (versionRepository.hasSubmissions(versionId)) {
            throw new IllegalStateException("Cannot delete version with associated screening submissions");
        }

        versionRepository.delete(version);
    }

    private QuestionnaireVersion initializeResponseRelations(QuestionnaireVersion version) {
        version.getItems().size();
        version.getRules().size();
        return version;
    }
}
