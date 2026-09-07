import { QUESTION_TYPE } from '../constants/screeningConstants';

export const ELIGIBILITY_RULE_OPERATOR = Object.freeze({
  EQUALS: '==',
  NOT_EQUALS: '!=',
  CONTAINS: 'CONTAINS',
});

const EQUALS_OPERATORS = Object.freeze([
  ELIGIBILITY_RULE_OPERATOR.EQUALS,
  ELIGIBILITY_RULE_OPERATOR.NOT_EQUALS,
]);

const OPERATORS_BY_TYPE = Object.freeze({
  [QUESTION_TYPE.BOOLEAN]: EQUALS_OPERATORS,
  [QUESTION_TYPE.SINGLE_CHOICE]: EQUALS_OPERATORS,
  [QUESTION_TYPE.MULTI_CHOICE]: Object.freeze([ELIGIBILITY_RULE_OPERATOR.CONTAINS]),
  [QUESTION_TYPE.NUMERIC]: EQUALS_OPERATORS,
  [QUESTION_TYPE.DATE]: EQUALS_OPERATORS,
  [QUESTION_TYPE.TEXT]: Object.freeze([
    ELIGIBILITY_RULE_OPERATOR.EQUALS,
    ELIGIBILITY_RULE_OPERATOR.NOT_EQUALS,
    ELIGIBILITY_RULE_OPERATOR.CONTAINS,
  ]),
});

export const ELIGIBILITY_RULE_OPERATOR_LABELS = Object.freeze({
  [ELIGIBILITY_RULE_OPERATOR.EQUALS]: 'Equals',
  [ELIGIBILITY_RULE_OPERATOR.NOT_EQUALS]: 'Not equals',
  [ELIGIBILITY_RULE_OPERATOR.CONTAINS]: 'Contains',
});

export function getEligibilityOperators(questionType) {
  return OPERATORS_BY_TYPE[questionType] || [];
}

export function generateEligibilityRuleExpression({ questionCode, operator, value }) {
  const normalizedCode = String(questionCode || '').trim();
  const normalizedValue = String(value ?? '').trim();
  const supportedOperators = Object.values(ELIGIBILITY_RULE_OPERATOR);

  if (!normalizedCode || /\s|[()]|==|!=|CONTAINS/i.test(normalizedCode)) {
    throw new Error('Question code cannot be represented safely in an eligibility condition.');
  }
  if (!supportedOperators.includes(operator)) {
    throw new Error('Eligibility condition operator is not supported.');
  }
  if (!normalizedValue) {
    throw new Error('Eligibility condition value is required.');
  }
  if (/["']/.test(normalizedValue)) {
    throw new Error('Eligibility condition values cannot contain quote characters.');
  }
  if (/\r|\n/.test(normalizedValue)) {
    throw new Error('Eligibility condition values must be entered on one line.');
  }

  return `${normalizedCode} ${operator} ${normalizedValue}`;
}

export function parseEligibilityRuleExpression(expression) {
  if (typeof expression !== 'string' || !expression.trim() || /[()]/.test(expression)) {
    return null;
  }

  if (/\s+(?:AND|OR)\s+[^\s()]+\s*(?:==|!=|CONTAINS)\s+/i.test(expression)) {
    return null;
  }

  const match = expression.match(/^\s*([^\s()]+)\s*(==|!=|CONTAINS)\s*(.*?)\s*$/i);
  if (!match || !match[3]) return null;

  const questionCode = match[1];
  const operatorToken = match[2].toUpperCase();
  const operator = operatorToken === 'CONTAINS' ? operatorToken : match[2];
  let value = match[3].trim();

  const quote = value[0];
  if ((quote === '"' || quote === "'") && value[value.length - 1] === quote) {
    value = value.slice(1, -1).trim();
  }

  if (!value || /["']/.test(value) || /==|!=|CONTAINS/i.test(questionCode)) return null;

  return { questionCode, operator, value };
}

export function validateMultiChoiceTypedValues(options = []) {
  const values = options.map((option) => String(option?.typedValue ?? '').trim());
  if (values.some((value) => !value)) {
    return 'Every answer option must have a typed value before it can be used in a rule.';
  }
  if (values.some((value) => /[,"']/.test(value))) {
    return 'Multiple-choice rule values cannot contain commas or quote characters.';
  }

  const normalized = values.map((value) => value.toLocaleLowerCase());
  for (let left = 0; left < normalized.length; left += 1) {
    for (let right = left + 1; right < normalized.length; right += 1) {
      if (normalized[left].includes(normalized[right]) || normalized[right].includes(normalized[left])) {
        return 'Multiple-choice typed values overlap, so the backend substring match would be ambiguous.';
      }
    }
  }
  return null;
}

export function validateEligibilityRuleValue(questionType, value, options = []) {
  const normalizedValue = String(value ?? '').trim();
  if (questionType === QUESTION_TYPE.MULTI_CHOICE) {
    const safetyError = validateMultiChoiceTypedValues(options);
    if (safetyError) return safetyError;
  }
  if (!normalizedValue) return 'Choose or enter a comparison value.';
  if (/["']/.test(normalizedValue)) {
    return 'Comparison values cannot contain quote characters.';
  }
  if ((questionType === QUESTION_TYPE.SINGLE_CHOICE || questionType === QUESTION_TYPE.MULTI_CHOICE)
      && !options.some((option) => String(option?.typedValue ?? '') === normalizedValue)) {
    return 'Choose a current answer option for this question.';
  }
  return null;
}
