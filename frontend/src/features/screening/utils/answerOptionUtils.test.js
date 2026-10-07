import { describe, expect, it } from 'vitest';
import {
  generateOptionCode,
  getNextDisplayOrder,
  normalizeLabel,
  parseMultipleOptions,
  validateOptionLabel,
} from './answerOptionUtils';

describe('answerOptionUtils', () => {
  describe('generateOptionCode', () => {
    it('generates uppercase snake_case code from plain label', () => {
      expect(generateOptionCode('No symptoms')).toBe('NO_SYMPTOMS');
      expect(generateOptionCode('High fever')).toBe('HIGH_FEVER');
      expect(generateOptionCode('Not sure')).toBe('NOT_SURE');
    });

    it('trims whitespace and replaces hyphens and spaces with underscores', () => {
      expect(generateOptionCode('  severe - headache  ')).toBe('SEVERE_HEADACHE');
      expect(generateOptionCode('mild   cough')).toBe('MILD_COUGH');
    });

    it('strips unsupported special characters and collapses repeated underscores', () => {
      expect(generateOptionCode('Fever > 38.5°C?')).toBe('FEVER_385C');
      expect(generateOptionCode('Yes / No (Maybe!)')).toBe('YES_NO_MAYBE');
    });

    it('falls back to OPTION when label has no alphanumeric characters', () => {
      expect(generateOptionCode('???')).toBe('OPTION');
      expect(generateOptionCode('---')).toBe('OPTION');
      expect(generateOptionCode('')).toBe('OPTION');
      expect(generateOptionCode(null)).toBe('OPTION');
    });

    it('resolves collisions by appending a deterministic _2, _3 suffix', () => {
      expect(generateOptionCode('Other', ['OTHER'])).toBe('OTHER_2');
      expect(generateOptionCode('Other', ['OTHER', 'OTHER_2'])).toBe('OTHER_3');
      expect(generateOptionCode('Other_2', ['OTHER_2'])).toBe('OTHER_3');
    });

    it('guarantees code length does not exceed 64 characters even with conflict suffix', () => {
      const veryLongLabel = 'A'.repeat(80);
      const code1 = generateOptionCode(veryLongLabel);
      expect(code1.length).toBeLessThanOrEqual(64);

      const code2 = generateOptionCode(veryLongLabel, [code1]);
      expect(code2.length).toBeLessThanOrEqual(64);
      expect(code2.endsWith('_2')).toBe(true);
    });
  });

  describe('getNextDisplayOrder', () => {
    it('returns 1 when options list is empty or invalid', () => {
      expect(getNextDisplayOrder([])).toBe(1);
      expect(getNextDisplayOrder(null)).toBe(1);
      expect(getNextDisplayOrder(undefined)).toBe(1);
    });

    it('returns max(existing displayOrder) + 1', () => {
      expect(getNextDisplayOrder([{ displayOrder: 1 }, { displayOrder: 2 }, { displayOrder: 5 }])).toBe(6);
      expect(getNextDisplayOrder([{ displayOrder: 1 }, { displayOrder: 2 }, { displayOrder: 3 }])).toBe(4);
    });

    it('handles string display orders gracefully', () => {
      expect(getNextDisplayOrder([{ displayOrder: '1' }, { displayOrder: '4' }])).toBe(5);
    });
  });

  describe('normalizeLabel and validateOptionLabel', () => {
    it('normalizes label by trimming and lowercasing', () => {
      expect(normalizeLabel('  No Symptoms ')).toBe('no symptoms');
    });

    it('requires a non-empty label', () => {
      expect(validateOptionLabel('')).toBe('Option label is required.');
      expect(validateOptionLabel('   ')).toBe('Option label is required.');
    });

    it('detects duplicates with case-insensitive and whitespace normalization', () => {
      const existing = [{ id: 1, label: 'Other' }];
      expect(validateOptionLabel('Other', existing)).toBe('An option with this label already exists.');
      expect(validateOptionLabel(' other ', existing)).toBe('An option with this label already exists.');
      expect(validateOptionLabel('OTHER', existing)).toBe('An option with this label already exists.');
    });

    it('allows keeping the same label when editing the same option', () => {
      const existing = [{ id: 1, label: 'Other' }];
      expect(validateOptionLabel('Other', existing, 1)).toBeNull();
      expect(validateOptionLabel(' other ', existing, 1)).toBeNull();
    });

    it('returns null for valid distinct label', () => {
      const existing = [{ id: 1, label: 'Other' }];
      expect(validateOptionLabel('Fever', existing)).toBeNull();
    });
  });

  describe('parseMultipleOptions', () => {
    it('parses valid multiline input into options with auto-generated fields', () => {
      const text = 'No symptoms\nFever\nCough';
      const result = parseMultipleOptions(text, [{ code: 'EXISTING', label: 'Existing', displayOrder: 1 }]);

      expect(result.valid).toBe(true);
      expect(result.items).toHaveLength(3);
      expect(result.items[0]).toEqual({
        code: 'NO_SYMPTOMS',
        label: 'No symptoms',
        typedValue: 'No symptoms',
        displayOrder: 2,
      });
      expect(result.items[1]).toEqual({
        code: 'FEVER',
        label: 'Fever',
        typedValue: 'Fever',
        displayOrder: 3,
      });
      expect(result.items[2]).toEqual({
        code: 'COUGH',
        label: 'Cough',
        typedValue: 'Cough',
        displayOrder: 4,
      });
    });

    it('ignores blank lines', () => {
      const text = '\n  No symptoms  \n\n  Fever  \n\n';
      const result = parseMultipleOptions(text);
      expect(result.valid).toBe(true);
      expect(result.items).toHaveLength(2);
      expect(result.items[0].label).toBe('No symptoms');
      expect(result.items[1].label).toBe('Fever');
    });

    it('detects duplicate labels within the batch', () => {
      const text = 'Fever\nCough\n  fever  ';
      const result = parseMultipleOptions(text);
      expect(result.valid).toBe(false);
      expect(result.error).toContain('Duplicate label "fever" found in the entered options.');
    });

    it('detects duplicate labels against existing options', () => {
      const text = 'Fever\nCough';
      const existing = [{ code: 'FEVER', label: 'fever', displayOrder: 1 }];
      const result = parseMultipleOptions(text, existing);
      expect(result.valid).toBe(false);
      expect(result.error).toContain('An option with label "Fever" already exists');
    });
  });
});
