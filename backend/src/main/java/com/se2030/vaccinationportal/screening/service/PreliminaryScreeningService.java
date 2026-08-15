package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.screening.model.EligibilityRule;
import com.se2030.vaccinationportal.screening.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.repository.EligibilityRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Automated preliminary screening rule evaluation engine.
 * Evaluates citizen screening answers against the immutable rule snapshot of a QuestionnaireVersion.
 * Non-diagnostic: calculates PRELIMINARY_PASS vs PRELIMINARY_REVIEW_REQUIRED for HCO clinical review.
 */
@Service
@Transactional(readOnly = true)
public class PreliminaryScreeningService {

    private final EligibilityRuleRepository ruleRepository;

    public PreliminaryScreeningService(EligibilityRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public static record RuleEvaluationResult(
            PreliminaryScreeningResult overallResult,
            List<String> triggeredRuleCodes,
            List<String> guidanceMessages
    ) {}

    /**
     * Evaluates a set of screening answers against the rules snapshot for a given questionnaire version.
     *
     * @param versionId The QuestionnaireVersion ID
     * @param answers The submitted ScreeningAnswer entities
     * @return RuleEvaluationResult containing overall flag and triggered rules
     */
    public RuleEvaluationResult evaluateAnswers(Long versionId, List<ScreeningAnswer> answers) {
        if (versionId == null) {
            throw new IllegalArgumentException("Version ID cannot be null");
        }
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("Screening answers cannot be empty");
        }

        List<EligibilityRule> rules = ruleRepository.findByQuestionnaireVersionIdOrderByPriorityAsc(versionId);

        // Map question code -> submitted answer string
        Map<String, String> answerMap = answers.stream()
                .filter(a -> a.getQuestion() != null && a.getQuestion().getCode() != null)
                .collect(Collectors.toMap(
                        a -> a.getQuestion().getCode().trim().toUpperCase(),
                        ScreeningAnswer::getTypedAnswer,
                        (existing, replacement) -> replacement
                ));

        List<String> triggeredCodes = new ArrayList<>();
        List<String> guidanceMessages = new ArrayList<>();
        boolean requiresReview = false;

        for (EligibilityRule rule : rules) {
            boolean conditionMet = evaluateCondition(rule.getConditionExpression(), answerMap);
            if (conditionMet) {
                triggeredCodes.add(rule.getRuleCode());
                guidanceMessages.add(rule.getMessage());

                if (rule.getPreliminaryResult() == PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED) {
                    requiresReview = true;
                }
            }
        }

        PreliminaryScreeningResult overallResult = requiresReview
                ? PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED
                : PreliminaryScreeningResult.PRELIMINARY_PASS;

        return new RuleEvaluationResult(overallResult, triggeredCodes, guidanceMessages);
    }

    /**
     * Evaluates a rule condition expression against the citizen's answers.
     * Supports formats: "QUESTION_CODE == value", "QUESTION_CODE != value", "QUESTION_CODE CONTAINS value"
     */
    private boolean evaluateCondition(String conditionExpression, Map<String, String> answers) {
        if (conditionExpression == null || conditionExpression.trim().isEmpty()) {
            return false;
        }

        String expr = conditionExpression.trim();

        if (expr.contains("==")) {
            String[] parts = expr.split("==", 2);
            String questionCode = parts[0].trim().toUpperCase();
            String expectedValue = parts[1].trim().replace("\"", "").replace("'", "");
            String actualValue = answers.get(questionCode);
            return actualValue != null && actualValue.equalsIgnoreCase(expectedValue);
        } else if (expr.contains("!=")) {
            String[] parts = expr.split("!=", 2);
            String questionCode = parts[0].trim().toUpperCase();
            String expectedValue = parts[1].trim().replace("\"", "").replace("'", "");
            String actualValue = answers.get(questionCode);
            return actualValue != null && !actualValue.equalsIgnoreCase(expectedValue);
        } else if (expr.toUpperCase().contains("CONTAINS")) {
            String[] parts = expr.split("(?i)CONTAINS", 2);
            String questionCode = parts[0].trim().toUpperCase();
            String targetValue = parts[1].trim().replace("\"", "").replace("'", "");
            String actualValue = answers.get(questionCode);
            return actualValue != null && actualValue.toUpperCase().contains(targetValue.toUpperCase());
        }

        return false;
    }
}
