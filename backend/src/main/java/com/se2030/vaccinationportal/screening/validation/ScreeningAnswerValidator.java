package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validates questionnaire rules and delegates answer validation to type-specific strategies.
 */
@Component
public class ScreeningAnswerValidator {
    private final Map<QuestionType, AnswerValidationStrategy> strategies;

    public ScreeningAnswerValidator(List<AnswerValidationStrategy> strategies) {
        Map<QuestionType, AnswerValidationStrategy> byType = new EnumMap<>(QuestionType.class);
        for (AnswerValidationStrategy strategy : strategies) {
            if (byType.putIfAbsent(strategy.questionType(), strategy) != null) {
                throw new IllegalArgumentException("Duplicate answer validation strategy for " + strategy.questionType());
            }
        }
        this.strategies = Map.copyOf(byType);
    }

    public List<ValidatedAnswer> validateAndNormalize(
            Long versionId,
            List<QuestionnaireItem> items,
            Map<Long, String> submittedAnswers
    ) {
        Map<Long, QuestionnaireItem> itemsByQuestionId = items.stream()
                .collect(Collectors.toMap(item -> item.getQuestion().getId(), item -> item));

        for (Long questionId : submittedAnswers.keySet()) {
            if (questionId == null || !itemsByQuestionId.containsKey(questionId)) {
                throw new IllegalArgumentException(
                        "Question " + questionId + " is not part of questionnaire version " + versionId
                );
            }
        }

        for (QuestionnaireItem item : items) {
            Long questionId = item.getQuestion().getId();
            if (isRequired(item) && isBlank(submittedAnswers.get(questionId))) {
                throw new IllegalArgumentException("Answer required for question " + questionId);
            }
        }

        List<ValidatedAnswer> validatedAnswers = new ArrayList<>(submittedAnswers.size());
        for (Map.Entry<Long, String> entry : submittedAnswers.entrySet()) {
            QuestionnaireItem item = itemsByQuestionId.get(entry.getKey());
            ScreeningQuestion question = item.getQuestion();
            String normalizedAnswer = validateAnswer(question, entry.getValue());
            validatedAnswers.add(new ValidatedAnswer(question, normalizedAnswer));
        }
        return validatedAnswers;
    }

    private String validateAnswer(ScreeningQuestion question, String rawAnswer) {
        Long questionId = question.getId();
        if (isBlank(rawAnswer)) {
            throw new IllegalArgumentException("Blank answer submitted for question " + questionId);
        }

        String answer = rawAnswer.trim();
        QuestionType questionType = question.getQuestionType();
        if (questionType == null) {
            throw new IllegalArgumentException("Question type is missing for question " + questionId);
        }

        AnswerValidationStrategy strategy = strategies.get(questionType);
        if (strategy == null) {
            throw new IllegalStateException("No answer validation strategy for " + questionType);
        }
        return strategy.validate(questionId, answer);
    }

    private boolean isRequired(QuestionnaireItem item) {
        return item.getRequiredOverride() != null
                ? item.getRequiredOverride()
                : item.getQuestion().isDefaultRequired();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record ValidatedAnswer(ScreeningQuestion question, String typedAnswer) {}
}
