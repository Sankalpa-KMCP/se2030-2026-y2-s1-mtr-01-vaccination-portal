package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class MultiChoiceAnswerValidationStrategy extends ChoiceAnswerValidationStrategy {
    public MultiChoiceAnswerValidationStrategy(AnswerOptionRepository answerOptionRepository) {
        super(answerOptionRepository);
    }

    @Override
    public QuestionType questionType() {
        return QuestionType.MULTI_CHOICE;
    }

    @Override
    public String validate(Long questionId, String answer) {
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
}
