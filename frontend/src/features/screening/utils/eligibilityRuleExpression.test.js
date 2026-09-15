import { describe, expect, it } from 'vitest';
import { QUESTION_TYPE } from '../constants/screeningConstants';
import {
  ELIGIBILITY_RULE_OPERATOR,
  generateEligibilityRuleExpression,
  getEligibilityOperators,
  parseEligibilityRuleExpression,
  validateEligibilityRuleValue,
  validateMultiChoiceTypedValues,
} from './eligibilityRuleExpression';

describe('eligibilityRuleExpression', () => {
  it.each([
    ['FEVER == true', { questionCode: 'FEVER', operator: '==', value: 'true' }],
    ['AGE_GROUP != CHILD', { questionCode: 'AGE_GROUP', operator: '!=', value: 'CHILD' }],
    ['symptoms contains rash', { questionCode: 'symptoms', operator: 'CONTAINS', value: 'rash' }],
    ['  ALLERGY == "severe"  ', { questionCode: 'ALLERGY', operator: '==', value: 'severe' }],
    ["ALLERGY == 'yes'", { questionCode: 'ALLERGY', operator: '==', value: 'yes' }],
  ])('parses supported expression %s', (expression, expected) => {
    expect(parseEligibilityRuleExpression(expression)).toEqual(expected);
  });

  it.each([
    'AGE < 18',
    'TEMPERATURE >= 39',
    'A == true AND B == false',
    'A == true OR B == false',
    '(A == true)',
    'NOT A == true',
    'A ==',
  ])('rejects unsupported or malformed expression %s', (expression) => {
    expect(parseEligibilityRuleExpression(expression)).toBeNull();
  });

  it('generates canonical unquoted expressions', () => {
    expect(generateEligibilityRuleExpression({ questionCode: ' FEVER ', operator: '==', value: ' true ' }))
      .toBe('FEVER == true');
    expect(generateEligibilityRuleExpression({ questionCode: 'REACTION', operator: 'CONTAINS', value: 'severe' }))
      .toBe('REACTION CONTAINS severe');
  });

  it('rejects invalid operators, quoted values, and malformed question codes', () => {
    expect(() => generateEligibilityRuleExpression({ questionCode: 'AGE', operator: '>', value: '18' })).toThrow(/operator/i);
    expect(() => generateEligibilityRuleExpression({ questionCode: 'TEXT', operator: '==', value: "can't" })).toThrow(/quote/i);
    expect(() => generateEligibilityRuleExpression({ questionCode: 'BAD CODE', operator: '==', value: 'yes' })).toThrow(/question code/i);
    expect(() => generateEligibilityRuleExpression({ questionCode: 'BAD==CODE', operator: '==', value: 'yes' })).toThrow(/question code/i);
  });

  it('limits operators by question type', () => {
    expect(getEligibilityOperators(QUESTION_TYPE.BOOLEAN)).toEqual(['==', '!=']);
    expect(getEligibilityOperators(QUESTION_TYPE.MULTI_CHOICE)).toEqual(['CONTAINS']);
    expect(getEligibilityOperators(QUESTION_TYPE.NUMERIC)).not.toContain('>');
    expect(getEligibilityOperators(QUESTION_TYPE.DATE)).not.toContain('<');
    expect(getEligibilityOperators(QUESTION_TYPE.TEXT)).toEqual(['==', '!=', 'CONTAINS']);
  });

  it('rejects unsafe multiple-choice typed values', () => {
    expect(validateMultiChoiceTypedValues([{ typedValue: 'yes' }, { typedValue: 'no' }])).toBeNull();
    expect(validateMultiChoiceTypedValues([{ typedValue: 'pain' }, { typedValue: 'chest pain' }])).toMatch(/overlap/i);
    expect(validateMultiChoiceTypedValues([{ typedValue: 'one,two' }])).toMatch(/commas/i);
    expect(validateMultiChoiceTypedValues([{ typedValue: "can't" }])).toMatch(/quote/i);
  });

  it('validates required and quoted values', () => {
    expect(validateEligibilityRuleValue(QUESTION_TYPE.TEXT, '')).toMatch(/choose or enter/i);
    expect(validateEligibilityRuleValue(QUESTION_TYPE.TEXT, '"yes"')).toMatch(/quote/i);
    expect(validateEligibilityRuleValue(QUESTION_TYPE.BOOLEAN, 'true')).toBeNull();
    expect(validateEligibilityRuleValue(QUESTION_TYPE.SINGLE_CHOICE, 'missing', [{ typedValue: 'present' }])).toMatch(/current answer option/i);
    expect(ELIGIBILITY_RULE_OPERATOR.NOT_EQUALS).toBe('!=');
  });
});
