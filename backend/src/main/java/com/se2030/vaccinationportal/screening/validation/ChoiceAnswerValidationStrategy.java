package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;

import java.util.List;

/** Shared option lookup used by both choice strategies. */
abstract class ChoiceAnswerValidationStrategy implements AnswerValidationStrategy {
    private final AnswerOptionRepository answerOptionRepository;

    protected ChoiceAnswerValidationStrategy(AnswerOptionRepository answerOptionRepository) {
        this.answerOptionRepository = answerOptionRepository;
    }

    protected List<AnswerOption> loadOptions(Long questionId) {
        return answerOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(questionId);
    }

    protected AnswerOption findOption(Long questionId, String submittedValue, List<AnswerOption> options) {
        return options.stream()
                .filter(option -> option.getQuestion() != null && questionId.equals(option.getQuestion().getId()))
                .filter(option -> submittedValue.equals(option.getCode()) || submittedValue.equals(option.getTypedValue()))
                .findFirst()
                .orElse(null);
    }
}
