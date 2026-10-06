package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import org.springframework.stereotype.Component;

@Component
public class TextAnswerValidationStrategy implements AnswerValidationStrategy {
    @Override
    public QuestionType questionType() {
        return QuestionType.TEXT;
    }

    @Override
    public String validate(Long questionId, String answer) {
        return answer;
    }
}
