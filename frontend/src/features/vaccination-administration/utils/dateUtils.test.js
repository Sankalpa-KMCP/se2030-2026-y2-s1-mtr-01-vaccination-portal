import { describe, expect, it } from 'vitest';
import { formatLocalDateOnly, getLocalCalendarToday, toApiIsoDate } from './dateUtils';

describe('dateUtils', () => {
  describe('getLocalCalendarToday', () => {
    it('formats a provided Date using local year, month, and day', () => {
      // 2026-09-21 02:30:00 local time
      const date = new Date(2026, 8, 21, 2, 30, 0);
      expect(getLocalCalendarToday(date)).toBe('2026-09-21');
    });

    it('pads single-digit month and day with leading zero', () => {
      // 2026-03-05 local time
      const date = new Date(2026, 2, 5, 10, 0, 0);
      expect(getLocalCalendarToday(date)).toBe('2026-03-05');
    });

    it('does not shift calendar date regardless of UTC offset', () => {
      // Create a date right after local midnight (e.g. 00:05 AM on Jan 1)
      const midnightDate = new Date(2027, 0, 1, 0, 5, 0);
      expect(getLocalCalendarToday(midnightDate)).toBe('2027-01-01');
    });

    it('defaults to current local date when called without arguments', () => {
      const now = new Date();
      const expected = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
      expect(getLocalCalendarToday()).toBe(expected);
    });
  });

  describe('formatLocalDateOnly', () => {
    it('extracts YYYY-MM-DD from ISO-8601 strings', () => {
      expect(formatLocalDateOnly('2026-09-21T14:30:00Z')).toBe('2026-09-21');
      expect(formatLocalDateOnly('2026-09-21')).toBe('2026-09-21');
    });

    it('returns empty string for null, undefined, or empty values', () => {
      expect(formatLocalDateOnly(null)).toBe('');
      expect(formatLocalDateOnly(undefined)).toBe('');
      expect(formatLocalDateOnly('')).toBe('');
    });
  });

  describe('toApiIsoDate', () => {
    it('appends UTC midnight format to past YYYY-MM-DD dates', () => {
      // Provided date is not today
      expect(toApiIsoDate('2026-09-21', new Date('2026-09-25T10:00:00Z'))).toBe('2026-09-21T00:00:00Z');
    });

    it('returns current instant for today date without advancing to future UTC midnight in early-morning timezone', () => {
      // Simulate client in UTC+05:30 at 01:15 AM on Oct 9 (UTC is Oct 8 at 19:45)
      // When client local calendar date is 2026-10-09, UTC midnight would be 2026-10-09T00:00:00Z (4h15m in future)
      const simulatedNow = new Date('2026-10-08T19:45:00.000Z');
      const clientLocalDate = getLocalCalendarToday(simulatedNow);
      const result = toApiIsoDate(clientLocalDate, simulatedNow);

      expect(result).toBe('2026-10-08T19:45:00.000Z');
      expect(Date.parse(result)).toBeLessThanOrEqual(simulatedNow.getTime());
    });

    it('returns ISO string unchanged if already containing T', () => {
      expect(toApiIsoDate('2026-10-08T19:45:00.000Z')).toBe('2026-10-08T19:45:00.000Z');
    });

    it('returns empty string for empty input', () => {
      expect(toApiIsoDate('')).toBe('');
      expect(toApiIsoDate(null)).toBe('');
    });
  });
});
