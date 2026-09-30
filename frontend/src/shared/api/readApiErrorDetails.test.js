import { describe, expect, it } from 'vitest';
import { PERMISSION_DENIED_MESSAGE } from './apiClient';
import { readApiErrorDetails } from './readApiErrorDetails';

describe('readApiErrorDetails', () => {
  it('returns null for absent errors', () => {
    expect(readApiErrorDetails(null)).toBeNull();
    expect(readApiErrorDetails(undefined)).toBeNull();
  });

  it.each([
    [0, 'Network unavailable'],
    [400, 'Validation failed'],
    [401, 'Session expired'],
    [404, 'Resource missing'],
    [409, 'Conflict'],
    [500, 'Server unavailable'],
  ])('retains status %s and server message', (status, message) => {
    expect(readApiErrorDetails({ status, message })).toEqual({ status, message, fieldErrors: [] });
  });

  it('uses the existing permission-denied wording for 403', () => {
    expect(readApiErrorDetails({ status: 403, message: 'Backend secret' })).toEqual({
      status: 403, message: PERMISSION_DENIED_MESSAGE, fieldErrors: [],
    });
  });

  it('retains string errors and missing or partial response facts', () => {
    expect(readApiErrorDetails('Request unavailable')).toEqual({
      status: null, message: 'Request unavailable', fieldErrors: [],
    });
    expect(readApiErrorDetails({ data: { fieldErrors: 'malformed' } })).toEqual({
      status: undefined, message: undefined, fieldErrors: [],
    });
  });

  it('keeps object and array field-error order and filters empty entries', () => {
    expect(readApiErrorDetails({ data: { fieldErrors: { first: ['A', '', 'B'], second: 'C' } } }).fieldErrors)
      .toEqual([{ field: 'first', message: 'A' }, { field: 'first', message: 'B' }, { field: 'second', message: 'C' }]);
    expect(readApiErrorDetails({ data: { fieldErrors: [{ field: 'email', message: 'Invalid' }, null, { field: 'empty' }] } }).fieldErrors)
      .toEqual([{ field: 'email', message: 'Invalid' }]);
  });
});
