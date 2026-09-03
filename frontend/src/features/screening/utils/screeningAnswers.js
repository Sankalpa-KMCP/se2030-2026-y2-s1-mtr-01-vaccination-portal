import { QUESTION_TYPE } from '../constants/screeningConstants';

export function isScreeningAnswerEmpty(questionType, value) {
  if (questionType === QUESTION_TYPE.MULTI_CHOICE) {
    return !Array.isArray(value) || value.length === 0;
  }

  return value === undefined || value === null || String(value).trim() === '';
}

export function validateScreeningAnswers(questions, answers) {
  return questions.reduce((errors, question) => {
    if (question.required && isScreeningAnswerEmpty(question.questionType, answers[question.id])) {
      errors[question.id] = 'This question is required.';
    }
    return errors;
  }, {});
}

export function serializeScreeningAnswers(questions, answers) {
  return questions.reduce((serialized, question) => {
    const value = answers[question.id];
    if (isScreeningAnswerEmpty(question.questionType, value)) {
      return serialized;
    }

    serialized[String(question.id)] = question.questionType === QUESTION_TYPE.MULTI_CHOICE
      ? value.join(',')
      : String(value).trim();
    return serialized;
  }, {});
}
