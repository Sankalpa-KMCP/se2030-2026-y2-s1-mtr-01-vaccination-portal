package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
public class DateAnswerValidationStrategy implements AnswerValidationStrategy {
    @Override
    public QuestionType questionType() {
        return QuestionType.DATE;
    }

    @Override
    public String validate(Long questionId, String answer) {
        try {
            LocalDate.parse(answer);
            return answer;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date answer for question " + questionId);
        }
    }
}
