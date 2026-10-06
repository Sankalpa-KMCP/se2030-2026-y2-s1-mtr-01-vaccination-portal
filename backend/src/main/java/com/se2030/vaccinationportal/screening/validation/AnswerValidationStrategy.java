package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;

/** Validates an already trimmed, nonblank answer for one question type. */
public interface AnswerValidationStrategy {
    QuestionType questionType();

    String validate(Long questionId, String answer);
}
