export const QUESTION_TYPE = Object.freeze({
  BOOLEAN: 'BOOLEAN',
  SINGLE_CHOICE: 'SINGLE_CHOICE',
  MULTI_CHOICE: 'MULTI_CHOICE',
  NUMERIC: 'NUMERIC',
  DATE: 'DATE',
  TEXT: 'TEXT',
});

export const QUESTION_TYPES = Object.freeze(Object.values(QUESTION_TYPE));

export const QUESTION_TYPE_LABELS = Object.freeze({
  [QUESTION_TYPE.BOOLEAN]: 'Yes / No',
  [QUESTION_TYPE.SINGLE_CHOICE]: 'Single Choice',
  [QUESTION_TYPE.MULTI_CHOICE]: 'Multiple Choice',
  [QUESTION_TYPE.NUMERIC]: 'Numeric',
  [QUESTION_TYPE.DATE]: 'Date',
  [QUESTION_TYPE.TEXT]: 'Text',
});

const CHOICE_QUESTION_TYPES = Object.freeze([
  QUESTION_TYPE.SINGLE_CHOICE,
  QUESTION_TYPE.MULTI_CHOICE,
]);

export function isChoiceQuestionType(questionType) {
  return CHOICE_QUESTION_TYPES.includes(questionType);
}
