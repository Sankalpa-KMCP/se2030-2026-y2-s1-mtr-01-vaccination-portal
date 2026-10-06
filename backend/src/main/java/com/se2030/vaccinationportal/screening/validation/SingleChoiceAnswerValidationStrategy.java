package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import org.springframework.stereotype.Component;

@Component
public class SingleChoiceAnswerValidationStrategy extends ChoiceAnswerValidationStrategy {
    public SingleChoiceAnswerValidationStrategy(AnswerOptionRepository answerOptionRepository) {
        super(answerOptionRepository);
    }

    @Override
    public QuestionType questionType() {
        return QuestionType.SINGLE_CHOICE;
    }

    @Override
    public String validate(Long questionId, String answer) {
        AnswerOption option = findOption(questionId, answer, loadOptions(questionId));
        if (option == null) {
            throw new IllegalArgumentException("Selected option is not valid for question " + questionId);
        }
        return option.getTypedValue();
    }
}
