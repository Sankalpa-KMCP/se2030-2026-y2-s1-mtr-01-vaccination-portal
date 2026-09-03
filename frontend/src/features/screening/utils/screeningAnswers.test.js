import { describe, expect, it } from 'vitest';
import { serializeScreeningAnswers, validateScreeningAnswers } from './screeningAnswers';

const questions = [
  { id: 101, questionType: 'BOOLEAN', required: true },
  { id: 102, questionType: 'SINGLE_CHOICE', required: true },
  { id: 103, questionType: 'MULTI_CHOICE', required: true },
  { id: 104, questionType: 'NUMERIC', required: true },
  { id: 105, questionType: 'DATE', required: true },
  { id: 106, questionType: 'TEXT', required: true },
  { id: 107, questionType: 'TEXT', required: false },
];

describe('screening answer helpers', () => {
  it('validates every required question using its effective required field', () => {
    const errors = validateScreeningAnswers(questions, {
      101: '', 102: '', 103: [], 104: '', 105: '', 106: '   ', 107: '',
    });
    expect(Object.keys(errors)).toEqual(['101', '102', '103', '104', '105', '106']);
  });

  it('serializes every answer as a string and omits optional blanks', () => {
    const result = serializeScreeningAnswers(questions, {
      101: 'true',
      102: 'NO',
      103: ['ASTHMA', 'DIABETES'],
      104: '37.5',
      105: '2026-09-03',
      106: ' Additional details ',
      107: '   ',
    });
    expect(result).toEqual({
      101: 'true',
      102: 'NO',
      103: 'ASTHMA,DIABETES',
      104: '37.5',
      105: '2026-09-03',
      106: 'Additional details',
    });
    expect(Object.values(result).every((value) => typeof value === 'string')).toBe(true);
  });
});
