import { describe, expect, it } from 'vitest';
import { createCorrectionAnswerState } from './screeningCorrectionAnswers';

function question(overrides) {
  return {
    id: 1,
    questionType: 'TEXT',
    options: [],
    currentAnswer: null,
    ...overrides,
  };
}

const options = [
  { id: 11, code: 'YES_CODE', typedValue: 'true' },
  { id: 12, code: 'NO_CODE', typedValue: 'false' },
];

describe('correction answer prefill', () => {
  it.each([['true'], ['false']])('preserves BOOLEAN typed answer %s', (typedAnswer) => {
    const result = createCorrectionAnswerState([
      question({ questionType: 'BOOLEAN', currentAnswer: { typedAnswer } }),
    ]);
    expect(result.answers[1]).toBe(typedAnswer);
  });

  it('maps a SINGLE_CHOICE selected option typed value to its UI code', () => {
    const result = createCorrectionAnswerState([question({
      questionType: 'SINGLE_CHOICE',
      options,
      currentAnswer: { typedAnswer: 'false', selectedOptions: [options[1]] },
    })]);
    expect(result.answers[1]).toBe('NO_CODE');
    expect(result.legacyAnswers).toEqual({});
  });

  it('safely resolves a SINGLE_CHOICE typed value when selected-option metadata is unavailable', () => {
    const result = createCorrectionAnswerState([question({
      questionType: 'SINGLE_CHOICE',
      options,
      currentAnswer: { typedAnswer: 'false' },
    })]);
    expect(result.answers[1]).toBe('NO_CODE');
    expect(result.legacyAnswers).toEqual({});
  });

  it('maps MULTI_CHOICE selections to UI codes in stored selection order', () => {
    const result = createCorrectionAnswerState([question({
      questionType: 'MULTI_CHOICE',
      options,
      currentAnswer: { typedAnswer: 'false,true', selectedOptions: [options[1], options[0]] },
    })]);
    expect(result.answers[1]).toEqual(['NO_CODE', 'YES_CODE']);
  });

  it.each([
    ['NUMERIC', '37.50'],
    ['DATE', '2026-09-05'],
    ['TEXT', 'Preserve this text exactly'],
  ])('preserves %s answers as strings', (questionType, typedAnswer) => {
    const result = createCorrectionAnswerState([
      question({ questionType, currentAnswer: { typedAnswer } }),
    ]);
    expect(result.answers[1]).toBe(typedAnswer);
  });

  it('initializes unanswered optional questions using the existing empty value shapes', () => {
    const result = createCorrectionAnswerState([
      question({ id: 1, questionType: 'TEXT' }),
      question({ id: 2, questionType: 'MULTI_CHOICE' }),
    ]);
    expect(result.answers).toEqual({ 1: '', 2: [] });
  });

  it('handles an unknown single choice without mapping it to another option', () => {
    const result = createCorrectionAnswerState([question({
      questionType: 'SINGLE_CHOICE',
      options,
      currentAnswer: { typedAnswer: 'LEGACY_VALUE', selectedOptions: [] },
    })]);
    expect(result.answers[1]).toBe('');
    expect(result.legacyAnswers[1]).toEqual(['LEGACY_VALUE']);
  });

  it('preserves recognized multi-choice selections and reports unknown values', () => {
    const result = createCorrectionAnswerState([question({
      questionType: 'MULTI_CHOICE',
      options,
      currentAnswer: { typedAnswer: 'true,LEGACY_VALUE', selectedOptions: [options[0]] },
    })]);
    expect(result.answers[1]).toEqual(['YES_CODE']);
    expect(result.legacyAnswers[1]).toEqual(['LEGACY_VALUE']);
  });

  it('does not match selected-option metadata from another question', () => {
    const result = createCorrectionAnswerState([question({
      questionType: 'SINGLE_CHOICE',
      options,
      currentAnswer: {
        typedAnswer: 'true',
        selectedOptions: [{ id: 999, code: 'YES_CODE', typedValue: 'true' }],
      },
    })]);
    expect(result.answers[1]).toBe('');
    expect(result.legacyAnswers[1]).toEqual(['true']);
  });
});
