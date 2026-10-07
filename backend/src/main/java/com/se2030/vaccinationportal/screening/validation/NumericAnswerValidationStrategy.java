package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NumericAnswerValidationStrategy implements AnswerValidationStrategy {
    @Override
    public QuestionType questionType() {
        return QuestionType.NUMERIC;
    }

    @Override
    public String validate(Long questionId, String answer) {
        try {
            new BigDecimal(answer);
            return answer;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric answer for question " + questionId);
        }
    }
}
