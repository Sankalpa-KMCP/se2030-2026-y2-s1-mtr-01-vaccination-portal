package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import org.springframework.stereotype.Component;

@Component
public class BooleanAnswerValidationStrategy implements AnswerValidationStrategy {
    @Override
    public QuestionType questionType() {
        return QuestionType.BOOLEAN;
    }

    @Override
    public String validate(Long questionId, String answer) {
        if (!answer.equalsIgnoreCase("true") && !answer.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("Invalid boolean answer for question " + questionId);
        }
        return answer.toLowerCase();
    }
}
