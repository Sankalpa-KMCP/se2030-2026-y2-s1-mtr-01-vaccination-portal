package com.se2030.vaccinationportal.screening.questionnaire.service;

import com.se2030.vaccinationportal.screening.questionnaire.model.EligibilityRule;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;


import com.se2030.vaccinationportal.campaign.service.QuestionnaireVersionPublicationPort;
import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.questionnaire.repository.EligibilityRuleRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.ScreeningQuestionRepository;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningSubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final ScreeningSubmissionRepository submissionRepository;
    private final QuestionnaireMutabilityValidator mutabilityValidator;

    public QuestionnaireVersionService(
            QuestionnaireRepository questionnaireRepository,
            QuestionnaireVersionRepository versionRepository,
            QuestionnaireItemRepository itemRepository,
            EligibilityRuleRepository ruleRepository,
            ScreeningQuestionRepository questionRepository,
            QuestionnaireMutabilityValidator mutabilityValidator
    ) {
        this(questionnaireRepository, versionRepository, itemRepository, ruleRepository, questionRepository, null, mutabilityValidator);
    }

    @Autowired
    public QuestionnaireVersionService(
            QuestionnaireRepository questionnaireRepository,
            QuestionnaireVersionRepository versionRepository,
            QuestionnaireItemRepository itemRepository,
            EligibilityRuleRepository ruleRepository,
            ScreeningQuestionRepository questionRepository,
            ScreeningSubmissionRepository submissionRepository,
            QuestionnaireMutabilityValidator mutabilityValidator
    ) {
        this.questionnaireRepository = questionnaireRepository;
        this.versionRepository = versionRepository;
        this.itemRepository = itemRepository;
        this.ruleRepository = ruleRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
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

    @Override
    @Transactional(readOnly = true)
    public boolean hasCampaignSubmissions(Long campaignId) {
        if (campaignId == null || submissionRepository == null) {
            return false;
        }
        return submissionRepository.existsByCampaignId(campaignId);
    }

    @Transactional(readOnly = true)
    public List<QuestionnaireVersion> getPublishedVersions() {
        List<QuestionnaireVersion> versions = versionRepository.findByStatus(QuestionnaireVersionStatus.PUBLISHED);
        versions.forEach(this::initializeResponseRelations);
        return versions;
    }

    @Transactional(readOnly = true)
    public QuestionnaireVersion getVersionById(Long versionId) {
        if (versionId == null) {
            throw new IllegalArgumentException("Version ID cannot be null");
        }
        QuestionnaireVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire version not found with ID: " + versionId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire not found with ID: " + questionnaireId));

        int nextVersionNumber = versionRepository.findFirstByQuestionnaireIdOrderByVersionNumberDesc(questionnaireId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        QuestionnaireVersion draft = new QuestionnaireVersion(questionnaire, nextVersionNumber, QuestionnaireVersionStatus.DRAFT);
        return versionRepository.save(draft);
    }

    public QuestionnaireItem addQuestionItem(Long versionId, Long questionId, int displayOrder, Boolean requiredOverride) {
        QuestionnaireVersion version = getVersionById(versionId);
        mutabilityValidator.assertVersionIsDraft(version);

        if (displayOrder < 1) {
            throw new IllegalArgumentException("Display order must be at least 1");
        }

        ScreeningQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with ID: " + questionId));

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

    public void removeQuestionItem(Long versionId, Long itemId) {
        if (versionId == null) {
            throw new IllegalArgumentException("Version ID cannot be null");
        }
        if (itemId == null) {
            throw new IllegalArgumentException("Item ID cannot be null");
        }

        QuestionnaireVersion version = getVersionById(versionId);
        mutabilityValidator.assertVersionIsDraft(version);

        QuestionnaireItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire item not found with ID: " + itemId));

        if (!item.getQuestionnaireVersion().getId().equals(versionId)) {
            throw new IllegalArgumentException("Questionnaire item " + itemId + " does not belong to version " + versionId);
        }

        assertQuestionNotReferencedByRules(version, item.getQuestion());

        version.removeItem(item);
        itemRepository.delete(item);
        itemRepository.flush();

        List<QuestionnaireItem> remaining = itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(versionId);
        if (!remaining.isEmpty()) {
            int currentMax = remaining.stream().mapToInt(QuestionnaireItem::getDisplayOrder).max().orElse(0);
            int tempBase = currentMax + remaining.size() + 100;

            // Phase 1: assign temporary unique values safely above currentMax to prevent unique constraint collisions
            for (int i = 0; i < remaining.size(); i++) {
                remaining.get(i).setDisplayOrder(tempBase + i);
            }
            itemRepository.saveAllAndFlush(remaining);

            // Phase 2: assign contiguous 1..N
            for (int i = 0; i < remaining.size(); i++) {
                remaining.get(i).setDisplayOrder(i + 1);
            }
            itemRepository.saveAllAndFlush(remaining);
        }
    }

    public QuestionnaireItem replaceQuestionItem(Long versionId, Long itemId, Long newQuestionId, Boolean requiredOverride) {
        if (versionId == null) {
            throw new IllegalArgumentException("Version ID cannot be null");
        }
        if (itemId == null) {
            throw new IllegalArgumentException("Item ID cannot be null");
        }
        if (newQuestionId == null) {
            throw new IllegalArgumentException("Question ID cannot be null");
        }

        QuestionnaireVersion version = getVersionById(versionId);
        mutabilityValidator.assertVersionIsDraft(version);

        QuestionnaireItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire item not found with ID: " + itemId));

        if (!item.getQuestionnaireVersion().getId().equals(versionId)) {
            throw new IllegalArgumentException("Questionnaire item " + itemId + " does not belong to version " + versionId);
        }

        ScreeningQuestion newQuestion = questionRepository.findById(newQuestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with ID: " + newQuestionId));

        if (!newQuestion.isActive()) {
            throw new IllegalStateException("Cannot add inactive question to a questionnaire version");
        }

        if (!item.getQuestion().getId().equals(newQuestionId)) {
            if (itemRepository.existsByQuestionnaireVersionIdAndQuestionId(versionId, newQuestionId)) {
                throw new IllegalStateException("This question is already included in this questionnaire version.");
            }
            assertQuestionNotReferencedByRules(version, item.getQuestion());
            item.setQuestion(newQuestion);
        }

        if (requiredOverride != null) {
            item.setRequiredOverride(requiredOverride);
        }

        return itemRepository.save(item);
    }

    public List<QuestionnaireItem> reorderQuestionItems(Long versionId, List<Long> itemIds) {
        if (versionId == null) {
            throw new IllegalArgumentException("Version ID cannot be null");
        }
        if (itemIds == null || itemIds.isEmpty()) {
            throw new IllegalArgumentException("Item IDs list cannot be empty");
        }

        QuestionnaireVersion version = getVersionById(versionId);
        mutabilityValidator.assertVersionIsDraft(version);

        List<QuestionnaireItem> existingItems = itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(versionId);
        if (itemIds.size() != existingItems.size()) {
            throw new IllegalArgumentException("Reorder request must include all questionnaire items for this version exactly once.");
        }

        Set<Long> uniqueRequestedIds = new HashSet<>(itemIds);
        if (uniqueRequestedIds.size() != itemIds.size()) {
            throw new IllegalArgumentException("Reorder request contains duplicate item IDs.");
        }

        Map<Long, QuestionnaireItem> itemsById = existingItems.stream()
                .collect(Collectors.toMap(QuestionnaireItem::getId, Function.identity()));

        for (Long id : itemIds) {
            if (!itemsById.containsKey(id)) {
                throw new IllegalArgumentException("Reorder request contains item ID " + id + " which does not belong to version " + versionId);
            }
        }

        int currentMax = existingItems.stream().mapToInt(QuestionnaireItem::getDisplayOrder).max().orElse(0);
        int tempBase = currentMax + existingItems.size() + 100;

        // Phase 1: assign temporary unique values safely above currentMax to prevent unique constraint collisions
        for (int i = 0; i < itemIds.size(); i++) {
            itemsById.get(itemIds.get(i)).setDisplayOrder(tempBase + i);
        }
        itemRepository.saveAllAndFlush(existingItems);

        // Phase 2: assign contiguous 1..N in requested order
        for (int i = 0; i < itemIds.size(); i++) {
            itemsById.get(itemIds.get(i)).setDisplayOrder(i + 1);
        }
        itemRepository.saveAllAndFlush(existingItems);

        return itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(versionId);
    }

    private void assertQuestionNotReferencedByRules(QuestionnaireVersion version, ScreeningQuestion question) {
        if (version.getRules() == null || version.getRules().isEmpty() || question == null) {
            return;
        }
        String targetCode = question.getCode().trim();
        for (EligibilityRule rule : version.getRules()) {
            String referencedCode = extractReferencedQuestionCode(rule.getConditionExpression());
            if (referencedCode != null && referencedCode.equalsIgnoreCase(targetCode)) {
                throw new IllegalStateException(
                        "Question '" + targetCode + "' is used by an eligibility rule ('" + rule.getRuleCode() + "'). Update or remove that rule first."
                );
            }
        }
    }

    private static final Pattern CONDITION_EXPRESSION_TOKEN_PATTERN =
            Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*(?:==|!=|(?i)CONTAINS\\b)");

    public static String extractReferencedQuestionCode(String conditionExpression) {
        if (conditionExpression == null || conditionExpression.trim().isEmpty()) {
            return null;
        }
        String expr = conditionExpression.trim();
        Matcher matcher = CONDITION_EXPRESSION_TOKEN_PATTERN.matcher(expr);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
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
            throw new ResourceNotFoundException("Questionnaire version not found with ID: " + versionId);
        }
        return ruleRepository.findByQuestionnaireVersionIdOrderByPriorityAsc(versionId);
    }

    @Transactional(readOnly = true)
    public EligibilityRule getEligibilityRuleById(Long ruleId) {
        if (ruleId == null) {
            throw new IllegalArgumentException("Eligibility rule ID cannot be null");
        }
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Eligibility rule not found with ID: " + ruleId));
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
        if (version.getQuestionnaire() != null) {
            version.getQuestionnaire().getCode();
            version.getQuestionnaire().getName();
        }
        version.getItems().size();
        version.getRules().size();
        return version;
    }
}
