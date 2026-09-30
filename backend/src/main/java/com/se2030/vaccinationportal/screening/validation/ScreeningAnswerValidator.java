package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates and normalizes answers against the immutable questionnaire version snapshot.
 */
@Component
public class ScreeningAnswerValidator {

    private final AnswerOptionRepository answerOptionRepository;

    public ScreeningAnswerValidator(AnswerOptionRepository answerOptionRepository) {
        this.answerOptionRepository = answerOptionRepository;
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

        return switch (questionType) {
            case TEXT -> answer;
            case BOOLEAN -> validateBoolean(questionId, answer);
            case NUMERIC -> validateNumeric(questionId, answer);
            case DATE -> validateDate(questionId, answer);
            case SINGLE_CHOICE -> validateSingleChoice(questionId, answer);
            case MULTI_CHOICE -> validateMultiChoice(questionId, answer);
        };
    }

    private String validateBoolean(Long questionId, String answer) {
        if (!answer.equalsIgnoreCase("true") && !answer.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("Invalid boolean answer for question " + questionId);
        }
        return answer.toLowerCase();
    }

    private String validateNumeric(Long questionId, String answer) {
        try {
            new BigDecimal(answer);
            return answer;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric answer for question " + questionId);
        }
    }

    private String validateDate(Long questionId, String answer) {
        try {
            LocalDate.parse(answer);
            return answer;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date answer for question " + questionId);
        }
    }

    private String validateSingleChoice(Long questionId, String answer) {
        AnswerOption option = findOption(questionId, answer, loadOptions(questionId));
        if (option == null) {
            throw new IllegalArgumentException("Selected option is not valid for question " + questionId);
        }
        return option.getTypedValue();
    }

    private String validateMultiChoice(Long questionId, String answer) {
        List<AnswerOption> options = loadOptions(questionId);
        String[] selections = answer.split(",", -1);
        List<String> normalizedSelections = new ArrayList<>(selections.length);
        Set<String> selectedOptionKeys = new HashSet<>();

        for (String rawSelection : selections) {
            String selection = rawSelection.trim();
            if (selection.isEmpty()) {
                throw new IllegalArgumentException("Invalid multi-choice answer for question " + questionId);
            }

            AnswerOption option = findOption(questionId, selection, options);
            if (option == null) {
                throw new IllegalArgumentException("Selected option is not valid for question " + questionId);
            }

            String optionKey = option.getId() != null
                    ? "ID:" + option.getId()
                    : "CODE:" + option.getCode();
            if (!selectedOptionKeys.add(optionKey)) {
                throw new IllegalArgumentException("Duplicate option selected for question " + questionId);
            }
            normalizedSelections.add(option.getTypedValue());
        }
        return String.join(",", normalizedSelections);
    }

    private List<AnswerOption> loadOptions(Long questionId) {
        return answerOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(questionId);
    }

    private AnswerOption findOption(Long questionId, String submittedValue, List<AnswerOption> options) {
        return options.stream()
                .filter(option -> option.getQuestion() != null && questionId.equals(option.getQuestion().getId()))
                .filter(option -> submittedValue.equals(option.getCode()) || submittedValue.equals(option.getTypedValue()))
                .findFirst()
                .orElse(null);
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
